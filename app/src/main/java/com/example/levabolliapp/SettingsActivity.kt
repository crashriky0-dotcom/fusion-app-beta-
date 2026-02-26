package com.example.levabolliapp

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val edtIva = findViewById<EditText>(R.id.edtIvaPercent)
        val chkDefault = findViewById<CheckBox>(R.id.chkIvaInclusaDefault)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)
        val btnResetListino = findViewById<Button>(R.id.btnResetListino)

        val s = AppSettingsRepo.load(this)
        edtIva.setText(s.ivaPercent.toString())
        chkDefault.isChecked = s.ivaInclusaDefault

        btnSave.setOnClickListener {
            val iva = edtIva.text.toString().trim().replace(",", ".").toDoubleOrNull() ?: 22.0
            AppSettingsRepo.save(this, AppSettings(ivaPercent = iva, ivaInclusaDefault = chkDefault.isChecked))
            finish()
        }

        btnResetListino.setOnClickListener {
            Storage.remove(this, AppKeys.CUSTOM_LISTINO_JSON)
            Storage.remove(this, AppKeys.CUSTOM_LISTINO_UPDATED_AT)
        }
    }
}
