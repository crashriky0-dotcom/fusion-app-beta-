package com.example.levabolliapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs
import kotlin.math.round

class PreventivoFormActivity : AppCompatActivity() {

    private lateinit var chkSmontaggio: CheckBox
    private lateinit var spnCatSmontaggio: Spinner
    private lateinit var edtSmontaggioEuro: EditText
    private lateinit var panelContainer: LinearLayout

    private lateinit var txtTotaleConsigliato: TextView
    private lateinit var edtPrezzoReale: EditText
    private lateinit var txtScontoEffettivo: TextView

    // --- SMONTAGGIO: gestione consigliato/manuale ---
    private var smontaggioManualOverride = false
    private var lastSmontaggioSuggested = 0.0

    private val smontaggioPrezzi = mapOf(
        "A" to 90.0,
        "B" to 120.0,
        "C" to 150.0,
        "D" to 200.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preventivo_form)

        chkSmontaggio = findViewById(R.id.chkSmontaggio)
        spnCatSmontaggio = findViewById(R.id.spnCatSmontaggio)
        edtSmontaggioEuro = findViewById(R.id.edtSmontaggioEuro)
        panelContainer = findViewById(R.id.panelContainer)

        txtTotaleConsigliato = findViewById(R.id.txtTotaleConsigliato)
        edtPrezzoReale = findViewById(R.id.edtPrezzoReale)
        txtScontoEffettivo = findViewById(R.id.txtScontoEffettivo)

        // Spinner categorie A/B/C/D
        val cats = listOf("A", "B", "C", "D")
        spnCatSmontaggio.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, cats).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // Stato iniziale
        spnCatSmontaggio.isEnabled = chkSmontaggio.isChecked
        edtSmontaggioEuro.isEnabled = chkSmontaggio.isChecked
        edtSmontaggioEuro.setText("0")

        fun applySmontaggioSuggestedPrice() {
            val cat = spnCatSmontaggio.selectedItem?.toString() ?: "A"
            val suggested = smontaggioPrezzi[cat] ?: 0.0
            lastSmontaggioSuggested = suggested

            if (!smontaggioManualOverride) {
                edtSmontaggioEuro.setText(
                    if (suggested % 1.0 == 0.0)
                        suggested.toInt().toString()
                    else
                        suggested.toString()
                )
            }

            recalcAndShow()
        }

        // Checkbox comportamento
        chkSmontaggio.setOnCheckedChangeListener { _, isChecked ->
            spnCatSmontaggio.isEnabled = isChecked
            edtSmontaggioEuro.isEnabled = isChecked

            if (!isChecked) {
                smontaggioManualOverride = false
                edtSmontaggioEuro.setText("0")
            } else {
                smontaggioManualOverride = false
                applySmontaggioSuggestedPrice()
            }

            recalcAndShow()
        }

        // Cambio categoria
        spnCatSmontaggio.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (!chkSmontaggio.isChecked) return
                    applySmontaggioSuggestedPrice()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        // Override manuale
        edtSmontaggioEuro.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!chkSmontaggio.isChecked) return

                val txt = s?.toString()?.trim().orEmpty()
                val value = txt.replace(",", ".").toDoubleOrNull()

                smontaggioManualOverride =
                    (value != null && abs(value - lastSmontaggioSuggested) > 0.001)

                recalcAndShow()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun recalcAndShow() {

        var totale = 0.0

        if (chkSmontaggio.isChecked) {
            val smontaggio =
                edtSmontaggioEuro.text.toString().replace(",", ".").toDoubleOrNull()
                    ?: 0.0
            totale += smontaggio
        }

        txtTotaleConsigliato.text =
            "Totale consigliato: ${round2(totale)} €"

        val prezzoReale =
            edtPrezzoReale.text.toString().replace(",", ".").toDoubleOrNull()
                ?: 0.0

        val sconto =
            if (totale > 0) ((totale - prezzoReale) / totale) * 100 else 0.0

        txtScontoEffettivo.text =
            "Sconto effettivo: ${round2(sconto)}%"
    }

    private fun round2(x: Double): Double =
        round(x * 100.0) / 100.0
}
