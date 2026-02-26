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

    // Views (non-null ones that must exist in layout)
    private lateinit var chkSmontaggio: CheckBox
    private lateinit var spnCatSmontaggio: Spinner
    private lateinit var edtSmontaggioEuro: EditText
    private lateinit var panelContainer: LinearLayout

    private lateinit var txtTotaleConsigliato: TextView
    private lateinit var edtPrezzoReale: EditText
    private lateinit var txtScontoEffettivo: TextView

    // Rimontaggio (nullable to avoid crash if the layout isn't updated yet)
    private var chkRimontaggio: CheckBox? = null
    private var spnCatRimontaggio: Spinner? = null
    private var edtRimontaggioEuro: EditText? = null

    // IVA views (nullable — in case layout still missing them)
    private var chkIvaCompresa: CheckBox? = null
    private var edtIvaPercent: EditText? = null
    private var txtIvaBreakdown: TextView? = null

    // Settings repo - used to get default IVA
    private lateinit var settings: AppSettings

    // --- Smontaggio/Rimontaggio: gestione consigliato vs manuale ---
    private var smontaggioManualOverride = false
    private var rimontaggioManualOverride = false
    private var lastSmontaggioSuggested = 0.0
    private var lastRimontaggioSuggested = 0.0

    // Prezzi consigliati A/B/C/D (modifica qui se vuoi)
    private val opPrezzi = mapOf(
        "A" to 90.0,
        "B" to 120.0,
        "C" to 150.0,
        "D" to 200.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preventivo_form)

        settings = AppSettingsRepo.load(this)

        chkSmontaggio = findViewById(R.id.chkSmontaggio)
        spnCatSmontaggio = findViewById(R.id.spnCatSmontaggio)
        edtSmontaggioEuro = findViewById(R.id.edtSmontaggioEuro)
        panelContainer = findViewById(R.id.panelContainer)

        txtTotaleConsigliato = findViewById(R.id.txtTotaleConsigliato)
        edtPrezzoReale = findViewById(R.id.edtPrezzoReale)
        txtScontoEffettivo = findViewById(R.id.txtScontoEffettivo)

        // IVA optional
        chkIvaCompresa = findViewById(R.id.chkIvaCompresa)
        edtIvaPercent = findViewById(R.id.edtIvaPercent)
        txtIvaBreakdown = findViewById(R.id.txtIvaBreakdown)

        // Rimontaggio optional (add these IDs in layout to enable the feature)
        chkRimontaggio = findViewById(R.id.chkRimontaggio)
        spnCatRimontaggio = findViewById(R.id.spnCatRimontaggio)
        edtRimontaggioEuro = findViewById(R.id.edtRimontaggioEuro)

        // IVA defaults
        edtIvaPercent?.setText(settings.ivaPercent.toString())
        chkIvaCompresa?.isChecked = settings.ivaInclusaDefault

        // Setup category spinners (A/B/C/D)
        val cats = listOf("A", "B", "C", "D")
        spnCatSmontaggio.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cats).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spnCatRimontaggio?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cats).also {
            it?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Default values
        edtSmontaggioEuro.setText("0")
        edtRimontaggioEuro?.setText("0")

        // Setup smontaggio behavior
        setupOperationCost(
            chk = chkSmontaggio,
            spn = spnCatSmontaggio,
            edt = edtSmontaggioEuro,
            getManualOverride = { smontaggioManualOverride },
            setManualOverride = { smontaggioManualOverride = it },
            getLastSuggested = { lastSmontaggioSuggested },
            setLastSuggested = { lastSmontaggioSuggested = it }
        )

        // Setup rimontaggio behavior (only if IDs exist in layout)
        if (chkRimontaggio != null && spnCatRimontaggio != null && edtRimontaggioEuro != null) {
            setupOperationCost(
                chk = chkRimontaggio!!,
                spn = spnCatRimontaggio!!,
                edt = edtRimontaggioEuro!!,
                getManualOverride = { rimontaggioManualOverride },
                setManualOverride = { rimontaggioManualOverride = it },
                getLastSuggested = { lastRimontaggioSuggested },
                setLastSuggested = { lastRimontaggioSuggested = it }
            )
        }

        // Example panel rows (replace with your real panel selection UI)
        val defaultPanels = listOf("Cofano", "Montante", "Parafango", "Porta")
        for (p in defaultPanels) createPanelRow(p)

        // Listeners that trigger recalculation
        edtPrezzoReale.addTextChangedListener(simpleAfterTextChanged { recalcAndShow() })
        chkIvaCompresa?.setOnCheckedChangeListener { _, _ -> recalcAndShow() }
        edtIvaPercent?.addTextChangedListener(simpleAfterTextChanged { recalcAndShow() })

        // Initial calc
        recalcAndShow()
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
        // Allow typing money values
        edt.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        edt.filters = arrayOf(InputFilter.LengthFilter(8))

        fun applySuggested() {
            val cat = spn.selectedItem?.toString() ?: "A"
            val suggested = opPrezzi[cat] ?: 0.0
            setLastSuggested(suggested)

            if (!getManualOverride()) {
                edt.setText(if (suggested % 1.0 == 0.0) suggested.toInt().toString() else suggested.toString())
            }
        }

        // Initial state
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
                recalcAndShow()
            } else {
                setManualOverride(false)
                applySuggested()
                recalcAndShow()
            }
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

    private fun createPanelRow(nomePannello: String) {
        val ctx = this

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (8 * resources.displayMetrics.density).toInt()
            setPadding(0, pad, 0, pad)
        }

        val title = TextView(ctx).apply {
            text = nomePannello
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Spinner bolli 0..600 (qui poi lo cambiamo in campo scrivibile quando vuoi)
        val spnBolli = Spinner(ctx).apply { id = View.generateViewId() }
        val bolliValues = (0..600).toList()
        val bolliAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, bolliValues).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spnBolli.adapter = bolliAdapter

        // Spinner misura 1/2/3
        val spnMisura = Spinner(ctx).apply { id = View.generateViewId() }
        val misuraValues = listOf("Misura 1", "Misura 2", "Misura 3")
        spnMisura.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, misuraValues).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val chkAlluminio = CheckBox(ctx).apply { text = "Alluminio (+30%)" }
        val chkPTP = CheckBox(ctx).apply { text = "PTP (-30%)" }

        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val lp0 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(spnBolli, lp0)
        row.addView(spnMisura, lp0)

        container.addView(title)
        container.addView(row)
        container.addView(chkAlluminio)
        container.addView(chkPTP)

        spnBolli.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { recalcAndShow() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spnMisura.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { recalcAndShow() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        chkAlluminio.setOnCheckedChangeListener { _, _ -> recalcAndShow() }
        chkPTP.setOnCheckedChangeListener { _, _ -> recalcAndShow() }

        container.tag = PanelTag(nomePannello, spnBolli, spnMisura, chkAlluminio, chkPTP)
        panelContainer.addView(container)
    }

    private data class PanelTag(
        val name: String,
        val spnBolli: Spinner,
        val spnMisura: Spinner,
        val chkAlluminio: CheckBox,
        val chkPTP: CheckBox
    )

    private fun recalcAndShow() {
        var consigliatoNetto = 0.0

        for (i in 0 until panelContainer.childCount) {
            val child = panelContainer.getChildAt(i)
            val tag = child.tag
            if (tag is PanelTag) {
                val bolli = (tag.spnBolli.selectedItem as? Int) ?: 0
                val misuraIdx = tag.spnMisura.selectedItemPosition
                val isAll = tag.chkAlluminio.isChecked
                val isPtp = tag.chkPTP.isChecked

                var panelPrice = priceFromRules(bolli, misuraIdx + 1)
                if (isAll) panelPrice *= 1.30
                if (isPtp) panelPrice *= 0.70

                consigliatoNetto += panelPrice
            }
        }

        if (chkSmontaggio.isChecked) {
            val smontaggioVal = edtSmontaggioEuro.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            consigliatoNetto += smontaggioVal
        }

        if (chkRimontaggio?.isChecked == true) {
            val rimVal = edtRimontaggioEuro?.text?.toString()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
            consigliatoNetto += rimVal
        }

        txtTotaleConsigliato.text = "Totale consigliato: ${round2(consigliatoNetto)} €"

        val ivaPercent = edtIvaPercent?.text?.toString()?.replace(",", ".")?.toDoubleOrNull() ?: settings.ivaPercent
        val ivaIncludedChecked = chkIvaCompresa?.isChecked ?: false

        val vatResult = if (!ivaIncludedChecked) {
            VatCalculator.netToGross(consigliatoNetto, ivaPercent)
        } else {
            VatCalculator.grossToNet(consigliatoNetto, ivaPercent)
        }

        txtIvaBreakdown?.text =
            "Imponibile: ${vatResult.baseNetto} € | IVA: ${vatResult.iva} € | Totale: ${vatResult.totaleLordo} €"

        val prezzoRealeText = edtPrezzoReale.text.toString().replace(",", ".")
        val prezzoRealeVal = prezzoRealeText.toDoubleOrNull()

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

    private fun round2(x: Double): Double = kotlin.math.round(x * 100.0) / 100.0
}
