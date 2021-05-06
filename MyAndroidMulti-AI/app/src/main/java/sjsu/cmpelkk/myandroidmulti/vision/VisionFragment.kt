package sjsu.cmpelkk.myandroidmulti.vision

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import sjsu.cmpelkk.myandroidmulti.R
import sjsu.cmpelkk.myandroidmulti.databinding.VisionFragmentBinding
import java.io.InputStream

class VisionFragment : Fragment() {
    //https://developer.android.com/topic/libraries/view-binding
    private var _binding: VisionFragmentBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    companion object {
        fun newInstance() = VisionFragment()
        const val CAMERA_PERMISSIONS_REQUEST: Int = 1
        const val ODT_REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_CAPTURE = 1
    }

    private lateinit var viewModel: VisionViewModel

    private lateinit var outputFileUri: Uri

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = VisionFragmentBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.imageButton.setOnClickListener {
            viewModel.openCamera()

            //Option1, get photo into image view
//            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//            try {
//                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
//            } catch (e: ActivityNotFoundException) {
//                // display error state to the user
//                Log.e("VisionFragment", "REQUEST_IMAGE_CAPTURE error ")
//            }

            //Option2, get photo into file uri
            val packageManager = activity!!.packageManager
            val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePhotoIntent.resolveActivity(packageManager) != null) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "Vision")
                outputFileUri = activity!!.contentResolver
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!

                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
                startActivityForResult(takePhotoIntent, ODT_REQUEST_IMAGE_CAPTURE)
            }
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) !=
            PackageManager.PERMISSION_GRANTED) {

            //binding.imageButton.isEnabled = false
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSIONS_REQUEST
            )
        }

//        if (ContextCompat.checkSelfPermission(
//                context!!,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
//            ) !=
//            PackageManager.PERMISSION_GRANTED) {
//
//            binding.imageButton.isEnabled = false
//            requestPermissions(
//                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
//                ODT_PERMISSIONS_REQUEST
//            )
//        }

        return view
        //return inflater.inflate(R.layout.vision_fragment, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(VisionViewModel::class.java)
        // TODO: Use the ViewModel
        viewModel.setupModel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            //Option1
//            val imageBitmap = data?.extras?.get("data")
//            //imageView.setImageBitmap(imageBitmap)
//            if (imageBitmap != null) {
//                val bitmap = imageBitmap as Bitmap
//                binding.photoimageView.setImageBitmap(bitmap)
//            }

            //Option2, get image from uri
            binding.photoimageView.setImageURI(outputFileUri)
            //binding.photoimageView.setImageBitmap(image)
            var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, outputFileUri)
            // Scale down bitmap size
            bitmap = viewModel.scaleBitmapDown(bitmap, 640)
            //val base64encoded = viewModel.base64(bitmap)
            //viewModel.firebasemldetect(base64encoded)

            //Firebase ML
            viewModel.runInterpreter(bitmap)

        }
    }

}