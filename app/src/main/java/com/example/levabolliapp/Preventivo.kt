package com.example.levabolliapp

data class Preventivo(
    val id: String,
    val createdAt: Long,
    val consigliatoNetto: Double,
    val prezzoRealeInserito: Double,
    val ivaPercent: Double,
    val ivaCompresa: Boolean
)
