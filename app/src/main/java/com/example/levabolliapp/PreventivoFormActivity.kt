package com.example.levabolliapp

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class PreventivoFormActivity : AppCompatActivity() {

    // --- Dati base ---
    private lateinit var edtCliente: EditText
    private lateinit var edtTecnico: EditText
    private lateinit var edtData: EditText
    private lateinit var edtMarca: EditText
    private lateinit var edtModello: EditText
    private lateinit var edtTargaTelaio: EditText

    // --- Catalogo pannelli selezionabili + container pannelli selezionati ---
    private lateinit var panelCatalogContainer: LinearLayout
    private lateinit var panelContainer: LinearLayout

    // --- Smontaggio / Rimontaggio separati ---
    private lateinit var chkSmontaggio: CheckBox
    private lateinit var spnCatSmontaggio: Spinner
    private lateinit var edtSmontaggioEuro: EditText

    private lateinit var chkRimontaggio: CheckBox
    private lateinit var spnCatRimontaggio: Spinner
    private lateinit var edtRimontaggioEuro: EditText

    private lateinit var txtHintSmontaggio: TextView

    // --- Prezzi / IVA / Sconto ---
    private lateinit var txtTotaleConsigliato: TextView
    private lateinit var edtPrezzoReale: EditText
    private lateinit var txtScontoEffettivo: TextView

    private lateinit var chkIvaCompresa: CheckBox
    private lateinit var edtIvaPercent: EditText
    private lateinit var txtIvaBreakdown: TextView

    private lateinit var btnSalvaPreventivo: Button

    // --- Settings ---
    private val settings by lazy { SettingsRepo(this) }
    private val preventiviRepo by lazy { PreventiviRepo(this) }

    // Prezzi consigliati A/B/C/D (stessi per smontaggio e rimontaggio)
    private val opPrezzi = mapOf(
        "A" to 90.0,
        "B" to 120.0,
        "C" to 150.0,
        "D" to 200.0
    )

    // Override manuali
    private var smontaggioManualOverride = false
    private var rimontaggioManualOverride = false
    private var lastSmontaggioSuggested = 0.0
    private var lastRimontaggioSuggested = 0.0

    // --- Modello pannelli selezionabili (attivabili) ---
    data class PanelDef(val group: String, val label: String, val key: String)

    // ID “chiave” pannelli (usali come vuoi per salvarli)
    private val panelCatalog = listOf(
        // Anteriore
        PanelDef("Anteriore", "Cofano", "cofano"),
        PanelDef("Anteriore", "Parafango anteriore SX", "parafango_ant_sx"),
        PanelDef("Anteriore", "Parafango anteriore DX", "parafango_ant_dx"),

        // Tetto / centrale
        PanelDef("Centrale", "Tetto", "tetto"),
        PanelDef("Centrale", "Montante SX", "montante_sx"),
        PanelDef("Centrale", "Montante DX", "montante_dx"),

        // Lato sinistro
        PanelDef("Lato sinistro", "Porta anteriore SX", "porta_ant_sx"),
        PanelDef("Lato sinistro", "Porta posteriore SX", "porta_post_sx"),
        PanelDef("Lato sinistro", "Parafango posteriore SX", "parafango_post_sx"),
        PanelDef("Lato sinistro", "Fiancata SX", "fiancata_sx"),
        PanelDef("Lato sinistro", "Minigonna SX", "minigonna_sx"),

        // Lato destro
        PanelDef("Lato destro", "Porta anteriore DX", "porta_ant_dx"),
        PanelDef("Lato destro", "Porta posteriore DX", "porta_post_dx"),
        PanelDef("Lato destro", "Parafango posteriore DX", "parafango_post_dx"),
        PanelDef("Lato destro", "Fiancata DX", "fiancata_dx"),
        PanelDef("Lato destro", "Minigonna DX", "minigonna_dx"),

        // Posteriore
        PanelDef("Posteriore", "Baule", "baule"),
        PanelDef("Posteriore", "Baule parte alta", "baule_alta"),
        PanelDef("Posteriore", "Baule parte bassa", "baule_bassa")
    )

    // Tag UI pannello selezionato
    data class PanelTag(
        val key: String,
        val label: String,
        val edtBolli: EditText,
        val spnMisura: Spinner,
        val chkAlluminio: CheckBox,
        val chkPTP: CheckBox
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preventivo_form)

        // Dati base
        edtCliente = findViewById(R.id.edtCliente)
        edtTecnico = findViewById(R.id.edtTecnico)
        edtData = findViewById(R.id.edtData)
        edtMarca = findViewById(R.id.edtMarca)
        edtModello = findViewById(R.id.edtModello)
        edtTargaTelaio = findViewById(R.id.edtTargaTelaio)

        // Containers
        panelCatalogContainer = findViewById(R.id.panelCatalogContainer)
        panelContainer = findViewById(R.id.panelContainer)

        // Smontaggio
        chkSmontaggio = findViewById(R.id.chkSmontaggio)
        spnCatSmontaggio = findViewById(R.id.spnCatSmontaggio)
        edtSmontaggioEuro = findViewById(R.id.edtSmontaggioEuro)

        // Rimontaggio
        chkRimontaggio = findViewById(R.id.chkRimontaggio)
        spnCatRimontaggio = findViewById(R.id.spnCatRimontaggio)
        edtRimontaggioEuro = findViewById(R.id.edtRimontaggioEuro)

        txtHintSmontaggio = findViewById(R.id.txtHintSmontaggio)

        // Prezzo/IVA/Sconto
        txtTotaleConsigliato = findViewById(R.id.txtTotaleConsigliato)
        edtPrezzoReale = findViewById(R.id.edtPrezzoReale)
        txtScontoEffettivo = findViewById(R.id.txtScontoEffettivo)

        chkIvaCompresa = findViewById(R.id.chkIvaCompresa)
        edtIvaPercent = findViewById(R.id.edtIvaPercent)
        txtIvaBreakdown = findViewById(R.id.txtIvaBreakdown)

        btnSalvaPreventivo = findViewById(R.id.btnSalvaPreventivo)

        // Default IVA
        edtIvaPercent.setText(settings.ivaPercent.toString())

        // 1) Costruisci catalogo pannelli (checkbox)
        buildPanelCatalog()

        // 2) Spinner A/B/C/D per smontaggio e rimontaggio
        val cats = listOf("A", "B", "C", "D")
        val adapterCats = ArrayAdapter(this, android.R.layout.simple_spinner_item, cats).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spnCatSmontaggio.adapter = adapterCats
        spnCatRimontaggio.adapter = adapterCats

        // default
        edtSmontaggioEuro.setText("0")
        edtRimontaggioEuro.setText("0")
        txtHintSmontaggio.text = "A=90€  B=120€  C=150€  D=200€ (modificabile)"

        // Setup operazioni
        setupOperationCost(
            chk = chkSmontaggio,
            spn = spnCatSmontaggio,
            edt = edtSmontaggioEuro,
            getManualOverride = { smontaggioManualOverride },
            setManualOverride = { smontaggioManualOverride = it },
            getLastSuggested = { lastSmontaggioSuggested },
            setLastSuggested = { lastSmontaggioSuggested = it }
        )

        setupOperationCost(
            chk = chkRimontaggio,
            spn = spnCatRimontaggio,
            edt = edtRimontaggioEuro,
            getManualOverride = { rimontaggioManualOverride },
            setManualOverride = { rimontaggioManualOverride = it },
            getLastSuggested = { lastRimontaggioSuggested },
            setLastSuggested = { lastRimontaggioSuggested = it }
        )

        // Listener prezzo reale / IVA / checkbox IVA
        edtPrezzoReale.addTextChangedListener(simpleAfterTextChanged { recalcAndShow() })
        edtIvaPercent.addTextChangedListener(simpleAfterTextChanged { recalcAndShow() })
        chkIvaCompresa.setOnCheckedChangeListener { _, _ -> recalcAndShow() }

        btnSalvaPreventivo.setOnClickListener { savePreventivo() }

        recalcAndShow()
    }

    private fun buildPanelCatalog() {
        panelCatalogContainer.removeAllViews()

        var currentGroup: String? = null

        panelCatalog.forEach { def ->
            if (currentGroup != def.group) {
                currentGroup = def.group

                val title = TextView(this).apply {
                    text = def.group
                    textSize = 16f
                    setPadding(0, 12, 0, 6)
                }
                panelCatalogContainer.addView(title)
            }

            val chk = CheckBox(this).apply {
                text = def.label
            }

            chk.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) addPanelRow(def) else removePanelRow(def.key)
                recalcAndShow()
            }

            panelCatalogContainer.addView(chk)
        }
    }

    private fun addPanelRow(def: PanelDef) {
        // evita duplicati
        for (i in 0 until panelContainer.childCount) {
            val tag = panelContainer.getChildAt(i).tag
            if (tag is PanelTag && tag.key == def.key) return
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val title = TextView(this).apply {
            text = def.label
            textSize = 18f
            setPadding(0, 0, 0, 6)
        }

        val line1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val edtBolli = EditText(this).apply {
            hint = "0"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(3)) // fino a 600 (3 cifre)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // clamp 0..600 mentre scrivi
        edtBolli.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val v = s?.toString()?.toIntOrNull()
                if (v != null && v > 600) {
                    edtBolli.setText("600")
                    edtBolli.setSelection(edtBolli.text.length)
                }
                recalcAndShow()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val spnMisura = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val misure = listOf("Misura 1", "Misura 2", "Misura 3")
        val adapterMisure = ArrayAdapter(this, android.R.layout.simple_spinner_item, misure).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spnMisura.adapter = adapterMisure
        spnMisura.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                recalcAndShow()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        line1.addView(edtBolli)
        line1.addView(spnMisura)

        val chkAll = CheckBox(this).apply {
            text = "Alluminio (+30%)"
        }
        chkAll.setOnCheckedChangeListener { _, _ -> recalcAndShow() }

        val chkPtp = CheckBox(this).apply {
            text = "PTP (-30%)"
        }
        chkPtp.setOnCheckedChangeListener { _, _ -> recalcAndShow() }

        row.addView(title)
        row.addView(line1)
        row.addView(chkAll)
        row.addView(chkPtp)

        row.tag = PanelTag(
            key = def.key,
            label = def.label,
            edtBolli = edtBolli,
            spnMisura = spnMisura,
            chkAlluminio = chkAll,
            chkPTP = chkPtp
        )

        panelContainer.addView(row)
    }

    private fun removePanelRow(key: String) {
        for (i in panelContainer.childCount - 1 downTo 0) {
            val child = panelContainer.getChildAt(i)
            val tag = child.tag
            if (tag is PanelTag && tag.key == key) {
                panelContainer.removeViewAt(i)
                return
            }
        }
    }

    private fun setupOperationCost(
        chk: CheckBox,
        spn: Spinner,
        edt: EditText,
        getManualOverride: () -> Boolean,
        setManualOverride: (Boolean) -> Unit,
        getLastSuggested: () -> Double,
        setLastSuggested: (Double) -> Unit
    ) {
        fun applySuggested() {
            val cat = spn.selectedItem?.toString() ?: "A"
            val suggested = opPrezzi[cat] ?: 0.0
            setLastSuggested(suggested)

            if (!getManualOverride()) {
                edt.setText(if (suggested % 1.0 == 0.0) suggested.toInt().toString() else suggested.toString())
            }
        }

        // stato iniziale
        spn.isEnabled = chk.isChecked
        edt.isEnabled = chk.isChecked

        if (!chk.isChecked) {
            edt.setText("0")
            setManualOverride(false)
        } else {
            setManualOverride(false)
            applySuggested()
        }

        chk.setOnCheckedChangeListener { _, isChecked ->
            spn.isEnabled = isChecked
            edt.isEnabled = isChecked

            if (!isChecked) {
                setManualOverride(false)
                edt.setText("0")
            } else {
                setManualOverride(false)
                applySuggested()
            }
            recalcAndShow()
        }

        spn.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!chk.isChecked) return
                applySuggested()
                recalcAndShow()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        edt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!chk.isChecked) return
                val txt = s?.toString()?.trim().orEmpty()
                val v = txt.replace(",", ".").toDoubleOrNull()
                val suggested = getLastSuggested()
                setManualOverride(v != null && abs(v - suggested) > 0.001)
                recalcAndShow()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun simpleAfterTextChanged(block: () -> Unit): TextWatcher =
        object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = block()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

    private fun recalcAndShow() {
        var consigliatoNetto = 0.0

        // pannelli
        for (i in 0 until panelContainer.childCount) {
            val child = panelContainer.getChildAt(i)
            val tag = child.tag
            if (tag is PanelTag) {
                val bolli = (tag.edtBolli.text?.toString()?.toIntOrNull() ?: 0).coerceIn(0, 600)
                val misuraIdx = tag.spnMisura.selectedItemPosition // 0..2
                val isAll = tag.chkAlluminio.isChecked
                val isPtp = tag.chkPTP.isChecked

                var panelPrice = priceFromRules(bolli, misuraIdx + 1)
                if (isAll) panelPrice *= 1.30
                if (isPtp) panelPrice *= 0.70

                consigliatoNetto += panelPrice
            }
        }

        // smontaggio
        if (chkSmontaggio.isChecked) {
            consigliatoNetto += edtSmontaggioEuro.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
        }

        // rimontaggio
        if (chkRimontaggio.isChecked) {
            consigliatoNetto += edtRimontaggioEuro.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
        }

        txtTotaleConsigliato.text = "Totale consigliato: ${round2(consigliatoNetto)} €"

        // IVA
        val ivaPercent = edtIvaPercent.text.toString().replace(",", ".").toDoubleOrNull() ?: settings.ivaPercent
        val ivaIncludedChecked = chkIvaCompresa.isChecked

        val vatResult = if (!ivaIncludedChecked) {
            VatCalculator.netToGross(consigliatoNetto, ivaPercent)
        } else {
            VatCalculator.grossToNet(consigliatoNetto, ivaPercent)
        }

        txtIvaBreakdown.text =
            "Imponibile: ${vatResult.baseNetto} € | IVA: ${vatResult.iva} € | Totale: ${vatResult.totaleLordo} €"

        // Sconto effettivo: prezzo reale può essere netto o lordo in base alla checkbox
        val prezzoRealeVal = edtPrezzoReale.text.toString().replace(",", ".").toDoubleOrNull()

        val scontoPercent = if (prezzoRealeVal == null || prezzoRealeVal <= 0.0) {
            0.0
        } else {
            val realeNetto = if (ivaIncludedChecked) {
                VatCalculator.grossToNet(prezzoRealeVal, ivaPercent).baseNetto
            } else {
                prezzoRealeVal
            }
            VatCalculator.discountPercent(consigliatoNetto, realeNetto)
        }

        txtScontoEffettivo.text =
            "Sconto effettivo: ${prezzoRealeVal?.let { round2(it) } ?: 0.0} € (${round2(scontoPercent)}%)"
    }

    /**
     * Logica prezzo (placeholder) – qui ci agganci la tua tabella/listino vero.
     */
    private fun priceFromRules(bolli: Int, misura: Int): Double {
        if (bolli <= 0) return 0.0

        val baseFactor = when {
            bolli <= 5 -> 8.0
            bolli <= 10 -> 6.5
            bolli <= 20 -> 5.5
            bolli <= 50 -> 4.0
            bolli <= 100 -> 3.0
            bolli <= 200 -> 2.2
            bolli <= 400 -> 1.8
            else -> 1.5
        }

        val misuraMultiplier = when (misura) {
            1 -> 1.0
            2 -> 1.25
            3 -> 1.6
            else -> 1.0
        }

        return round2(bolli * baseFactor * misuraMultiplier)
    }

    private fun round2(x: Double): Double = Math.round(x * 100.0) / 100.0

    private fun savePreventivo() {
        // ricalcolo veloce coerente con recalcAndShow
        var consigliatoNetto = 0.0
        for (i in 0 until panelContainer.childCount) {
            val child = panelContainer.getChildAt(i)
            val tag = child.tag
            if (tag is PanelTag) {
                val bolli = (tag.edtBolli.text?.toString()?.toIntOrNull() ?: 0).coerceIn(0, 600)
                val misuraIdx = tag.spnMisura.selectedItemPosition
                var panelPrice = priceFromRules(bolli, misuraIdx + 1)
                if (tag.chkAlluminio.isChecked) panelPrice *= 1.30
                if (tag.chkPTP.isChecked) panelPrice *= 0.70
                consigliatoNetto += panelPrice
            }
        }
        if (chkSmontaggio.isChecked) {
            consigliatoNetto += edtSmontaggioEuro.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
        }
        if (chkRimontaggio.isChecked) {
            consigliatoNetto += edtRimontaggioEuro.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
        }

        val prezzoReale = edtPrezzoReale.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0

        val preventivo = Preventivo(
            cliente = edtCliente.text.toString(),
            tecnico = edtTecnico.text.toString(),
            data = edtData.text.toString(),
            marca = edtMarca.text.toString(),
            modello = edtModello.text.toString(),
            targaTelaio = edtTargaTelaio.text.toString(),
            totaleConsigliatoNetto = consigliatoNetto,
            prezzoReale = prezzoReale,
            ivaPercent = edtIvaPercent.text.toString().replace(",", ".").toDoubleOrNull() ?: settings.ivaPercent,
            ivaCompresa = chkIvaCompresa.isChecked
        )

        preventiviRepo.add(preventivo)
        Toast.makeText(this, "Preventivo salvato!", Toast.LENGTH_SHORT).show()
    }
}
