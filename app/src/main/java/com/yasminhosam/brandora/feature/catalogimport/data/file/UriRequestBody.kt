package com.yasminhosam.brandora.feature.catalogimport.data.file

import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

/**
 * Streams the file straight into OkHttp's sink without holding it in memory. The stream is
 * reopened on every writeTo() call so the body stays replayable; a -1 length makes OkHttp
 * fall back to a chunked body.
 */
internal class UriRequestBody(private val file: ExcelFile) : RequestBody() {

    override fun contentType(): MediaType = XLSX_MEDIA_TYPE

    override fun contentLength(): Long = file.sizeBytes

    override fun writeTo(sink: BufferedSink) {
        file.openStream().use { input -> sink.writeAll(input.source()) }
    }

    companion object {
        private val XLSX_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaType()
    }
}
