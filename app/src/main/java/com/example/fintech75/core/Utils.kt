package com.example.fintech75.core

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class Utils {
    companion object {
        private const val patternDateTime: String = "yyyy-MM-dd HH:mm:ss"

        fun generateUTCDateTimeString(): String {
            val currentTime: Date = Date()
            val simpleDateFormat = SimpleDateFormat(patternDateTime, Locale.ENGLISH)
            simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")

            return simpleDateFormat.format(currentTime)
        }

        // timestamp must be in seconds
        fun getLocalTimeStringFromUTCTimestamp(timestamp: Double): String {
            val timestampMillis: Long = (timestamp * 1000).toLong()

            val simpleDateFormat = SimpleDateFormat(patternDateTime, Locale.ENGLISH)
            simpleDateFormat.timeZone = TimeZone.getDefault()
            val utcTimeString: String = simpleDateFormat.format(timestampMillis)
            simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date: Date = simpleDateFormat.parse(utcTimeString) as Date
            simpleDateFormat.timeZone = TimeZone.getDefault()


            return simpleDateFormat.format(date)
        }

        fun <T> mapToJSON(objectToMap: T): String {
            return GsonBuilder().setPrettyPrinting().create().toJson(objectToMap)
        }

        inline fun <reified T> jsonToClass(jsonString: String): T {
            return Gson().fromJson(jsonString, T::class.java)
        }

        fun saveJSONAsFile(data: String, fileName: String, path: String = "./"): Boolean {
            val name: String = if (!fileName.endsWith(".json")) { "$fileName.json" } else { fileName }
            val correctPath: String = if (!path.endsWith("/")) { "$path/" } else { path }

            try {
                val saveFile: File = File("$correctPath$name")
                saveFile.writeText(data, Charsets.UTF_8)
            } catch (e: Exception) {
                return false
            }

            return true
        }

        @kotlin.jvm.Throws(IOException::class)
        fun readJSONFromFile(path: String): String {
            val correctPath: String = if (!path.endsWith(".json")) { "$path.json" } else { path }
            val readFile: File = File(correctPath)

            return readFile.readText(Charsets.UTF_8)
        }
    }
}