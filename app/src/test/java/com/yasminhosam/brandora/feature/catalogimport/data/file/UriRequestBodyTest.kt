package com.yasminhosam.brandora.feature.catalogimport.data.file

import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.InputStream

class UriRequestBodyTest {

    private fun file(
        name: String = "catalog.xlsx",
        sizeBytes: Long = 4L,
        content: String = "test"
    ) = ExcelFile(name, sizeBytes) { content.byteInputStream() }

    @Test
    fun `declares the xlsx content type`() {
        val body = UriRequestBody(file())

        assertEquals(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            body.contentType().toString()
        )
    }

    @Test
    fun `content length mirrors the size reported by the content provider`() {
        // Read into a local first: contentLength() returns a platform long, which makes
        // assertEquals ambiguous between its (Any, Any) and (Long, Long) overloads.
        val length: Long = UriRequestBody(file(sizeBytes = 4L)).contentLength()

        assertEquals(4L, length)
    }

    @Test
    fun `unknown size is reported as -1 so okhttp falls back to a chunked body`() {
        val length: Long = UriRequestBody(file(sizeBytes = ExcelFile.UNKNOWN_SIZE)).contentLength()

        assertEquals(ExcelFile.UNKNOWN_SIZE, length)
    }

    @Test
    fun `writing copies the file bytes without loss`() {
        val body = UriRequestBody(file(content = "SKU-NEW-001"))
        val sink = Buffer()

        body.writeTo(sink)

        assertEquals("SKU-NEW-001", sink.readUtf8())
    }

    @Test
    fun `writing twice reopens the stream so the body stays replayable`() {
        val body = UriRequestBody(file(content = "hello"))

        val first = Buffer()
        val second = Buffer()
        body.writeTo(first)
        body.writeTo(second)

        assertEquals("hello", first.readUtf8())
        assertEquals("hello", second.readUtf8())
    }

    @Test
    fun `the underlying stream is closed after writing`() {
        var closed = false
        val body = UriRequestBody(
            ExcelFile("catalog.xlsx", 4L) {
                val original = "test".byteInputStream()
                object : InputStream() {
                    override fun read(): Int = original.read().also { if (it == -1) closed = true }
                    override fun close() { closed = true; original.close() }
                }
            }
        )

        body.writeTo(Buffer())

        assertEquals(true, closed)
    }
}
