package com.srijeesolution.rojgaarwaala.utils.resume

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * What a seeker fills in the resume builder. Kept on the phone only (see ResumeStore):
 * the finished PDF is what reaches the server, as the profile or application resume.
 *
 * Preset choices (education level, skills, languages, licences) are stored as keys,
 * so the same resume can be printed in Hindi or English. Anything the seeker typed is
 * kept as typed.
 */
data class ResumeData(
    val name: String = "",
    val mobile: String = "",
    val email: String = "",
    /** yyyy-MM-dd */
    val dob: String = "",
    val district: String = "",
    val address: String = "",
    /** The kinds of work they are looking for, shown under the name. */
    val roles: List<String> = emptyList(),
    /** Where they want to work: district names, or [ResumeCopy.ANYWHERE]. */
    val jobLocations: List<String> = emptyList(),
    val photoPath: String? = null,
    val fresher: Boolean = false,
    val experiences: List<Experience> = emptyList(),
    val education: List<Education> = emptyList(),
    val skills: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val licences: List<String> = emptyList(),
    val language: ResumeLanguage = ResumeLanguage.HINDI,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("mobile", mobile)
        .put("email", email)
        .put("dob", dob)
        .put("district", district)
        .put("address", address)
        .put("roles", JSONArray(roles))
        .put("job_locations", JSONArray(jobLocations))
        .put("photo_path", photoPath ?: "")
        .put("fresher", fresher)
        .put("experiences", JSONArray(experiences.map { it.toJson() }))
        .put("education", JSONArray(education.map { it.toJson() }))
        .put("skills", JSONArray(skills))
        .put("languages", JSONArray(languages))
        .put("licences", JSONArray(licences))
        .put("language", language.name)

    companion object {
        fun fromJson(json: JSONObject): ResumeData = ResumeData(
            name = json.optString("name"),
            mobile = json.optString("mobile"),
            email = json.optString("email"),
            dob = json.optString("dob"),
            district = json.optString("district"),
            address = json.optString("address"),
            // Drafts saved before roles were a list kept one comma-separated line.
            roles = json.optJSONArray("roles").strings().ifEmpty {
                json.optString("headline").split(",").map { it.trim() }.filter { it.isNotBlank() }
            },
            jobLocations = json.optJSONArray("job_locations").strings(),
            photoPath = json.optString("photo_path").takeIf { it.isNotBlank() },
            fresher = json.optBoolean("fresher"),
            experiences = json.optJSONArray("experiences").objects().map { Experience.fromJson(it) },
            education = json.optJSONArray("education").objects().map { Education.fromJson(it) },
            skills = json.optJSONArray("skills").strings(),
            languages = json.optJSONArray("languages").strings(),
            licences = json.optJSONArray("licences").strings(),
            language = runCatching { ResumeLanguage.valueOf(json.optString("language")) }
                .getOrDefault(ResumeLanguage.HINDI),
        )

        private fun JSONArray?.objects(): List<JSONObject> =
            if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

        private fun JSONArray?.strings(): List<String> =
            if (this == null) emptyList() else (0 until length()).map { optString(it) }.filter { it.isNotBlank() }
    }
}

data class Experience(
    val role: String,
    val company: String,
    val years: Int,
) {
    fun toJson(): JSONObject = JSONObject().put("role", role).put("company", company).put("years", years)

    companion object {
        fun fromJson(json: JSONObject) = Experience(
            role = json.optString("role"),
            company = json.optString("company"),
            years = json.optInt("years"),
        )
    }
}

data class Education(
    /** One of [ResumeCopy.EDUCATION_LEVELS]. */
    val level: String,
    val institute: String,
    val year: Int,
) {
    fun toJson(): JSONObject = JSONObject().put("level", level).put("institute", institute).put("year", year)

    companion object {
        fun fromJson(json: JSONObject) = Education(
            level = json.optString("level"),
            institute = json.optString("institute"),
            year = json.optInt("year"),
        )
    }
}

enum class ResumeLanguage { HINDI, ENGLISH }

/** Rules for each builder step, kept pure so JVM tests cover them. */
object ResumeRules {

    const val MIN_AGE = 14
    const val MAX_AGE = 80

    // Caps that keep the resume on one A4 page.
    const val MAX_ROLES = 5
    const val MAX_EXPERIENCES = 4
    const val MAX_EDUCATION = 4
    const val MAX_SKILLS = 12

    /** Age in whole years on [today], or null when the date is missing or malformed. */
    fun ageOn(dob: String, today: Calendar = Calendar.getInstance()): Int? {
        val parts = dob.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return null
        val (year, month, day) = parts
        if (month !in 1..12 || day !in 1..31) return null
        var age = today.get(Calendar.YEAR) - year
        val beforeBirthday = today.get(Calendar.MONTH) + 1 < month ||
            (today.get(Calendar.MONTH) + 1 == month && today.get(Calendar.DAY_OF_MONTH) < day)
        if (beforeBirthday) age--
        return age
    }

    /** What is still missing on step [step] (1 to 3), in the words shown to the seeker. */
    fun missing(step: Int, data: ResumeData, today: Calendar = Calendar.getInstance()): List<String> {
        val out = mutableListOf<String>()
        when (step) {
            1 -> {
                if (data.photoPath.isNullOrBlank()) out += "फोटो लगाएँ"
                if (data.name.isBlank()) out += "नाम लिखें"
                if (data.mobile.count { it.isDigit() } != 10) out += "अभी का 10 अंकों का फोन नंबर लिखें"
                val age = ageOn(data.dob, today)
                if (age == null) {
                    out += "जन्म तिथि चुनें"
                } else if (age !in MIN_AGE..MAX_AGE) {
                    out += "जन्म तिथि सही चुनें"
                }
                if (data.district.isBlank()) out += "अभी किस ज़िले में रहते हैं, लिखें"
            }
            2 -> {
                if (data.roles.isEmpty()) out += "कौन सा काम चाहिए, कम से कम एक जोड़ें"
                if (data.jobLocations.isEmpty()) out += "कहाँ नौकरी चाहिए, जगह चुनें"
                if (!data.fresher && data.experiences.isEmpty()) out += "काम का अनुभव जोड़ें, या फ्रेशर चुनें"
            }
            3 -> if (data.education.isEmpty()) out += "कम से कम एक पढ़ाई जोड़ें"
        }
        return out
    }

    /** Years offered for a passing year: this year back to 1970. */
    fun passingYears(today: Calendar = Calendar.getInstance()): List<Int> =
        (today.get(Calendar.YEAR) downTo 1970).toList()
}
