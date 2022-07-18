package com.oldeee.oldeeimageutil

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object OldeeImageUtil {
    const val MAX = 1048

    private fun createImageFile(storageDir:File): File {
        val timeStamp: String = UUID.randomUUID().toString()
        return File.createTempFile(
            "COMP_JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    fun optimizeBitmap(context: Context, uri: Uri, storageDir: File): String? {
        try {
            val tempFile = createImageFile(storageDir)

            val fos = FileOutputStream(tempFile)

            val bitmap = resizeBitmapFormUri(uri, context)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            bitmap?.recycle()

            fos.flush()
            fos.close()

            return tempFile.absolutePath // 임시파일 저장경로 리턴
        } catch (e: Exception) {
            Log.e(TAG, "FileUtil - ${e.message}")
        }

        return null
    }

    fun optimizeBitmap(context: Context, path: String, storageDir:File): File? {
        try {
            val tempFile = createImageFile(storageDir)

            val fos = FileOutputStream(tempFile)

            val bitmap = resizeBitmapFormUri(path, context)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            bitmap?.recycle()

            fos.flush()
            fos.close()

            return tempFile // 임시파일 저장경로 리턴
        } catch (e: Exception) {
            Log.e(TAG, "FileUtil - ${e.message}")
        }

        return null
    }

    private fun resizeBitmapFormUri(uri: Uri, context: Context): Bitmap? {
        val input = context.contentResolver.openInputStream(uri)

        var bitmap: Bitmap?
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        bitmap = BitmapFactory.decodeStream(input, null, options)
        options.inSampleSize = calculateInSampleSize(options)

        bitmap = bitmap?.let {
            rotateImageIfRequired(context, bitmap!!, uri)
        }

        input?.close()

        return bitmap
    }

    private fun resizeBitmapFormUri(path: String, context: Context): Bitmap? {
//        val input = context.contentResolver.openInputStream(uri)

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        var bitmap: Bitmap?
        bitmap = BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options)


        bitmap = bitmap?.let {
            rotateImageIfRequired(context, bitmap!!, path)
        }

        return bitmap
    }

    /**
     * 리사이즈가 필요한지 판단
     */
    fun needResize(context: Context, uri: Uri): Boolean {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val input = context.contentResolver!!.openInputStream(uri)

        var bitmap: Bitmap?
        bitmap = BitmapFactory.decodeStream(input, null, options)
        options.inJustDecodeBounds = false
//        BitmapFactory.Options().run {
//            bitmap = BitmapFactory.decodeStream(input, null, this)
//        }

//        input?.reset()

        Log.e("#debug", "image w:${bitmap?.width} h:${bitmap?.height}")
        if (bitmap == null)
            return false
        else {
            return bitmap?.width!! > MAX || bitmap?.height!! > MAX
        }
    }

    // 리샘플링 값 계산
    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        var height = options.outHeight
        var width = options.outWidth

        var inSampleSize = 1

        while (height > MAX || width > MAX) {
            height /= 2
            width /= 2
            inSampleSize *= 2
        }

        return inSampleSize
    }

    private fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap? {
        val input = context.contentResolver.openInputStream(uri) ?: return null

        val exif = if (Build.VERSION.SDK_INT > 23) {
            ExifInterface(input)
        } else {
            ExifInterface(uri.path!!)
        }

        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270)
            else -> bitmap
        }
    }

    private fun rotateImageIfRequired(context: Context, bitmap: Bitmap, path: String): Bitmap? {
//        val input = context.contentResolver.openInputStream(uri) ?: return null
//
//        val exif = if (Build.VERSION.SDK_INT > 23) {
//            ExifInterface(input)
//        } else {
//            ExifInterface(uri.path!!)
//        }

        val exif = ExifInterface(path)

        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270)
            else -> bitmap
        }
    }

    private fun rotateImage(bitmap: Bitmap, degree: Int): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }


}