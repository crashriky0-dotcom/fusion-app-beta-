package com.example.levabolliapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object PreventivoRepository {

    fun newId(): String = UUID.randomUUID().toString()

    fun loadAll(context: Context): List<Preventivo> {
        val raw = Storage.getString(context, AppKeys.PREVENTIVI_JSON, "[]")
        val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        val out = mutableListOf<Preventivo>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                Preventivo(
                    id = o.optString("id", ""),
                    createdAt = o.optLong("createdAt", 0L),
                    consigliatoNetto = o.optDouble("consigliatoNetto", 0.0),
                    prezzoRealeInserito = o.optDouble("prezzoRealeInserito", 0.0),
                    ivaPercent = o.optDouble("ivaPercent", 22.0),
                    ivaCompresa = o.optBoolean("ivaCompresa", false)
                )
            )
        }
        return out.sortedByDescending { it.createdAt }
    }

    fun upsert(context: Context, p: Preventivo) {
        val list = loadAll(context).toMutableList()
        val idx = list.indexOfFirst { it.id == p.id }
        if (idx >= 0) list[idx] = p else list.add(0, p)
        saveAll(context, list)
    }

    fun delete(context: Context, id: String) {
        val list = loadAll(context).filterNot { it.id == id }
        saveAll(context, list)
    }

    private fun saveAll(context: Context, list: List<Preventivo>) {
        val arr = JSONArray()
        for (p in list) {
            val o = JSONObject()
            o.put("id", p.id)
            o.put("createdAt", p.createdAt)
            o.put("consigliatoNetto", p.consigliatoNetto)
            o.put("prezzoRealeInserito", p.prezzoRealeInserito)
            o.put("ivaPercent", p.ivaPercent)
            o.put("ivaCompresa", p.ivaCompresa)
            arr.put(o)
        }
        Storage.putString(context, AppKeys.PREVENTIVI_JSON, arr.toString())
    }
}
