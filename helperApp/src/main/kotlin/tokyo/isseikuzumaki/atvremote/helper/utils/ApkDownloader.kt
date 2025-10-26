package tokyo.isseikuzumaki.atvremote.helper.utils

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Utility class for downloading APK files with progress tracking
 */
class ApkDownloader {

    companion object {
        private const val TAG = "ApkDownloader"
        private const val BUFFER_SIZE = 8192
    }

    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 300_000 // 5 minutes
        }
    }

    /**
     * Download APK from URL to output file with progress callback
     *
     * @param url The URL to download from (signed URL)
     * @param outputFile The destination file
     * @param onProgress Callback for progress updates (0-100)
     */
    suspend fun download(
        url: String,
        outputFile: File,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting download from: $url")
        
        try {
            client.prepareGet(url).execute { response ->
                if (!response.status.isSuccess()) {
                    throw Exception("Download failed with status: ${response.status}")
                }

                val contentLength = response.contentLength() ?: -1
                val channel = response.bodyAsChannel()
                
                outputFile.outputStream().use { output ->
                    var totalBytesRead = 0L
                    val buffer = ByteArray(BUFFER_SIZE)
                    
                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                        if (bytesRead == -1) break
                        
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        // Calculate and report progress
                        if (contentLength > 0) {
                            val progress = ((totalBytesRead * 100) / contentLength).toInt()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    
                    output.flush()
                }
                
                Log.d(TAG, "Download completed: ${outputFile.absolutePath}, size: ${outputFile.length()} bytes")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            outputFile.delete()
            throw e
        }
    }

    fun close() {
        client.close()
    }
}
