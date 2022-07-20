package com.oldeee.oldeeimageutil

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.*

object OldeeImageUtil {
    data class NeedResize(
        val need:Boolean,
        val width:Int,
        val height:Int
    )

    const val MAX = 1024

    enum class MODE {
        DEBUG,
        REAL
    }

    private var mode = MODE.DEBUG

    fun setMode(m: MODE) {
        mode = m
    }

    private fun createImageFile(storageDir: File): File {
        val timeStamp: String = UUID.randomUUID().toString()
        return File.createTempFile(
            "COMP_JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    fun isJpegImage(context: Context, uri: Uri): Boolean {
        val mimeType = uri.let { returnUri ->
            context.contentResolver?.getType(uri)
        }

        return mimeType == "image/jpeg"
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


    private fun resizeBitmapFormUri(uri: Uri, context: Context): Bitmap? {
        var input = context.contentResolver.openInputStream(uri)

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)

        input?.close()

        var bitmap: Bitmap?

        BitmapFactory.Options().run {
            inSampleSize = calculateInSampleSize(options)
            input = context.contentResolver.openInputStream(uri)
            bitmap = BitmapFactory.decodeStream(input, null, this)
        }

        // 아래에 회전된 이미지 되돌리기에서 다시 언급할게용 :)
        bitmap = bitmap?.let {
            rotateImageIfRequired(context, bitmap!!, uri)
        }

//        input?.close()

        return bitmap
    }


    /**
     * 리사이즈가 필요한지 판단
     */
    fun needResize(context: Context, uri: Uri): NeedResize {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val input = context.contentResolver!!.openInputStream(uri)

        var bitmap: Bitmap?
        BitmapFactory.Options().run {
            bitmap = BitmapFactory.decodeStream(input, null, this)
        }

        input?.close()

        Log.e("#debug", "image data: w->${bitmap?.width}, h->${bitmap?.height}")
        if (bitmap == null)
            return NeedResize(false, 0, 0)
        else {
            val need = bitmap?.width!! > MAX || bitmap?.height!! > MAX
            return NeedResize(need, bitmap?.width?:0, bitmap?.height?:0)
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


    private fun rotateImage(bitmap: Bitmap, degree: Int): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}