package com.oldeee.imagecomp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.oldee.imageviewer.ImageViewerActivity
import com.oldee.imageviewer.ImageViewerDialog
import com.oldeee.imagecomp.databinding.ActivityMainBinding
import com.oldeee.oldeeimageutil.OldeeImageUtil
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    //    lateinit var btn: Button
//    lateinit var tv: ImageView
    lateinit var binding: ActivityMainBinding

    var currentPhotoPath: String? = null
    var photoUri: Uri? = null

    var resizeUri: Uri? = null

    private val permissions =
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

    private val requestPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.all { per -> per.value == true }) {
                showFileSelector(this)
            }
        }

    private val pictureActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        pushImageData(it.resultCode, it.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.ivBefore.setOnClickListener {
            resizeUri?.let {

//                val uriIntent = Intent(this, ImageViewerActivity::class.java)
//                uriIntent.putExtra("uri", it.toString())
//
//                startActivity(uriIntent)
            }
        }

        binding.ivAfter.setOnClickListener {
            resizeUri?.let {
                val dialog = ImageViewerDialog(listOf(it.path?:"",it.path?:"")){imageView, s ->
                    imageView.setImageURI(Uri.parse(s))
                }
                dialog.show(supportFragmentManager, "")
//                val uriIntent = Intent(this, ImageViewerActivity::class.java)
//                uriIntent.putExtra("uri", it.toString())
//
//                startActivity(uriIntent)
            }
        }

        binding.btnCall.setOnClickListener {
            val check = checkPermission(this, *permissions)

            if (check) {
                showFileSelector(this)
            } else {
                requestPermissionResult.launch(permissions)
            }
        }
    }

    fun showFileSelector(context: Context) {
        val state = Environment.getExternalStorageState()
        if (!TextUtils.equals(state, Environment.MEDIA_MOUNTED))
            return

        //create camera intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        //add image capture event
        cameraIntent.resolveActivity(context.packageManager)?.let {
            createImageFile()?.let { f ->
                photoUri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    f
                )
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                cameraIntent.putExtra("mode", 0)
            }
        }

        //create galley intent
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = MediaStore.Images.Media.CONTENT_TYPE
            data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            putExtra(Intent.EXTRA_MIME_TYPES, "image/jpeg")
        }

        Intent.createChooser(intent, "사진 업로드 방법 선택").run {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            pictureActivityResultLauncher.launch(this)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    fun pushImageData(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val list = mutableListOf<Uri>()
            //use camera
            if (data == null) {
                val file = File(currentPhotoPath ?: "")
                val uri = Uri.fromFile(file)

                binding.ivBefore.setImageURI(uri)
//
                val needResize = OldeeImageUtil.needResize(this, uri)
//
                Log.e("#debug", "need Resize : ${needResize}")
                binding.tvInfoBefore.text =
                    String.format("w:%d, h:%d", needResize.width, needResize.height)
//
                if (needResize.need) {
                    val storageDir: File? = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val bitmap = OldeeImageUtil.optimizeBitmap(context = this, uri, storageDir!!)
                    bitmap?.let { path ->
                        val uri2 = Uri.fromFile(File(path))
                        binding.ivAfter.setImageURI(uri2)
                        resizeUri = uri2

                        val result = OldeeImageUtil.needResize(this, uri2)
                        binding.tvInfoAfter.text =
                            String.format("w:%d, h:%d", result.width, result.height)
                    }
                }
            } else {
                val selectedImage = data.data

                selectedImage?.let {
                    val mimeType = it.let { returnUri ->
                        this.contentResolver?.getType(it)
                    }

                    if (mimeType == "image/jpeg") {
                        val needResize = OldeeImageUtil.needResize(this, it)

                        Log.e("#debug", "need Resize : ${needResize}")

                        binding.ivBefore.setImageURI(it)
                        binding.tvInfoBefore.text =
                            String.format("w:%d, h:%d", needResize.width, needResize.height)

                        if (needResize.need) {
                            Log.e("#debug", "comp")
                            val storageDir: File? =
                                this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

                            val bitmap =
                                OldeeImageUtil.optimizeBitmap(context = this, it, storageDir!!)
                            bitmap?.let { path ->
                                val uri = Uri.fromFile(File(path))
                                binding.ivAfter.setImageURI(uri)

                                resizeUri = uri


                                val result = OldeeImageUtil.needResize(this, uri)
                                binding.tvInfoAfter.text =
                                    String.format("w:%d, h:%d", result.width, result.height)
                            }
                        }
                    } else {

                    }
                }
            }

            if (list.isEmpty()) {
                Log.e("#debug", "image list is null")
                return
            }
        }
    }

    fun checkPermission(context: Context, vararg permissions: String) = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}