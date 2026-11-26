package com.yasminhosam.brandora.feature.products.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.net.toUri
import com.yasminhosam.brandora.feature.products.domain.ProductImageReader
import com.yasminhosam.brandora.feature.products.domain.model.ImageUpload
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductImageFiles @Inject constructor(@ApplicationContext private val context: Context) :
    ProductImageReader {

    override suspend fun read(uris: List<String>): List<ImageUpload> =
        withContext(Dispatchers.IO) {
            require(uris.size in 1..5)
            // Process one image at a time so only one large bitmap is in memory at once.
            uris.map { compress(context.contentResolver, it.toUri()) }
        }

    private fun compress(resolver: ContentResolver, uri: Uri): ImageUpload {
        // 1. Trust the file header, not the MIME type reported by the gallery (it may be null or "image/jpg").
        val header = ByteArray(12)
        val read = resolver.openInputStream(uri)?.use { it.readFully(header) }
            ?: error("Image cannot be opened")
        require(read >= 12 && isSupportedImage(header))

        // 2. Read only the dimensions (no pixels in memory yet).
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val width = bounds.outWidth
        val height = bounds.outHeight
        require(width > 0 && height > 0)

        // 3. Decode with a power-of-two sample size to avoid OutOfMemoryError on 50MP photos.
        var sample = 1
        while (width / (sample * 2) >= MAX_DIMENSION && height / (sample * 2) >= MAX_DIMENSION) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("Image cannot be decoded")

        // 4. Exact downscale to MAX_DIMENSION + apply EXIF rotation/flip in one pass.
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix().apply {
            val longest = maxOf(decoded.width, decoded.height)
            if (longest > MAX_DIMENSION) {
                val scale = MAX_DIMENSION.toFloat() / longest
                postScale(scale, scale)
            }
            applyExifOrientation(orientation)
        }
        val transformed = Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height, matrix, true,
        )
        if (transformed !== decoded) decoded.recycle()

        // 5. JPEG has no alpha: flatten transparent PNG/WebP onto white instead of black.
        val flattened = if (transformed.hasAlpha()) {
            Bitmap.createBitmap(transformed.width, transformed.height, Bitmap.Config.ARGB_8888)
                .also { target ->
                    Canvas(target).apply {
                        drawColor(Color.WHITE)
                        drawBitmap(transformed, 0f, 0f, null)
                    }
                    transformed.recycle()
                }
        } else {
            transformed
        }

        // 6. Lower the quality step by step until the result fits the size budget.
        try {
            var bytes = ByteArray(0)
            for (quality in QUALITY_STEPS) {
                val out = ByteArrayOutputStream()
                flattened.compress(Bitmap.CompressFormat.JPEG, quality, out)
                bytes = out.toByteArray()
                if (bytes.size <= MAX_OUTPUT_BYTES) break
            }
            require(bytes.isNotEmpty() && bytes.size <= MAX_OUTPUT_BYTES)
            // Always JPEG after re-encoding, so the MIME problem disappears.
            return ImageUpload(bytes, "image/jpeg")
        } finally {
            flattened.recycle()
        }
    }

    private fun InputStream.readFully(buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val n = read(buffer, total, buffer.size - total)
            if (n < 0) break
            total += n
        }
        return total
    }

    private fun isSupportedImage(h: ByteArray): Boolean {
        fun b(i: Int) = h[i].toInt() and 0xFF
        val jpeg = b(0) == 0xFF && b(1) == 0xD8 && b(2) == 0xFF
        val png = b(0) == 0x89 && b(1) == 0x50 && b(2) == 0x4E && b(3) == 0x47
        val webp = String(h, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                String(h, 8, 4, Charsets.US_ASCII) == "WEBP"
        return jpeg || png || webp
    }

    private fun Matrix.applyExifOrientation(orientation: Int) {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
        }
    }

    private companion object {
        const val MAX_DIMENSION = 2048
        const val MAX_OUTPUT_BYTES = 4 * 1024 * 1024 // stay safely under the backend's 5 MB guard
        val QUALITY_STEPS = intArrayOf(85, 75, 65, 55)
    }
}