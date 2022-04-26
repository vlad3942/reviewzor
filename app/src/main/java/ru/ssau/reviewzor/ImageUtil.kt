package ru.ssau.reviewzor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
@Throws(IOException::class)
fun createUniqueImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
    val filename = "PlaceBook_" + timeStamp + "_"
    val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(filename, ".jpg", filesDir)
}

fun decodeFileToSize(filePath: String, width: Int, height: Int): Bitmap {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(filePath, options)
    options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, width, height)
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeFile(filePath, options)
}

fun decodeUriStreamToSize(uri: Uri, width: Int, height: Int, context: Context): Bitmap? {
    var inputStream: InputStream? = null
    try {
        val options: BitmapFactory.Options
        inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            options = BitmapFactory.Options()
            options.inJustDecodeBounds = false
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                options.inSampleSize =
                    calculateInSampleSize(options.outWidth, options.outHeight, width, height)
                options.inJustDecodeBounds = false
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                return bitmap
            }
        }
        return null
    } catch (e: Exception) {
        return null
    } finally {
        inputStream?.close()
    }
}

@Throws(IOException::class)
fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap {
    val input: InputStream? = context.contentResolver.openInputStream(selectedImage)
    val path = selectedImage.path
    val ei: ExifInterface = when {
        Build.VERSION.SDK_INT > 23 && input != null -> ExifInterface(input)
        path != null -> ExifInterface(path)
        else -> null
    } ?: return img
    return when (ei.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90.0f) ?: img
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180.0f) ?: img
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270.0f) ?: img
        else -> img
    }
}

private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight &&
            halfWidth / inSampleSize >= reqWidth
        ) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

private fun rotateImage(img: Bitmap, degree: Float): Bitmap? {
    val matrix = Matrix()
    matrix.postRotate(degree)
    val rotatedImg = Bitmap.createBitmap(
        img, 0, 0, img.width,
        img.height, matrix, true
    )
    img.recycle()
    return rotatedImg
}

fun Bitmap.convertToByteArray(): ByteArray = ByteBuffer.allocate(byteCount).apply {
    copyPixelsToBuffer(this)
    rewind()
}.array()