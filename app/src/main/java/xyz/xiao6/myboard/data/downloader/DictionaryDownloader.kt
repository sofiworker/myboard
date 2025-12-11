package xyz.xiao6.myboard.data.downloader

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class DictionaryDownloader(private val context: Context) {

    fun download(urlString: String, destinationFileName: String) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("Server returned HTTP " + connection.responseCode + " " + connection.responseMessage)
        }

        val inputStream = connection.inputStream
        val file = File(context.filesDir, destinationFileName)
        val fileOutputStream = FileOutputStream(file)

        val buffer = ByteArray(1024)
        var len1 = inputStream.read(buffer)
        while (len1 != -1) {
            fileOutputStream.write(buffer, 0, len1)
            len1 = inputStream.read(buffer)
        }

        fileOutputStream.close()
        inputStream.close()
    }
}
