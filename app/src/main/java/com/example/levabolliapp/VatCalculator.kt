package com.example.levabolliapp

import kotlin.math.round

data class VatResult(
    val baseNetto: Double,
    val iva: Double,
    val totaleLordo: Double
)

object VatCalculator {

    fun netToGross(net: Double, ivaPercent: Double): VatResult {
        val aliquota = ivaPercent / 100.0
        val gross = net * (1.0 + aliquota)
        val iva = gross - net
        return VatResult(round2(net), round2(iva), round2(gross))
    }

    fun grossToNet(gross: Double, ivaPercent: Double): VatResult {
        val aliquota = ivaPercent / 100.0
        val net = gross / (1.0 + aliquota)
        val iva = gross - net
        return VatResult(round2(net), round2(iva), round2(gross))
    }

    fun discountPercent(consigliato: Double, reale: Double): Double {
        if (consigliato <= 0.0) return 0.0
        val pct = (1.0 - (reale / consigliato)) * 100.0
        return round2(pct)
    }

    private fun round2(x: Double): Double = round(x * 100.0) / 100.0
}
