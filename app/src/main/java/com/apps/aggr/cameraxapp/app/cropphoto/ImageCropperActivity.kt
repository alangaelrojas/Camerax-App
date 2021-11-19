package com.apps.aggr.cameraxapp.app.cropphoto

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.apps.aggr.cameraxapp.R
import com.apps.aggr.cameraxapp.utils.Constants.CROPPED_PATH_FOLDER
import com.apps.aggr.cameraxapp.utils.Constants.getOutputMediaFile
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.takusemba.cropme.CropLayout
import com.takusemba.cropme.OnCropListener
import kotlinx.android.synthetic.main.activity_image_cropper.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class ImageCropperActivity : AppCompatActivity(), OnCropListener {

    private var fileName: String? = null

    private val backButton by lazy { findViewById<ImageView>(R.id.cross) }
    private val parent by lazy { findViewById<ConstraintLayout>(R.id.container) }
    private val cropLayout by lazy { findViewById<CropLayout>(R.id.crop_view) }
    private val cropButton by lazy { findViewById<ImageView>(R.id.crop) }
    private val progressBar by lazy { findViewById<ProgressBar>(R.id.progress) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_cropper)
        backButton.setOnClickListener { finish() }

        val image = intent.getStringExtra(IMAGE_EXTRA_NAME)

        image?.let {
            val mSaveBit =  File(it)
            val filePath = mSaveBit.path
            fileName = mSaveBit.name
            val bitmap = BitmapFactory.decodeFile(filePath)
            val rotated = rotateImage(90F, bitmap)
            cropLayout.setBitmap(rotated)
            cropLayout.addOnCropListener(this)
        }
    }

    override fun onStart() {
        super.onStart()

        cropButton.setOnClickListener {
            if (cropLayout.isOffFrame()) {
                Snackbar.make(parent, R.string.error_image_is_off_frame, Snackbar.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
            progressBar.visibility = View.VISIBLE
            cropLayout.crop()
        }
    }

    override fun onSuccess(bitmap: Bitmap) {
        bitmap.let {
            progressBar.visibility = View.GONE
            val view = layoutInflater.inflate(R.layout.dialog_result, null)
            view.findViewById<ImageView>(R.id.image).setImageBitmap(bitmap)
            saveImageLocally(bitmap)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_result)
                .setView(view)
                .setPositiveButton(R.string.dialog_button_close) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    override fun onFailure(e: Exception) {
        Snackbar.make(parent, R.string.error_failed_to_clip_image, Snackbar.LENGTH_LONG).show()
    }

    private fun saveImageLocally(bitmap: Bitmap){
        try {
            val pictureFile: File? = getOutputMediaFile(CROPPED_PATH_FOLDER, "$fileName")
            if (pictureFile == null) {
                Log.d("message", "Error creating media file, check storage permissions: ")
                return
            }
            val fos = FileOutputStream(pictureFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
            fos.close()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        } catch (e: FileNotFoundException) {
            Log.d("ImageCropperActivity", "File not found: " + e.message)
        } catch (e: IOException) {
            Log.d("ImageCropperActivity", "Error accessing file: " + e.message)
        }
    }

    private fun rotateImage(degrees: Float, bitmap: Bitmap): Bitmap{
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    companion object {
        const val IMAGE_EXTRA_NAME = "imageToCrop"

        fun launchCropper(context: Context, path: String){
            val intent = Intent(context, ImageCropperActivity::class.java)
            intent.putExtra(IMAGE_EXTRA_NAME, path)
            context.startActivity(intent)
        }
    }
}