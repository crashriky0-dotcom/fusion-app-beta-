package com.example.levabolliapp

import android.content.Context

data class AppSettings(
    val ivaPercent: Double = 22.0,
    val ivaInclusaDefault: Boolean = false
)

object AppSettingsRepo {
    fun load(context: Context): AppSettings {
        val ivaStr = Storage.getString(context, AppKeys.IVA_PERCENT, "22.0")
        val iva = ivaStr.replace(",", ".").toDoubleOrNull() ?: 22.0
        val incl = Storage.getBoolean(context, AppKeys.IVA_INCLUSA_DEFAULT, false)
        return AppSettings(ivaPercent = iva, ivaInclusaDefault = incl)
    }

    fun save(context: Context, settings: AppSettings) {
        Storage.putString(context, AppKeys.IVA_PERCENT, settings.ivaPercent.toString())
        Storage.putBoolean(context, AppKeys.IVA_INCLUSA_DEFAULT, settings.ivaInclusaDefault)
    }
}
