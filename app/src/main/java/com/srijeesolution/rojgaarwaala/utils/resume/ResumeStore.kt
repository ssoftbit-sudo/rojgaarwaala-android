package com.srijeesolution.rojgaarwaala.utils.resume

import android.content.Context
import org.json.JSONObject
import java.io.File

/** The builder's data, photo and last PDF, in the app's private storage on this phone. */
object ResumeStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "resume").apply { mkdirs() }

    fun photoFile(context: Context): File = File(dir(context), "photo.jpg")

    fun pdfFile(context: Context, data: ResumeData): File {
        // Plain ASCII, since the name travels in the upload; a Hindi name falls back to "resume.pdf".
        val safe = data.name.trim().replace(Regex("[^A-Za-z0-9]+"), "_").trim('_')
        return File(dir(context), if (safe.isBlank()) "resume.pdf" else "${safe}_resume.pdf")
    }

    fun load(context: Context): ResumeData? {
        val file = File(dir(context), "resume.json")
        if (!file.exists()) return null
        return runCatching { ResumeData.fromJson(JSONObject(file.readText())) }.getOrNull()
    }

    fun save(context: Context, data: ResumeData) {
        File(dir(context), "resume.json").writeText(data.toJson().toString())
    }
}
