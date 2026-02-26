package com.example.levabolliapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.levabolliapp.databinding.ActivityMeasureBinding
import kotlin.math.pow
import kotlin.math.sqrt

class MeasureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMeasureBinding

    companion object {
        private const val REQUEST_PICK_IMAGE = 1001
    }

    private var bitmap: Bitmap? = null

    // diametro reale in mm della moneta scelta (1€ o 2€)
    private var referenceDiameterMm = 0.0

    private enum class Stage {
        NONE,
        REF_POINT1,
        REF_POINT2,
        DENT_POINT1,
        DENT_POINT2
    }

    private var currentStage = Stage.NONE

    private var refP1: PointF? = null
    private var refP2: PointF? = null
    private var dentP1: PointF? = null
    private var dentP2: PointF? = null

    private var pxPerMm: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMeasureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectImage.setOnClickListener {
            pickImageFromGallery()
        }

        binding.imageView.setOnTouchListener { v, event ->
            handleImageTouch(v, event)
            true
        }

        updateStatus(getString(R.string.measure_photo_instruction))
        updateResult(getString(R.string.diameter_result_placeholder))
    }

    private fun updateStatus(text: String) {
        binding.txtStatus.text = text
    }

    private fun updateResult(text: String) {
        binding.txtResult.text = text
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                val bmp = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                bitmap = bmp
                binding.imageView.setImageBitmap(bmp)

                // reset stato
                refP1 = null
                refP2 = null
                dentP1 = null
                dentP2 = null
                pxPerMm = null
                currentStage = Stage.NONE

                scegliMoneta()
            }
        }
    }

    private fun scegliMoneta() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.choose_coin))

        val opzioni = arrayOf(
            getString(R.string.coin_1euro),
            getString(R.string.coin_2euro)
        )

        builder.setItems(opzioni) { _, which ->
            referenceDiameterMm = if (which == 0) 23.25 else 25.75

            currentStage = Stage.REF_POINT1
            updateStatus(
                getString(R.string.touch_coin)
            )
            updateResult(getString(R.string.diameter_result_placeholder))
        }

        builder.setCancelable(false)
        builder.show()
    }

    private fun handleImageTouch(view: View, event: MotionEvent) {
        if (event.action != MotionEvent.ACTION_DOWN) return

        val imageView = binding.imageView
        val drawable = imageView.drawable ?: return

        val touchPoint = PointF(event.x, event.y)
        val imgPoint = viewCoordToImageCoord(imageView, touchPoint) ?: return

        when (currentStage) {
            Stage.NONE -> {
                // ancora non è stata scelta la moneta o non c'è immagine
            }
            Stage.REF_POINT1 -> {
                refP1 = imgPoint
                currentStage = Stage.REF_POINT2
                updateStatus(getString(R.string.status_coin_second_point))
            }
            Stage.REF_POINT2 -> {
                refP2 = imgPoint
                val refDistPx = distance(refP1!!, refP2!!)
                pxPerMm = refDistPx / referenceDiameterMm
                currentStage = Stage.DENT_POINT1
                updateStatus(getString(R.string.status_dent_first_point))
            }
            Stage.DENT_POINT1 -> {
                dentP1 = imgPoint
                currentStage = Stage.DENT_POINT2
                updateStatus(getString(R.string.status_dent_second_point))
            }
            Stage.DENT_POINT2 -> {
                dentP2 = imgPoint
                val scale = pxPerMm ?: return
                val dentDistPx = distance(dentP1!!, dentP2!!)
                val diametroMm = dentDistPx / scale
                updateResult(getString(R.string.diameter_result_value, diametroMm))

                // avviso bollo grande (es. > 50mm)
                if (diametroMm > 50.0) {
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.big_dent_title))
                        .setMessage(getString(R.string.big_dent_message, String.format("%.1f", diametroMm)))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }

                updateStatus(
                    getString(R.string.status_dent_repeat)
                )
                currentStage = Stage.DENT_POINT1
            }
        }
    }

    private fun viewCoordToImageCoord(imageView: View, point: PointF): PointF? {
        val iv = imageView as? android.widget.ImageView ?: return null
        val drawable = iv.drawable ?: return null

        val matrix = iv.imageMatrix
        val values = FloatArray(9)
        matrix.getValues(values)

        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]

        val x = (point.x - transX) / scaleX
        val y = (point.y - transY) / scaleY

        val bmpWidth = drawable.intrinsicWidth
        val bmpHeight = drawable.intrinsicHeight

        if (x < 0 || y < 0 || x > bmpWidth || y > bmpHeight) {
            return null
        }

        return PointF(x, y)
    }

    private fun distance(p1: PointF, p2: PointF): Double {
        val dx = (p2.x - p1.x).toDouble()
        val dy = (p2.y - p1.y).toDouble()
        return sqrt(dx.pow(2.0) + dy.pow(2.0))
    }
}
