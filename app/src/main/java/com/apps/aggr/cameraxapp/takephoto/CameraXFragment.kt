package com.apps.aggr.cameraxapp.takephoto

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaRecorder
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.*
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.apps.aggr.cameraxapp.MainActivity
import com.apps.aggr.cameraxapp.R
import com.apps.aggr.cameraxapp.listphotos.ListPhotosFragment
import com.bumptech.glide.Glide
import java.io.File

@SuppressLint("RestrictedApi")
class CameraXFragment : Fragment(), LifecycleOwner {

    private lateinit var viewFinder: TextureView
    private lateinit var recordButton: ImageButton
    private lateinit var stopRecordButton: ImageButton
    private lateinit var takePhoto: ImageButton
    private lateinit var timeVideo: Chronometer
    private lateinit var switcherModes: SwitchCompat
    private lateinit var modeLabel: TextView
    private lateinit var lastPicture: ImageView
    private lateinit var btnFiles: Button

    private var file: File? = null
    private var path: String? = null

    private val displayWidth = Resources.getSystem().displayMetrics.widthPixels
    private val displayHeight = Resources.getSystem().displayMetrics.heightPixels

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_camerax, container, false)

        //Request Permissions
        allPermissionsGranted()

        viewFinder = view.findViewById(R.id.view_finder)
        recordButton = view.findViewById(R.id.video_button)
        stopRecordButton = view.findViewById(R.id.video_stop_button)
        takePhoto = view.findViewById(R.id.capture_button)
        timeVideo = view.findViewById(R.id.time_video)
        switcherModes = view.findViewById(R.id.switch1)
        modeLabel = view.findViewById(R.id.tv_mode)
        btnFiles = view.findViewById(R.id.btnFiles)
        lastPicture = view.findViewById(R.id.lastPicture)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Hide Record Buttons
        stopRecordButton.visibility = View.GONE
        recordButton.visibility = View.GONE

        switcherModes.setOnCheckedChangeListener { _, isVideo ->
            if (isVideo) {
                CameraX.unbindAll()
                viewFinder.post { startVideo() }
                modeLabel.text = getString(R.string.video)

                timeVideo.visibility = View.VISIBLE
                recordButton.visibility = View.VISIBLE
                takePhoto.visibility = View.INVISIBLE

            }
            if (!isVideo) {
                CameraX.unbindAll()
                modeLabel.text = getString(R.string.foto)
                viewFinder.post { startCamera() }

                timeVideo.visibility = View.INVISIBLE
                recordButton.visibility = View.GONE
                takePhoto.visibility = View.VISIBLE
            }
        }
        // Ask for camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }

        } else {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }

        lastPicture.setOnClickListener {

            CameraX.unbindAll()

            activity?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.fragment, ListPhotosFragment.newInstance())
                ?.addToBackStack("")
                ?.commit()

            /*
            file?.let{ _file ->
                _file.delete()
                lastPicture.setImageBitmap(null)
                Toast.makeText(context, "Eliminado", Toast.LENGTH_SHORT).show()
            }

             */
        }
        btnFiles.setOnClickListener {
            MainActivity.changeFragment(requireActivity(), ListPhotosFragment())
        }

        //Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener{ _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

    }

    private fun startVideo() {

        val pauseOffset:Long = 0

        val screenSize = Size(displayWidth, displayHeight)
        val screenAspectRatio = Rational(displayWidth, displayHeight)

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

            var surface: SurfaceTexture? = viewFinder.surfaceTexture
            surface = it.surfaceTexture

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


        activity?.findViewById<ImageButton>(R.id.video_button)?.setOnClickListener {


            val file = File(activity?.externalMediaDirs?.first(),
                "${System.currentTimeMillis()}.mp4")

            videoCapture.startRecording(file,
                object : VideoCapture.OnVideoSavedListener{

                    override fun onVideoSaved(file: File?) {
                        val msg = "Video capture succeeded: ${file?.absolutePath}"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        Log.d("CameraXApp", msg)
                    }
                    override fun onError(
                        useCaseError: VideoCapture.UseCaseError?,
                        message: String?,
                        cause: Throwable?
                    ) {
                        val msg = "Video capture failed: $message"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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

        activity?.findViewById<ImageButton>(R.id.video_stop_button)?.setOnClickListener {
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
        CameraX.bindToLifecycle(viewLifecycleOwner, preview, videoCapture)

    }

    private fun startCamera() {

        val screenSize = Size(displayWidth, displayHeight)
        val screenAspectRatio = Rational(displayWidth, displayHeight)

        //Create configuration object for the viewFinder
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(screenAspectRatio)
            setTargetResolution(screenSize)
        }.build()

        //Build the viewfinder use case
        val preview = Preview(previewConfig)

        //Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {
            //To updated the SurfaceTextureView, we have to remove it and readd it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.setSurfaceTexture(it.surfaceTexture)

            updateTransform()
        }

        //create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setTargetAspectRatio(screenAspectRatio)
                setTargetResolution(screenSize)
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            }.build()

        //Build teh image capture use case add attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)

        requireActivity().findViewById<ImageButton>(R.id.capture_button).setOnClickListener {

            val file = File(requireActivity().externalMediaDirs.first(),
                "${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(file, object : ImageCapture.OnImageSavedListener{
                override fun onImageSaved(file: File) {
                    val msg = "Photo capture succeeded: ${file.absolutePath}"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    Glide.with(lastPicture).load(file).into(lastPicture)
                }

                override fun onError(
                    useCaseError: ImageCapture.UseCaseError,
                    message: String,
                    cause: Throwable?
                ) {
                    Toast.makeText(requireContext(),
                        message,
                        Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }
        //Bind use case to lifeCycle
        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        //Calculate center of the view finder
        val centerX = viewFinder.width /2f
        val centerY = viewFinder.height /2f

        //Correct preview output to account for display
        val rotationDegrees = when(viewFinder.display.rotation){
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }

        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        //Apply transformations
        viewFinder.setTransform(matrix)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                activity?.finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        activity?.let { it1 ->
            ContextCompat.checkSelfPermission(
                it1, it)
        } == PackageManager.PERMISSION_GRANTED
    }

    override fun onStop() {
        super.onStop()
        CameraX.unbindAll()
    }

    companion object {

        @JvmStatic
        fun newInstance() = CameraXFragment()

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        //Audio Constants
        /** audio bit rate */
        private const val DEFAULT_AUDIO_BIT_RATE:Int = 64000
        /** audio sample rate */
        private const val DEFAULT_AUDIO_SAMPLE_RATE:Int = 8000
        /** audio channel count */
        private const val DEFAULT_AUDIO_CHANNEL_COUNT:Int = 1
        /** audio record source */
        private const val DEFAULT_AUDIO_RECORD_SOURCE:Int = MediaRecorder.AudioSource.CAMCORDER
        /** audio default minimum buffer size */
        private const val DEFAULT_AUDIO_MIN_BUFFER_SIZE:Int = 1024
    }
}