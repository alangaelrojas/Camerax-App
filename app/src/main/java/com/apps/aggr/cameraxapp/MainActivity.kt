package com.apps.aggr.cameraxapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.*
import android.widget.*
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

class MainActivity : AppCompatActivity(), LifecycleOwner {

    private lateinit var viewFinder: TextureView
    private lateinit var recordButton:ImageButton
    private lateinit var stopRecordButton:ImageButton
    private lateinit var takePhoto:ImageButton
    private lateinit var timeVideo:Chronometer
    private lateinit var switcherModes:Switch
    private lateinit var modeLabel:TextView

    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO);


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //make the activity fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Set view
        setContentView(R.layout.activity_main)

        // Add this at the end of onCreate function

        viewFinder = findViewById(R.id.view_finder)
        recordButton = findViewById(R.id.video_button)
        stopRecordButton = findViewById(R.id.video_stop_button)
        takePhoto = findViewById(R.id.capture_button)
        timeVideo = findViewById(R.id.tv_timeVideo)
        switcherModes = findViewById(R.id.switch1)
        modeLabel = findViewById(R.id.tv_mode)

        //Request Permissions
        allPermissionsGranted()

        //Hide Record Buttons
        stopRecordButton.visibility = View.GONE
        recordButton.visibility = View.GONE

        switcherModes.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(p0: CompoundButton?, isVideo: Boolean) {
                if(isVideo){
                    CameraX.unbindAll()
                    viewFinder.post {startVideo()}
                    modeLabel.text = "VIDEO"

                    timeVideo.visibility = View.VISIBLE
                    recordButton.visibility = View.VISIBLE
                    takePhoto.visibility = View.INVISIBLE

                }
                if(!isVideo){
                    CameraX.unbindAll()
                    modeLabel.text = "FOTO"
                    viewFinder.post {startCamera()}

                    timeVideo.visibility = View.INVISIBLE
                    recordButton.visibility = View.GONE
                    takePhoto.visibility = View.VISIBLE
                }
            }
        })
        // Ask for camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }

        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun startVideo() {

        val pauseOffset:Long = 0

        /** audio bit rate */
        val DEFAULT_AUDIO_BIT_RATE:Int = 64000
        /** audio sample rate */
        val DEFAULT_AUDIO_SAMPLE_RATE:Int = 8000
        /** audio channel count */
        val DEFAULT_AUDIO_CHANNEL_COUNT:Int = 1
        /** audio record source */
        val DEFAULT_AUDIO_RECORD_SOURCE:Int = MediaRecorder.AudioSource.CAMCORDER
        /** audio default minimum buffer size */
        val DEFAULT_AUDIO_MIN_BUFFER_SIZE:Int = 1024

        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(screenAspectRatio)
            setTargetResolution(screenSize)
            
        }.build()

        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }


        val videoCaptureConfig = VideoCaptureConfig.Builder()
            .apply {
                setLensFacing(CameraX.LensFacing.BACK)
                setTargetRotation(viewFinder.display.rotation)
                setTargetResolution(screenSize)
                setMaxResolution(screenSize)
                setAudioBitRate(DEFAULT_AUDIO_BIT_RATE)
                setAudioSampleRate(DEFAULT_AUDIO_SAMPLE_RATE)
                setAudioChannelCount(DEFAULT_AUDIO_CHANNEL_COUNT)
                setAudioRecordSource(DEFAULT_AUDIO_RECORD_SOURCE)
                setAudioMinBufferSize(DEFAULT_AUDIO_MIN_BUFFER_SIZE)
            }.build()


        val videoCapture = VideoCapture(videoCaptureConfig)


        findViewById<ImageButton>(R.id.video_button).setOnClickListener {


            val file = File(externalMediaDirs.first(),
                "${System.currentTimeMillis()}.mp4")

            videoCapture.startRecording(file,
                object : VideoCapture.OnVideoSavedListener{

                    override fun onVideoSaved(file: File?) {
                        val msg = "Video capture succeeded: ${file?.absolutePath}"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d("CameraXApp", msg)
                    }
                    override fun onError(
                        useCaseError: VideoCapture.UseCaseError?,
                        message: String?,
                        cause: Throwable?
                    ) {
                        val msg = "Video capture failed: $message"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.e("CameraXApp", msg)
                    }
                })
            recordButton.visibility = View.GONE
            modeLabel.visibility = View.GONE
            switcherModes.visibility = View.GONE
            stopRecordButton.visibility = View.VISIBLE
            timeVideo.visibility = View.VISIBLE
            timeVideo.base = SystemClock.elapsedRealtime() - pauseOffset
            timeVideo.start()

        }

        findViewById<ImageButton>(R.id.video_stop_button).setOnClickListener {
            videoCapture.stopRecording()
            recordButton.visibility = View.VISIBLE
            modeLabel.visibility = View.VISIBLE
            switcherModes.visibility = View.VISIBLE
            stopRecordButton.visibility = View.GONE
            timeVideo.visibility = View.GONE
            timeVideo.base = SystemClock.elapsedRealtime()
            timeVideo.stop()
        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview, videoCapture)

    }

    private fun startCamera() {

        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(screenAspectRatio)
            setTargetResolution(screenSize)
        }.build()

        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setTargetAspectRatio(screenAspectRatio)
                // We don't set a resolution for image capture; instead, we
                // select a capture mode which will infer the appropriate
                // resolution based on aspect ration and requested mode
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            }.build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)
        findViewById<ImageButton>(R.id.capture_button).setOnClickListener {

            val file = File(externalMediaDirs.first(),
                "${System.currentTimeMillis()}.jpg")

            imageCapture.takePicture(file,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(error: ImageCapture.UseCaseError,
                                         message: String, exc: Throwable?) {
                        val msg = "Photo capture failed: $message"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.e("CameraXApp", msg)
                        exc?.printStackTrace()
                    }

                    override fun onImageSaved(file: File) {
                        val msg = "Photo capture succeeded: ${file?.absolutePath}"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d("CameraXApp", msg)

                    }
                })
        }


        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}
