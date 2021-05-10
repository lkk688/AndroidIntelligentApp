package sjsu.cmpelkk.myandroidmulti.vision

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import java.io.ByteArrayOutputStream
import java.io.InputStream

//import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.*
import com.google.mlkit.common.model.CustomRemoteModel
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.linkfirebase.FirebaseModelSource
import org.tensorflow.lite.Interpreter
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.collections.ArrayList

//AndroidViewModel is a subclass of ViewModel that is aware of Application context
//class VisionViewModel : ViewModel()
//class NetworkViewModel(private val app: Application): AndroidViewModel(app) {
class VisionViewModel(private val app: Application) : AndroidViewModel(app) {
    // TODO: Implement the ViewModel
    private lateinit var functions: FirebaseFunctions
    var resultmessage = MutableLiveData<String>()

    private var myClassifier = TFLiteClassifier()//Classifier(this)
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private var model_name = "classifierstart"//""mobilenetv2" //classifierstart

    private val firebasePerformance = FirebasePerformance.getInstance()

    var labels = ArrayList<String>()

    //MLKit
    private var mlKitClassifier = MLKitClassifier()
    var confidencethreshold = 0.4

    companion object {
        private const val TAG = "VisionViewModel"

        private const val label_filename = "labels.txt" //4

        private const val useMLkit = true
    }

    fun openCamera() {
        Log.i("VisionViewModel", "clicked image button ")
    }

    @Throws(IOException::class)
    fun loadLines(): ArrayList<String> {
        val s = Scanner(InputStreamReader(app.applicationContext.assets.open(label_filename)))
        val labels = ArrayList<String>()
        while (s.hasNextLine()) {
            labels.add(s.nextLine())
        }
        s.close()
        return labels
    }

    /**
     * getCapturedImage():
     *     Decodes and center crops the captured image from camera.
     */
//    fun getCapturedImage(outputFileUri: Uri, width: Int, height: Int): Bitmap {
//
////        val srcImage = FirebaseVisionImage
////            .fromFilePath(activity!!, outputFileUri).bitmap
//        val iois: InputStream = getContentResolver().openInputStream(outputFileUri)
//        val srcImage = BitmapFactory.decodeStream(iois)
//        iois.close()
//
//        //val srcImage = BitmapFactory.decodeFile(outputFileUri.path)
//
//        // crop image to match imageView's aspect ratio
//        val scaleFactor = Math.min(
//            srcImage.width / width.toFloat(),
//            srcImage.height / height.toFloat()
//        )
//
//        val deltaWidth = (srcImage.width - width * scaleFactor).toInt()
//        val deltaHeight = (srcImage.height - height * scaleFactor).toInt()
//
//        val scaledImage = Bitmap.createBitmap(
//            srcImage, deltaWidth / 2, deltaHeight / 2,
//            srcImage.width - deltaWidth, srcImage.height - deltaHeight
//        )
//        srcImage.recycle()
//        return scaledImage
//
//    }

    fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth =
                (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight =
                (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }

    fun base64(bitmap: Bitmap): String {
        // Convert bitmap to base64 encoded string
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
        val base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        return base64encoded
    }

    //Use Firebase function to call Cloud Vision API
    fun firebasemldetect(base64encoded: String) {
        // ...
        // Create json request to cloud vision
        val request = JsonObject()
        // Add image to request
        val image = JsonObject()
        image.add("content", JsonPrimitive(base64encoded))
        request.add("image", image)
        //Add features to the request
        val feature = JsonObject()
        feature.add("maxResults", JsonPrimitive(5))
        feature.add("type", JsonPrimitive("LABEL_DETECTION"))
        val features = JsonArray()
        features.add(feature)
        request.add("features", features)

        annotateImage(request.toString())
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // Task failed with an exception
                    //
                } else {
                    // Task completed successfully
                    for (label in task.result!!.asJsonArray[0].asJsonObject["labelAnnotations"].asJsonArray) {
                        val labelObj = label.asJsonObject
                        val text = labelObj["description"]
                        val entityId = labelObj["mid"]
                        val confidence = labelObj["score"]
                        val message = "Text: ${text} entityId: ${entityId} confidence: ${confidence}"
                        resultmessage.value += message //send to live data
                        Log.i("VisionViewModel", message)
                    }
                }
            }

    }

    //Using Cloud Vision API
    private fun annotateImage(requestJson: String): Task<JsonElement> {
        functions = Firebase.functions
        return functions
            .getHttpsCallable("annotateImage")
            .call(requestJson)
            .continueWith { task ->
                // This continuation runs on either success or failure, but if the task
                // has failed then result will throw an Exception which will be
                // propagated down.
                val result = task.result?.data
                JsonParser.parseString(Gson().toJson(result))
            }
    }

    //Custom model
    fun cleanup() {
        myClassifier.close()
    }

    //custom model
    fun runInterpreter(inputbitmap: Bitmap) {
        if (useMLkit==true) {
            mlkitprocessimage(inputbitmap)
        }else {
            firebasemlprocessimage(inputbitmap)
        }

    }

    private fun configureRemoteConfig() {
        remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 6 //3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    fun setupModelviaRemoteConfig(){
        configureRemoteConfig()
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    model_name = remoteConfig.getString("model_name")
                    setupModel()
                } else {
                    Log.i(TAG, "Failed to fetch model name:")
                }
            }

    }

    fun setupModel() {
        //load labels
        labels = loadLines()
        myClassifier.labels = labels

        //Firebase performance trace
        val downloadTrace = firebasePerformance.newTrace("download_model")
        downloadTrace.start()

        val conditions = CustomModelDownloadConditions.Builder()
            .requireWifi()
            .build()
        FirebaseModelDownloader.getInstance()
            .getModel(model_name, DownloadType.LOCAL_MODEL, conditions)//"classifierstart"
            .addOnCompleteListener {
                // Download complete. Depending on your app, you could enable the ML
                // feature, or switch from the local model to the remote model, etc.
                downloadTrace.stop()

                val model = it.result
                if (model == null) {
                    Log.i("VisionViewModel", "Failed to get model file.")
                } else {
                    Log.i("VisionViewModel", "Downloaded remote model: $model")
                    val modelFile = model?.file
                    if (modelFile !=null) {
                        if (useMLkit==true) {
                            mlKitClassifier.setupLocalModel(modelFile)
                        }else {
                            myClassifier.initialize(modelFile)
                        }

                    }
                }
            }
            .addOnFailureListener {
                Log.i("VisionViewModel", "Model download failed:")
            }

    }

    fun firebasemlprocessimage(bitmap: Bitmap) {
        val classifyTrace = firebasePerformance.newTrace("classify")
        classifyTrace.start()

        //Classify Async operation
        myClassifier.classifyAsync(bitmap)
            .addOnSuccessListener {
                classifyTrace.stop()
                Log.i("VisionViewModel", it)
                resultmessage.value = it
            }
            .addOnFailureListener {
                resultmessage.value = "Error:"+it.localizedMessage
            }

        //Another option
//        val result = myClassifier.classify(inputbitmap)
//        Log.i("VisionViewModel", result)
//        resultmessage.value = result
    }

    //MLkit
//    fun setupMLKitModel() {
//
//        //mlKitClassifier.setupLocalModel()
//
//        // Specify the name you assigned in the Firebase console.
//        val remoteModel =
//            CustomRemoteModel
//                .Builder(FirebaseModelSource.Builder("mobilenetv2").build())
//                .build()
//
//        val downloadConditions = DownloadConditions.Builder()
//            .requireWifi()
//            .build()
//        RemoteModelManager.getInstance().download(remoteModel, downloadConditions)
//            .addOnSuccessListener {
//                // Success.
//                val model = it
//                if (model == null) {
//                    Log.i("VisionViewModel", "Failed to get model file.")
//                } else {
//                    Log.i("VisionViewModel", "Model downloaded:")
//                }
//            }
//            .addOnFailureListener {
//                Log.i("VisionViewModel", "Model download failed:")
//            }
//    }

    fun mlkitprocessimage(bitmap: Bitmap) {
        val inputimage = mlKitClassifier.imageFromBitmap(bitmap)
        //val bytebuffer = mlKitClassifier.convertBitmapToByterBuffer(bitmap)
        //val inputimage = mlKitClassifier.imageFromBuffer(bytebuffer, 0)
        mlKitClassifier.detectInImage(inputimage)
            .addOnSuccessListener {
                if (it == null) {
                    Log.i(TAG, "No labels detected")
                } else {
                    for (label in it) {
                        val text = label.text
                        val confidence = label.confidence
                        val index = label.index
                        val classname = labels[index]
                        val str = String.format("Label %s, classname %s confidence %f \n", text, classname, confidence)
                        if (confidence>confidencethreshold) {
                            resultmessage.value += str
                        }
                        Log.i(
                            TAG,
                            String.format("Label %s, confidence %f", text, confidence)
                        )
                    }
                }
            }
            .addOnFailureListener {
                Log.i("VisionViewModel", "mlkitprocessimage failed:")
                resultmessage.value = "Error:"+it.localizedMessage
            }
    }
}