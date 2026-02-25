package com.example.levabolliapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class PreventivoFormActivity : AppCompatActivity() {

    // pannelli fissi (come da tua richiesta)
    private val panelNames = listOf(
        "Cofano",
        "Tetto",
        "Baule ALTO",
        "Baule BASSO",
        "Parafango ANT SX", "Parafango ANT DX",
        "Parafango POST SX", "Parafango POST DX",
        "Porta ANT SX", "Porta ANT DX",
        "Porta POST SX", "Porta POST DX",
        "Montante ANT SX", "Montante ANT DX",
        "Montante CENTRALE SX", "Montante CENTRALE DX",
        "Montante POST SX", "Montante POST DX",
        "Minigonna SX", "Minigonna DX"
    )

    // spinner misura
    private val misureLabels = listOf("Misura 1 (<10mm)", "Misura 2 (<25mm)", "Misura 3 (<45mm)")

    // listino: se l’utente ha personalizzato, lo leggiamo da Storage (custom_listino_json)
    // altrimenti usiamo uno standard MINIMO (poi lo riempiamo bene con la tua tabella 2026 fino a 600)
    private var listino: List<ListinoRow> = emptyList()

    data class ListinoRow(val misura: Int, val min: Int, val max: Int, val prezzo: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preventivo_form)

// IVA defaults (Fusione App Beta)
val appSettings = AppSettingsRepo.load(this)
val edtIvaPercent = findViewById<EditText>(R.id.edtIvaPercent)
val chkIvaCompresa = findViewById<CheckBox>(R.id.chkIvaCompresa)
edtIvaPercent.setText(appSettings.ivaPercent.toString())
chkIvaCompresa.isChecked = appSettings.ivaInclusaDefault

edtIvaPercent.addTextChangedListener(simpleWatcher {
    recalcTotals()
})
chkIvaCompresa.setOnCheckedChangeListener { _, _ ->
    recalcTotals()
}

        // 1) prepara listino
        listino = loadListinoOrDefault()

        // 2) setup smontaggio
        setupSmontaggio()

        // 3) crea pannelli
        buildPanels()

        // 4) ricalcolo quando cambi prezzo reale
        findViewById<EditText>(R.id.edtPrezzoReale).addTextChangedListener(simpleWatcher {
            recalcTotals()
        })

        // primo calcolo
        recalcTotals()
    }

    private fun setupSmontaggio() {
        val chk = findViewById<CheckBox>(R.id.chkSmontaggio)
        val spn = findViewById<Spinner>(R.id.spnCatSmontaggio)
        val edtEuro = findViewById<EditText>(R.id.edtSmontaggioEuro)

        val cats = listOf("A (90€)", "B (120€)", "C (150€)", "D (200€)")
        spn.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cats)

        fun setEuroByCat(pos: Int) {
            val euro = when (pos) {
                0 -> 90
                1 -> 120
                2 -> 150
                else -> 200
            }
            edtEuro.setText(euro.toString())
        }

        chk.setOnCheckedChangeListener { _, isChecked ->
            spn.isEnabled = isChecked
            edtEuro.isEnabled = isChecked
            if (isChecked) {
                if (edtEuro.text.isNullOrBlank()) setEuroByCat(spn.selectedItemPosition)
            } else {
                edtEuro.setText("")
            }
            recalcTotals()
        }

        spn.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (chk.isChecked) {
                    setEuroByCat(position)
                    recalcTotals()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        edtEuro.addTextChangedListener(simpleWatcher { recalcTotals() })
    }

    private fun buildPanels() {
        val container = findViewById<LinearLayout>(R.id.panelsContainer)
        container.removeAllViews()

        val inflater = LayoutInflater.from(this)

        for (name in panelNames) {
            val row = inflater.inflate(R.layout.item_panel, container, false)

            val txtName = row.findViewById<TextView>(R.id.txtPanelName)
            val edtBolli = row.findViewById<EditText>(R.id.edtBolli)
            val spnMisura = row.findViewById<Spinner>(R.id.spnMisura)
            val chkAll = row.findViewById<CheckBox>(R.id.chkAlluminio)
            val chkPTP = row.findViewById<CheckBox>(R.id.chkPTP)
            val txtPrezzo = row.findViewById<TextView>(R.id.txtPrezzoPanel)

            txtName.text = name
            spnMisura.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, misureLabels)

            // ogni modifica ricalcola
            edtBolli.addTextChangedListener(simpleWatcher { recalcTotals() })
            spnMisura.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    recalcTotals()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            chkAll.setOnCheckedChangeListener { _, _ -> recalcTotals() }
            chkPTP.setOnCheckedChangeListener { _, _ -> recalcTotals() }

            // tagghiamo la view così la leggiamo in recalcTotals
            row.tag = PanelRefs(edtBolli, spnMisura, chkAll, chkPTP, txtPrezzo)

            container.addView(row)
        }
    }

    private data class PanelRefs(
        val edtBolli: EditText,
        val spnMisura: Spinner,
        val chkAll: CheckBox,
        val chkPTP: CheckBox,
        val txtPrezzo: TextView
    )

    private fun recalcTotals() {
        val container = findViewById<LinearLayout>(R.id.panelsContainer)

        var totalConsigliato = 0.0

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            val refs = child.tag as? PanelRefs ?: continue

            val bolli = refs.edtBolli.text.toString().trim().toIntOrNull() ?: 0
            val misura = refs.spnMisura.selectedItemPosition + 1

            var prezzo = lookupPrice(misura, bolli).toDouble()

            // Alluminio +30%
            if (refs.chkAll.isChecked) prezzo *= 1.30

            // PTP = sconto 30% sul pannello
            if (refs.chkPTP.isChecked) prezzo *= 0.70

            prezzo = prezzo.roundToInt().toDouble()
            refs.txtPrezzo.text = "Prezzo pannello: ${prezzo.toInt()} €"

            totalConsigliato += prezzo
        }

        // smontaggio se attivo
        val chkSm = findViewById<CheckBox>(R.id.chkSmontaggio)
        val edtSmEuro = findViewById<EditText>(R.id.edtSmontaggioEuro)
        if (chkSm.isChecked) {
            val sm = edtSmEuro.text.toString().trim().toIntOrNull() ?: 0
            totalConsigliato += sm.toDouble()
        }

        // aggiorna totale consigliato
        val txtTot = findViewById<TextView>(R.id.txtTotaleConsigliato)
        txtTot.text = "Totale consigliato: ${totalConsigliato.roundToInt()} €"

                // IVA + sconto effettivo (Fusione App Beta)
        val ivaPercent = findViewById<EditText>(R.id.edtIvaPercent)
            .text.toString().trim().replace(",", ".").toDoubleOrNull() ?: 22.0
        val ivaCompresa = findViewById<CheckBox>(R.id.chkIvaCompresa).isChecked

        val prezzoInserito = findViewById<EditText>(R.id.edtPrezzoReale)
            .text.toString().trim().replace(",", ".").toDoubleOrNull() ?: 0.0

        val consigliatoNetto = totalConsigliato
        val consigliatoLordo = VatCalculator.netToGross(consigliatoNetto, ivaPercent).totaleLordo

        val vatResult = if (ivaCompresa) {
            // l'utente inserisce il totale "fatto e finito"
            VatCalculator.grossToNet(prezzoInserito, ivaPercent)
        } else {
            // l'utente inserisce l'imponibile
            VatCalculator.netToGross(prezzoInserito, ivaPercent)
        }

        // breakdown a video
        val txtIva = findViewById<TextView>(R.id.txtIvaBreakdown)
        txtIva.text = "Imponibile: ${vatResult.baseNetto} € | IVA: ${vatResult.iva} € | Totale: ${vatResult.totaleLordo} €"

        // sconto coerente: confronto nello stesso mondo (netto o lordo)
        val consigliatoCmp = if (ivaCompresa) consigliatoLordo else consigliatoNetto
        val realeCmp = if (ivaCompresa) vatResult.totaleLordo else vatResult.baseNetto

        val diff = (consigliatoCmp - realeCmp)
        val perc = VatCalculator.discountPercent(consigliatoCmp, realeCmp)

        val txtSconto = findViewById<TextView>(R.id.txtScontoEffettivo)
        txtSconto.text = "Sconto effettivo: ${diff.roundToInt()} € (${perc.roundToInt()}%)"

    }

    private fun lookupPrice(misura: Int, bolli: Int): Int {
        if (bolli <= 0) return 0
        val row = listino.firstOrNull { it.misura == misura && bolli in it.min..it.max }
        return row?.prezzo ?: 0
    }

    private fun loadListinoOrDefault(): List<ListinoRow> {
        // prova a leggere dal tuo Storage (chiave: custom_listino_json)
        val raw = try {
            Storage.getString(this, "custom_listino_json", "")
        } catch (e: Exception) {
            ""
        }

        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                val out = mutableListOf<ListinoRow>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out.add(
                        ListinoRow(
                            misura = o.getInt("misura"),
                            min = o.getInt("min"),
                            max = o.getInt("max"),
                            prezzo = o.getInt("prezzo")
                        )
                    )
                }
                if (out.isNotEmpty()) return out
            } catch (_: Exception) {}
        }

        // DEFAULT MINIMO (solo per non avere 0 ovunque)
        // Poi lo sostituiamo con il tuo listino 2026 completo fino a 600.
        return listOf(
            ListinoRow(1, 1, 2, 44),
            ListinoRow(1, 3, 5, 71),
            ListinoRow(1, 6, 10, 99),
            ListinoRow(2, 1, 2, 79),
            ListinoRow(2, 3, 5, 107),
            ListinoRow(2, 6, 10, 151),
            ListinoRow(3, 1, 2, 123),
            ListinoRow(3, 3, 5, 158),
            ListinoRow(3, 6, 10, 201),
        )
    }

    private fun simpleWatcher(onChange: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { onChange() }
        }
    }
}
