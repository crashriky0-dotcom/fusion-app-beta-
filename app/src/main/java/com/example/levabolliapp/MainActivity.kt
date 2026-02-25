package com.example.levabolliapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnPreventivo).setOnClickListener {
            startActivity(Intent(this, PreventivoFormActivity::class.java))
        }

        findViewById<Button>(R.id.btnListino).setOnClickListener {
            startActivity(Intent(this, ListinoActivity::class.java))
        }

        findViewById<Button>(R.id.btnMisure).setOnClickListener {
            startActivity(Intent(this, MeasureActivity::class.java))
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btnInfo).setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java))
        }
    }
}
