package sjsu.cmpelkk.myandroidmulti.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MyClassifier{ //(private val context: Context) {
    companion object {
        private const val TAG = "ImageClassifier"

        private const val FLOAT_TYPE_SIZE = 4 //4
        private const val PIXEL_SIZE = 1

        private const val OUTPUT_CLASSES_COUNT = 1001
        private const val MODEL_NAME_KEY = "flower"//""classifierstart"
        private const val QUANTIZED_model = true
    }

    private var interpreter: Interpreter? = null
    var isInitialized = false
        private set

    /** Executor to run inference task in the background */
    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    private var inputImageWidth: Int = 0 // will be inferred from TF Lite model
    private var inputImageHeight: Int = 0 // will be inferred from TF Lite model
    private var modelInputSize: Int = 0 // will be inferred from TF Lite model

    fun initialize(model: File): Task<Void> {
        return Tasks.call(
            executorService,
            Callable<Void> {
                initializeInterpreter(model)
                null
            }
        )
    }

    private fun initializeInterpreter(model: File) {
        // Initialize TF Lite Interpreter with NNAPI enabled
        val options = Interpreter.Options()
        options.setUseNNAPI(true)
        var interpreter: Interpreter
        interpreter = Interpreter(model, options)
//        if (model is ByteBuffer) {
//            interpreter = Interpreter(model, options)
//        } else {
//            interpreter = Interpreter(model as File, options)
//        }
        // Read input shape from model file
        val inputShape = interpreter.getInputTensor(0).shape()
        inputImageWidth = inputShape[1]
        inputImageHeight = inputShape[2]
        modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * PIXEL_SIZE * 3

        // Finish interpreter initialization
        this.interpreter = interpreter
        isInitialized = true
        Log.d(TAG, "Initialized TFLite interpreter.")
    }

    fun classify(bitmap: Bitmap): String {
        if (!isInitialized) {
            throw IllegalStateException("TF Lite Interpreter is not initialized yet.")
        }

        var startTime: Long
        var elapsedTime: Long

        // Preprocessing: resize the input
        startTime = System.nanoTime()
        val resizedImage = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
        val byteBuffer = convertBitmapToByterBuffer2(resizedImage)
        elapsedTime = (System.nanoTime() - startTime) / 1000000
        Log.d(TAG, "Preprocessing time = " + elapsedTime + "ms")

        startTime = System.nanoTime()
        val result = Array(1) { FloatArray(OUTPUT_CLASSES_COUNT) }

//        val bufferSize = OUTPUT_CLASSES_COUNT * java.lang.Float.SIZE / java.lang.Byte.SIZE //1000*
//        val result = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
        interpreter?.run(byteBuffer, result)

        //interpreter?.run(byteBuffer, result)
        elapsedTime = (System.nanoTime() - startTime) / 1000000
        Log.d(TAG, "Inference time = " + elapsedTime + "ms")

        var index = getMaxResult(result[0])
        var stringresult = "Prediction is $index \nInference Time $elapsedTime ms"
        return stringresult//getOutputString(result[0])
    }

    fun classifyAsync(bitmap: Bitmap): Task<String> {
        return Tasks.call(executorService, Callable<String> { classify(bitmap) })
    }

    fun close() {
        Tasks.call(
            executorService,
            Callable<String> {
                interpreter?.close()
                Log.d(TAG, "Closed TFLite interpreter.")
                null
            }
        )
    }


    private fun convertBitmapToByterBuffer2(bitmap: Bitmap): ByteBuffer {
        val bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        //val input = ByteBuffer.allocateDirect(224*224*3*4).order(ByteOrder.nativeOrder())
        val input = ByteBuffer.allocateDirect(modelInputSize).order(ByteOrder.nativeOrder())
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val px = bitmap.getPixel(x, y)

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
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in pixels) {
            val r = (pixelValue shr 16 and 0xFF)
            val g = (pixelValue shr 8 and 0xFF)
            val b = (pixelValue and 0xFF)

            // Convert RGB to grayscale and normalize pixel value to [0..1]
            val normalizedPixelValue = (r + g + b) / 3.0f / 255.0f
            byteBuffer.putFloat(normalizedPixelValue)
        }

        return byteBuffer
    }

    private fun getMaxResult(result: FloatArray): Int {
        var probability = result[0]
        var index = 0
        for (i in result.indices) {
            if (probability < result[i]) {
                probability = result[i]
                index = i
            }
        }
        return index
    }

    private fun getOutputString(output: FloatArray): String {
        val maxIndex = output.indices.maxBy { output[it] } ?: -1
        return "Prediction Result: %d\nConfidence: %2f".format(maxIndex, output[maxIndex])
    }
}