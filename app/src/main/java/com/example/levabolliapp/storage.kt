package com.example.levabolliapp

import android.content.Context

object Storage {

    private const val PREF_NAME = "levabolli_prefs"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getString(context: Context, key: String, default: String = ""): String {
        return prefs(context).getString(key, default) ?: default
    }

    fun putString(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
    }

    fun remove(context: Context, key: String) {
        prefs(context).edit().remove(key).apply()
    }


    fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean {
        return prefs(context).getBoolean(key, default)
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        prefs(context).edit().putBoolean(key, value).apply()
    }

    fun getLong(context: Context, key: String, default: Long = 0L): Long {
        return prefs(context).getLong(key, default)
    }

    fun putLong(context: Context, key: String, value: Long) {
        prefs(context).edit().putLong(key, value).apply()
    }
}
