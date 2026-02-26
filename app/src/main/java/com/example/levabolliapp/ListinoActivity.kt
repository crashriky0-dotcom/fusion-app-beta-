package com.example.levabolliapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class ListinoActivity : AppCompatActivity() {

    private lateinit var edtListino: EditText
    private lateinit var btnSalva: Button
    private lateinit var btnReset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listino)

        edtListino = findViewById(R.id.edtListino)
        btnSalva = findViewById(R.id.btnSalvaListino)
        btnReset = findViewById(R.id.btnRipristina)

        loadListino()

        btnSalva.setOnClickListener {
            saveListino()
        }

        btnReset.setOnClickListener {
            Storage.remove(this, "custom_listino_json")
            edtListino.setText("")
        }
    }

    private fun loadListino() {
        val raw = Storage.getString(this, "custom_listino_json", "")
        if (raw.isNotBlank()) {
            val arr = JSONArray(raw)
            val sb = StringBuilder()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                sb.append("${o.getInt("misura")};${o.getInt("min")};${o.getInt("max")};${o.getInt("prezzo")}\n")
            }
            edtListino.setText(sb.toString())
        }
    }

    private fun saveListino() {
        val lines = edtListino.text.toString().split("\n")
        val arr = JSONArray()

        for (line in lines) {
            val parts = line.split(";")
            if (parts.size == 4) {
                val obj = JSONObject()
                obj.put("misura", parts[0].trim().toInt())
                obj.put("min", parts[1].trim().toInt())
                obj.put("max", parts[2].trim().toInt())
                obj.put("prezzo", parts[3].trim().toInt())
                arr.put(obj)
            }
        }

        Storage.putString(this, "custom_listino_json", arr.toString())
    }
}
