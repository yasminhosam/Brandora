package com.yasminhosam.brandora.feature.catalogimport.data.file

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.yasminhosam.brandora.feature.catalogimport.domain.file.ExcelFileFactory
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

/**
 * The Uri is captured inside ExcelFile.openStream and never leaves this class, keeping
 * android.net.Uri out of the domain package. An unidentified file gets an extensionless name
 * so local validation rejects it instead of silently accepting it.
 */
class UriExcelFileFactory @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ExcelFileFactory {

    /**
     * Reads name and size WITHOUT opening the stream (it opens later, during upload).
     * Never throws: failure returns null and becomes a validation error, not a crash.
     */
    override fun create(uri: String): ExcelFile? = try {
        build(Uri.parse(uri))
    } catch (e: RuntimeException) {
        // SecurityException (permission revoked), IllegalStateException (dead provider),
        // UnsupportedOperationException (provider cannot answer the query).
        null
    }

    private fun build(uri: Uri): ExcelFile {
        val resolver = context.contentResolver
        val displayName = queryDisplayName(resolver, uri)
            ?: uri.lastPathSegment
            ?: UNKNOWN_NAME
        val sizeBytes = querySizeBytes(resolver, uri)
            ?: descriptorSize(resolver, uri)
            ?: ExcelFile.UNKNOWN_SIZE

        return ExcelFile(displayName, sizeBytes) {
            resolver.openInputStream(uri)
                ?: throw IOException("Cannot open $displayName")
        }
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getString(index)
            else null
        }

    private fun querySizeBytes(resolver: ContentResolver, uri: Uri): Long? =
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index)
            else null
        }

    private fun descriptorSize(resolver: ContentResolver, uri: Uri): Long? = runCatching {
        resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.statSize.takeIf { it >= 0L }
        }
    }.getOrNull()

    private companion object {
        // Deliberately extensionless: an unidentified file must fail validation, not pass it.
        const val UNKNOWN_NAME = "unknown"
    }
}
