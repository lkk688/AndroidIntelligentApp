package sjsu.cmpelkk.myandroidmulti.vision

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import sjsu.cmpelkk.myandroidmulti.R
import sjsu.cmpelkk.myandroidmulti.databinding.ActivityCameraBinding
import sjsu.cmpelkk.myandroidmulti.databinding.VisionFragmentBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val REQUEST_CODE_PERMISSIONS = 10

private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit


class CameraActivity : AppCompatActivity() {
    //https://developer.android.com/topic/libraries/view-binding
    private lateinit var binding: ActivityCameraBinding

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imagePreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis

    //private lateinit var imageCapture: ImageCapture
    private var imageCapture: ImageCapture? = null

    private lateinit var videoCapture: VideoCapture

    private lateinit var previewView: PreviewView

    //private val executor = Executors.newSingleThreadExecutor()
    private lateinit var executor: ExecutorService

    private lateinit var outputDirectory: File

    private lateinit var cameraInfo: CameraInfo
    private lateinit var cameraControl: CameraControl

    private val objectDetectorConfig = TFLiteObjectDetectionAnalyzer.Config(
        minimumConfidence = 0.5f,
        numDetection = 10,
        inputSize = 300,
        isQuantized = true,
        modelFile = "detect.tflite",
        labelsFile = "labelmap.txt"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_camera)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        previewView = binding.previewView

        if (allPermissionsGranted()) {
            previewView.post { startCamera() }
            previewView.addOnLayoutChangeListener{ _, _, _, _, _, _, _, _, _ ->
                updateTransform()
            }

        } else {
            requestPermissions(
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        outputDirectory = getOutputDirectory(this)

        binding.cameraCaptureButton.setOnClickListener {
            //takePicture()
            takePhoto()
        }

        executor = Executors.newSingleThreadExecutor()
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                previewView.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateTransform() {
        // TODO: Implement camera viewfinder transformations
    }

    private fun startCamera() {

        bindCameraUseCases()


    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { previewView.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = previewView.display.rotation

        //A CameraSelector instance will be created and passed to bindToLifecycle function.
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        //Create an instance of the ProcessCameraProvider. This is used to bind the lifecycle of cameras to the lifecycle owner.
        // This eliminates the task of opening and closing the camera since CameraX is lifecycle-aware
        // Bind the CameraProvider to the LifeCycleOwner
        //need an instance of ProcessCameraProvider
        cameraProviderFuture = ProcessCameraProvider.getInstance(this) //will be obtained asynchronously using the static method

        //Add a listener to the cameraProviderFuture. Add a Runnable as one argument.
        //Add ContextCompat.getMainExecutor() as the second argument. This returns an Executor that runs on the main thread.
        cameraProviderFuture.addListener(Runnable {

            //In the Runnable, add a ProcessCameraProvider. This is used to bind the lifecycle of your camera to the LifecycleOwner within the application's process
            // Used to bind the lifecycle of cameras to the lifecycle owner
            // CameraProvider
            //val cameraProvider = cameraProviderFuture.get()
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            //Preview
            //Initialize your Preview object, call build on it, get a surface provider from viewfinder, and then set it on the preview.
            val viewFinder: PreviewView = binding.previewView//findViewById(R.id.previewView)
            imagePreview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.createSurfaceProvider())
                }

            imageCapture = ImageCapture.Builder()
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)//Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()
//                .also {
//                    it.setAnalyzer(executor, LuminosityAnalyzer { luma ->
//                        // Values returned from our analyzer are passed to the attached listener
//                        // We log image analysis results here - you should do something useful
//                        // instead!
//                        binding.resultslabel.text = luma.toString()
//                        Log.i(TAG, "Average luminosity: $luma")
//                    })
//                }

            //Object detection analyzer
            imageAnalysis.setAnalyzer(executor, TFLiteObjectDetectionAnalyzer(this,objectDetectorConfig,::onDetectionResult))

            //Using TFLite model for image classification
//            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
//                val rotationDegrees = image.imageInfo.rotationDegrees
//                // insert your code here.
//                val bitmap = image.toBitmap()
//                tfLiteClassifier.classifyAsync(bitmap)
//                    .addOnSuccessListener {
//                        binding.resultslabel.text = it
//                        image.close()
//                    }
//                    .addOnFailureListener { e ->
//                        Log.e(TAG, "Error in the classifier.", e)
//                    }
//            })
            //CameraX produces images in YUV_420_888 format.

            //A CameraSelector instance will be created and passed to bindToLifecycle function.
//            val cameraSelector = CameraSelector.Builder()
//                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//                .build()
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                var camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalysis, imageCapture, imagePreview)

                cameraInfo = camera.cameraInfo
                cameraControl = camera.cameraControl

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            //var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, imageCapture, imagePreview)
            //imagePreview.setSurfaceProvider(previewView.createSurfaceProvider(camera.cameraInfo))


        }, ContextCompat.getMainExecutor(this))
    }

    private fun onDetectionResult(result: TFLiteObjectDetectionAnalyzer.Result) {
        //binding.result_overlay.
        //binding.resultOverlay.updateResults(result)
        //result_overlay.updateResults(result)

        //detectionresult_overlay.updateResults(result)
        binding.detectionresultOverlay.updateResults(result)
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }
    //ref: https://developer.android.com/training/camerax/take-photo
//    private fun takePicture() {
//        val file = createFile(
//            outputDirectory,
//            FILENAME,
//            PHOTO_EXTENSION
//        )
//        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
//        imageCapture.takePicture(outputFileOptions, executor, object : ImageCapture.OnImageSavedCallback {
//            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                val msg = "Photo capture succeeded: ${file.absolutePath}"
//                Log.d(TAG, msg)
//                previewView.post {
//                    //Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
//                    Snackbar.make(previewView, msg, Snackbar.LENGTH_LONG)
//                        .setAction("Action", null)
//                        .show()
//                }
//            }
//
//            override fun onError(exception: ImageCaptureException) {
//                val msg = "Photo capture failed: ${exception.message}"
//                previewView.post {
//                    //Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
//                    Snackbar.make(previewView, msg, Snackbar.LENGTH_LONG)
//                        .setAction("Action", null)
//                        .show()
//                }
//            }
//        })
//    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        private const val PHOTO = 0
        private const val VIDEO = 1

        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }

        fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )
    }
}