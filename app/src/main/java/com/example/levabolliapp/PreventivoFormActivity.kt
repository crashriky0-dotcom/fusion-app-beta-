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
