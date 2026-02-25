package com.example.levabolliapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ShareUtils {

    fun sharePdfGeneric(context: Context, pdfUri: Uri, message: String? = null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            if (!message.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Condividi PDF"))
    }

    fun sharePdfWhatsApp(context: Context, pdfUri: Uri, message: String? = null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            if (!message.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp non trovato", Toast.LENGTH_SHORT).show()
            sharePdfGeneric(context, pdfUri, message)
        }
    }
}
