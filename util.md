package com.example.imageutil

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
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

object OldeeImageUtil {
    const val MAX = 1048

    fun optimizeBitmap(context: Context, uri: Uri): String? {
        try {
            val storage = context.cacheDir // 임시 파일 경로
            val fileName = String.format("%s.%s", UUID.randomUUID(), ".jpg") // 임시 파일 이름
            val tempFile = File(storage, fileName)
            tempFile.createNewFile() // 임시 파일 생성

            // 지정된 이름을 가진 파일에 쓸 파일 출력 스트림을 만든다.
            val fos = FileOutputStream(tempFile)

            val bitmap = resizeBitmapFormUri(uri, context)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            // 더 이상 해당 bitmap은 사용하지 않기 때문에 recycle()로 메모리 해제 필수!
            bitmap?.recycle()

            fos.flush()
            fos.close()

            return tempFile.absolutePath // 임시파일 저장경로 리턴
        } catch (e: IOException) {
            Log.e(TAG, "FileUtil - IOException: ${e.message}")
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "FileUtil - FileNotFoundException: ${e.message}")
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "FileUtil - OutOfMemoryError: ${e.message}")
        } catch (e:Exception) {
            Log.e(TAG, "FileUtil - ${e.message}")
        }

        return null
    }

    // 최적화 bitmap 반환
    private fun resizeBitmapFormUri(uri: Uri, context: Context): Bitmap? {
        val input = context.contentResolver.openInputStream(uri)

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        var bitmap: Bitmap?
        BitmapFactory.Options().run {
            inSampleSize = calculateInSampleSize(options)
            bitmap = BitmapFactory.decodeStream(input, null, this)
        }

        // 아래에 회전된 이미지 되돌리기에서 다시 언급할게용 :)
        bitmap = bitmap?.let {
            rotateImageIfRequired(context, bitmap!!, uri)
        }

        input?.close()

        return bitmap
    }

    fun needResize(context: Context, uri:Uri):Boolean{
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val input = context.contentResolver!!.openInputStream(uri)

        var bitmap: Bitmap?
        BitmapFactory.Options().run {
            bitmap = BitmapFactory.decodeStream(input, null, this)
        }

        if(bitmap == null)
            return false
        else{
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

    private fun rotateImage(bitmap: Bitmap, degree: Int): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
