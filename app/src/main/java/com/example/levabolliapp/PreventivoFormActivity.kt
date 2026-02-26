package com.example.levabolliapp

import android.os.Bundle
import android.text.InputFilter
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.util.*

class PreventivoFormActivity : AppCompatActivity() {

    // Views (non-null ones that must exist in layout)
    private lateinit var chkSmontaggio: CheckBox
    private lateinit var spnCatSmontaggio: Spinner
    private lateinit var edtSmontaggioEuro: EditText
    private lateinit var panelCatalogContainer: LinearLayout
    private lateinit var panelContainer: LinearLayout

    private lateinit var txtTotaleConsigliato: TextView
    private lateinit var edtPrezzoReale: EditText
    private lateinit var txtScontoEffettivo: TextView

    // IVA views (nullable — in case layout still missing them)
    private var chkIvaCompresa: CheckBox? = null
    private var edtIvaPercent: EditText? = null
    private var txtIvaBreakdown: TextView? = null

    // Settings repo (optional) - used to get default IVA
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preventivo_form)

        // load settings safely
        settings = AppSettingsRepo.load(this)

        // find views (throws if the mandatory ones are missing)
        chkSmontaggio = findViewById(R.id.chkSmontaggio)
        spnCatSmontaggio = findViewById(R.id.spnCatSmontaggio)
        edtSmontaggioEuro = findViewById(R.id.edtSmontaggioEuro)
        panelCatalogContainer = findViewById(R.id.panelCatalogContainer)
        panelContainer = findViewById(R.id.panelContainer)

        txtTotaleConsigliato = findViewById(R.id.txtTotaleConsigliato)
        edtPrezzoReale = findViewById(R.id.edtPrezzoReale)
        txtScontoEffettivo = findViewById(R.id.txtScontoEffettivo)

        // IVA optional (may be null if you haven't added the widgets yet)
        chkIvaCompresa = findViewById(R.id.chkIvaCompresa)
        edtIvaPercent = findViewById(R.id.edtIvaPercent)
        txtIvaBreakdown = findViewById(R.id.txtIvaBreakdown)

        // initialize IVA UI defaults
        edtIvaPercent?.setText(settings.ivaPercent.toString())
        chkIvaCompresa?.isChecked = settings.ivaInclusaDefault

        // setup smontaggio spinner example values (A/B/C/D)
        val smontaggioCats = listOf("A", "B", "C", "D")
        spnCatSmontaggio.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            smontaggioCats
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // if you want, default smontaggio euro value (can be changed by user)
        edtSmontaggioEuro.setText("0")

        // Build catalog (all panels start NOT selected)
        buildPanelCatalog()

        // listeners to recalc totals
        edtPrezzoReale.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { recalcAndShow() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        edtSmontaggioEuro.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { recalcAndShow() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        chkSmontaggio.setOnCheckedChangeListener { _, _ -> recalcAndShow() }
        chkIvaCompresa?.setOnCheckedChangeListener { _, _ -> recalcAndShow() }
        edtIvaPercent?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { recalcAndShow() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // initial calc
        recalcAndShow()
    }

    /**
     * Creates a panel row and appends to panelContainer.
     * Each row contains:
     * - title
     * - spinner bolli (0..600)
     * - spinner misura (1/2/3)
     * - checkbox alluminio (+30%)
     * - checkbox PTP (-30%)
     *
     * This function is safe to call multiple times.
     */
    private data class PanelDefinition(
        val id: String,
        val name: String,
        val group: String,
        val hasMode: Boolean = false
    )

    private val panelCatalog: List<PanelDefinition> = listOf(
        // Frontale
        PanelDefinition("cofano", "Cofano", "Frontale"),
        // Superiore
        PanelDefinition("tetto", "Tetto", "Superiore"),
        // Sinistra
        PanelDefinition("par_ant_sx", "Parafango anteriore SX", "Lato sinistro"),
        PanelDefinition("porta_ant_sx", "Porta anteriore SX", "Lato sinistro"),
        PanelDefinition("porta_post_sx", "Porta posteriore SX", "Lato sinistro"),
        PanelDefinition("par_post_sx", "Parafango posteriore SX", "Lato sinistro"),
        PanelDefinition("montante_sx", "Montante SX", "Lato sinistro"),
        PanelDefinition("fiancata_sx", "Fiancata SX", "Lato sinistro"),
        PanelDefinition("minigonna_sx", "Minigonna SX", "Lato sinistro"),
        // Destra
        PanelDefinition("par_ant_dx", "Parafango anteriore DX", "Lato destro"),
        PanelDefinition("porta_ant_dx", "Porta anteriore DX", "Lato destro"),
        PanelDefinition("porta_post_dx", "Porta posteriore DX", "Lato destro"),
        PanelDefinition("par_post_dx", "Parafango posteriore DX", "Lato destro"),
        PanelDefinition("montante_dx", "Montante DX", "Lato destro"),
        PanelDefinition("fiancata_dx", "Fiancata DX", "Lato destro"),
        PanelDefinition("minigonna_dx", "Minigonna DX", "Lato destro"),
        // Posteriore (Baule: Intero/Alta/Bassa)
        PanelDefinition("baule", "Baule", "Posteriore", hasMode = true)
    )

    private val selectedPanelViews: MutableMap<String, View> = mutableMapOf()

    private fun buildPanelCatalog() {
        panelCatalogContainer.removeAllViews()

        var currentGroup: String? = null
        for (def in panelCatalog) {
            if (currentGroup != def.group) {
                currentGroup = def.group
                val header = TextView(this).apply {
                    text = def.group
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    textSize = 15f
                    setPadding(0, dp(10), 0, dp(4))
                }
                panelCatalogContainer.addView(header)
            }

            val cb = CheckBox(this).apply {
                text = def.name
                isChecked = false
            }

            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    addSelectedPanel(def)
                } else {
                    removeSelectedPanel(def)
                }
                recalcAndShow()
            }

            panelCatalogContainer.addView(cb)
        }
    }

    private fun addSelectedPanel(def: PanelDefinition) {
        if (selectedPanelViews.containsKey(def.id)) return
        val rowView = createPanelRow(def)
        selectedPanelViews[def.id] = rowView
        panelContainer.addView(rowView)
    }

    private fun removeSelectedPanel(def: PanelDefinition) {
        val v = selectedPanelViews.remove(def.id) ?: return
        panelContainer.removeView(v)
    }

    /**
     * Creates a panel row view.
     * - Bolli: EditText (0..600) to allow fast manual input
     * - Misura: Spinner 1/2/3
     * - Optional "Baule mode": Spinner Intero/Alta/Bassa
     * - Alluminio/PTP toggles
     */
    private fun createPanelRow(def: PanelDefinition): View {
        val ctx = this

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (8 * resources.displayMetrics.density).toInt()
            setPadding(0, pad, 0, pad)
        }

        val title = TextView(ctx).apply {
            text = def.name
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Bolli input (0..600) - manual typing is much faster than scrolling
        val edtBolli = EditText(ctx).apply {
            hint = "Bolli (0-600)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(3), MinMaxInputFilter(0, 600))
        }

        // Spinner misura 1/2/3
        val spnMisura = Spinner(ctx).apply { id = View.generateViewId() }
        val misuraValues = listOf("Misura 1", "Misura 2", "Misura 3")
        spnMisura.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, misuraValues).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Checkboxes
        val chkAlluminio = CheckBox(ctx).apply { text = "Alluminio (+30%)" }
        val chkPTP = CheckBox(ctx).apply { text = "PTP (-30%)" }

        // Optional mode for Baule
        val spnBauleMode: Spinner? = if (def.hasMode) {
            Spinner(ctx).apply {
                adapter = ArrayAdapter(
                    ctx,
                    android.R.layout.simple_spinner_item,
                    listOf("Intero", "Parte alta", "Parte bassa")
                ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            }
        } else null

        // Horizontal row: Bolli + Misura (+ Mode if present)
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val lp0 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(edtBolli, lp0)
        row.addView(spnMisura, lp0)
        if (spnBauleMode != null) row.addView(spnBauleMode, lp0)

        // add views
        container.addView(title)
        container.addView(row)
        container.addView(chkAlluminio)
        container.addView(chkPTP)

        // on change recalc
        edtBolli.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { recalcAndShow() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        spnMisura.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { recalcAndShow() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spnBauleMode?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { recalcAndShow() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        chkAlluminio.setOnCheckedChangeListener { _, _ -> recalcAndShow() }
        chkPTP.setOnCheckedChangeListener { _, _ -> recalcAndShow() }

        // tag the view with an object so we can read values later
        container.tag = PanelTag(def.id, def.name, edtBolli, spnMisura, spnBauleMode, chkAlluminio, chkPTP)

        return container
    }

    /**
     * Data holder for a panel row
     */
    private data class PanelTag(
        val id: String,
        val name: String,
        val edtBolli: EditText,
        val spnMisura: Spinner,
        val spnMode: Spinner?,
        val chkAlluminio: CheckBox,
        val chkPTP: CheckBox
    )

    /**
     * Recalculate total suggested price (netto), IVA breakdown and sconto based on the user-entered "prezzo reale".
     */
    private fun recalcAndShow() {
        // sum panels
        var consigliatoNetto = 0.0

        for (i in 0 until panelContainer.childCount) {
            val child = panelContainer.getChildAt(i)
            val tag = child.tag
            if (tag is PanelTag) {
                val bolli = (tag.edtBolli.text?.toString()?.toIntOrNull() ?: 0).coerceIn(0, 600)
                val misuraIdx = tag.spnMisura.selectedItemPosition // 0->misura1,1->misura2...
                val isAll = tag.chkAlluminio.isChecked
                val isPtp = tag.chkPTP.isChecked

                // get base price for this panel using a simple placeholder function
                var panelPrice = priceFromRules(bolli, misuraIdx + 1)

                if (isAll) panelPrice *= 1.30
                if (isPtp) panelPrice *= 0.70

                consigliatoNetto += panelPrice
            }
        }

        // smontaggio
        val includeSmontaggio = chkSmontaggio.isChecked
        val smontaggioVal = edtSmontaggioEuro.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
        if (includeSmontaggio) consigliatoNetto += smontaggioVal

        // show consigliato netto and (optionally) consigliato lordo
        txtTotaleConsigliato.text = "Totale consigliato: ${round2(consigliatoNetto)} €"

        // IVA calculations
        val ivaPercent = edtIvaPercent?.text?.toString()?.replace(",", ".")?.toDoubleOrNull() ?: settings.ivaPercent
        val ivaIncludedChecked = chkIvaCompresa?.isChecked ?: false

        val vatResult = if (!ivaIncludedChecked) {
            // consigliatoNetto is net -> calc gross
            VatCalculator.netToGross(consigliatoNetto, ivaPercent)
        } else {
            // if user stored consigliato as gross (rare), treat consigliatoNetto as gross for breakdown:
            VatCalculator.grossToNet(consigliatoNetto, ivaPercent)
        }

        // display breakdown if text exists
        txtIvaBreakdown?.text = "Imponibile: ${vatResult.baseNetto} € | IVA: ${vatResult.iva} € | Totale: ${vatResult.totaleLordo} €"

        // compute sconto: user can insert prezzo reale (either net or gross depending on checkbox)
        val prezzoRealeText = edtPrezzoReale.text.toString().replace(",", ".")
        val prezzoRealeVal = prezzoRealeText.toDoubleOrNull()

        val scontoPercent = if (prezzoRealeVal == null || prezzoRealeVal <= 0.0) {
            0.0
        } else {
            // interpret prezzoRealeVal relative to checkbox:
            val realeNetto = if (ivaIncludedChecked) {
                // prezzo reale is gross -> convert to netto to compare with consigliatoNetto (which is netto)
                VatCalculator.grossToNet(prezzoRealeVal, ivaPercent).baseNetto
            } else {
                prezzoRealeVal
            }
            VatCalculator.discountPercent(consigliatoNetto, realeNetto)
        }

        txtScontoEffettivo.text = "Sconto effettivo: ${prezzoRealeVal?.let { round2(it) } ?: 0.0} € (${round2(scontoPercent)}%)"

        // store last computed values on view tags if needed (not mandatory)
    }

    /**
     * Placeholder pricing logic:
     * Replace this with your real listino/range lookup logic (range -> price for misura).
     * For now: simple heuristic:
     *  - if bolli == 0 -> price 0
     *  - else price = bolli * baseFactor * misuraMultiplier
     *
     * This function is intentionally local and simple to guarantee compilation and immediate functionality.
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

    /**
     * Save the current preventivo to repository (simple)
     */
    private fun savePreventivo() {
        // read consigliatoNetto by calling recalc logic (we could refactor to return value)
        // We'll do a cheap recompute similar to recalcAndShow but returning consigliatoNetto:
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
        val includeSmontaggio = chkSmontaggio.isChecked
        val smontaggioVal = edtSmontaggioEuro.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
        if (includeSmontaggio) consigliatoNetto += smontaggioVal

        val prezzoReale = edtPrezzoReale.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
        val ivaPercent = edtIvaPercent?.text?.toString()?.replace(",", ".")?.toDoubleOrNull() ?: settings.ivaPercent
        val ivaCompresa = chkIvaCompresa?.isChecked ?: false

        val id = PreventivoRepository.newId()
        val now = System.currentTimeMillis()

        val p = Preventivo(
            id = id,
            createdAt = now,
            consigliatoNetto = round2(consigliatoNetto),
            prezzoRealeInserito = round2(prezzoReale),
            ivaPercent = ivaPercent,
            ivaCompresa = ivaCompresa
        )

        PreventivoRepository.upsert(this, p)
        Toast.makeText(this, "Preventivo salvato", Toast.LENGTH_SHORT).show()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /** Enforces a numeric range for EditText input */
    private class MinMaxInputFilter(private val min: Int, private val max: Int) : InputFilter {
        override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: android.text.Spanned,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            try {
                val newVal = StringBuilder(dest)
                    .replace(dstart, dend, source.subSequence(start, end).toString())
                    .toString()

                if (newVal.isBlank()) return null
                val input = newVal.toInt()
                if (input in min..max) return null
            } catch (_: Exception) {
                // ignore
            }
            return ""
        }
    }
}
