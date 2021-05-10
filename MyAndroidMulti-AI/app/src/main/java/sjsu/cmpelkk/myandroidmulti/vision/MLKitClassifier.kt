package sjsu.cmpelkk.myandroidmulti.vision

import android.graphics.Bitmap
import android.graphics.Color
import android.renderscript.ScriptGroup
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MLKitClassifier {
    companion object {
        private const val TAG = "MLKitClassifier"
    }

    private var localModelfile = "mobilenet_v2_1.0_224_quant.tflite"//""mobilenet_v2_1.0_224.tflite"
    private lateinit var imageLabeler: ImageLabeler

    fun setupLocalModel(modelfile: File) {
        //ref: https://developers.google.com/ml-kit/vision/image-labeling/custom-models/android

        //Use the model in the asset
//        val localModel = LocalModel.Builder()
//            .setAssetFilePath(localModelfile)
//            // or .setAbsoluteFilePath(absolute file path to model file)
//            // or .setUri(URI to model file)
//            .build()
        val localModel = LocalModel.Builder()
            //.setAssetFilePath(localModelfile)
            .setAbsoluteFilePath(modelfile.absolutePath)
            // or .setUri(URI to model file)
            .build()
        val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.5f)
            .setMaxResultCount(5)
            .build()
        //create an ImageLabeler object
        imageLabeler = ImageLabeling.getClient(customImageLabelerOptions)
    }

    //for each image you want to label, create an InputImage object from your image. The image labeler runs fastest when you use a Bitmap
    fun detectInImage(image: InputImage): Task<List<ImageLabel>> {
        return imageLabeler.process(image)

//        labeler.process(image)
//            .addOnSuccessListener { labels ->
//                // Task completed successfully
//                // ...
//            }
//            .addOnFailureListener { e ->
//                // Task failed with an exception
//                // ...
//            }
    }

    //ref: https://github.com/googlesamples/mlkit/blob/master/android/android-snippets/app/src/main/java/com/google/example/mlkit/kotlin/MLKitVisionImage.kt
    fun imageFromBitmap(bitmap: Bitmap): InputImage {
        val rotationDegrees = 0
        // [START image_from_bitmap]
        val image = InputImage.fromBitmap(bitmap, 0)
        // [END image_from_bitmap]
        return image
    }

    fun imageFromBuffer(byteBuffer: ByteBuffer, rotationDegrees: Int): InputImage {
        // [START set_metadata]
        // TODO How do we document the FrameMetadata developers need to implement?
        // [END set_metadata]
        // [START image_from_buffer]
        val image = InputImage.fromByteBuffer(
            byteBuffer,
            /* image width */ 224, //480,
            /* image height */ 224, //360,
            rotationDegrees,
            InputImage.IMAGE_FORMAT_NV21 // or IMAGE_FORMAT_YV12
        )
        return image
        // [END image_from_buffer]
    }

    fun convertBitmapToByterBuffer(bitmap: Bitmap): ByteBuffer {
        val inputImageWidth = 224
        val inputImageHeight =224
        val modelInputSize = 224*224*3*4
        val bitmap = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
        //val input = ByteBuffer.allocateDirect(224*224*3*4).order(ByteOrder.nativeOrder())
        val input = ByteBuffer.allocateDirect(modelInputSize).order(ByteOrder.nativeOrder())
        for (y in 0 until inputImageHeight) {
            for (x in 0 until inputImageWidth) {
                val px = bitmap.getPixel(x, y) //x is the width, y is the height

                // Get channel values from the pixel value.
                val r = Color.red(px)
                val g = Color.green(px)
                val b = Color.blue(px)

                // Normalize channel values to [-1.0, 1.0]. This requirement depends on the model.
                // For example, some models might require values to be normalized to the range
                // [0.0, 1.0] instead.
                val rf = (r - 127) / 255f
                val gf = (g - 127) / 255f
                val bf = (b - 127) / 255f

                input.putFloat(rf)
                input.putFloat(gf)
                input.putFloat(bf)
            }
        }
        return input
    }

    fun stop() {
        try {
            imageLabeler.close()
        } catch (e: IOException) {
            Log.e(
                TAG,
                "Exception thrown while trying to close ImageLabelerClient: $e"
            )
        }
    }

}