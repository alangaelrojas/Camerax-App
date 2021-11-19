package com.apps.aggr.cameraxapp.cropphoto

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStorageDirectory
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import com.apps.aggr.cameraxapp.R
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
            cropLayout.setBitmap(bitmap)
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
        val mBitmap: Bitmap? = bitmap
        mBitmap?.let {
            progressBar.visibility = View.GONE
            val view = layoutInflater.inflate(R.layout.dialog_result, null)
            view.findViewById<ImageView>(R.id.image).setImageBitmap(it)
            saveImageLocally(it)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_result)
                .setView(view)
                .setPositiveButton(R.string.dialog_button_close) { dialog, _ -> dialog.dismiss() }
                .show()
        }?: Snackbar.make(parent, R.string.error_failed_to_clip_image, Snackbar.LENGTH_LONG).show()

    }

    override fun onFailure(e: Exception) {
        Snackbar.make(parent, R.string.error_failed_to_clip_image, Snackbar.LENGTH_LONG).show()
    }

    private fun saveImageLocally(bitmap: Bitmap){
        val pictureFile: File? = getOutputMediaFile()
        if (pictureFile == null) {
            Log.d("message", "Error creating media file, check storage permissions: ")
            return
        }
        try {
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

    private fun getOutputMediaFile(): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        val mediaStorageDir = File(
            getExternalStorageDirectory()
                .toString() + "/Android/data/"
                    + applicationContext.packageName
        )

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }

        return File(mediaStorageDir.path + File.separator + fileName)
    }

    companion object {
        const val IMAGE_EXTRA_NAME = "imageToCrop"
    }
}