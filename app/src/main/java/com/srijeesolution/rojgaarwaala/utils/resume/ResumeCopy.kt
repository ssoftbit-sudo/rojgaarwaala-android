package com.srijeesolution.rojgaarwaala.utils.resume

/**
 * Everything printed on the resume that the seeker did not type, in both languages.
 * Preset choices are stored as these keys so a resume can be reprinted in either.
 */
object ResumeCopy {

    val EDUCATION_LEVELS = listOf(
        "CLASS_8", "CLASS_10", "CLASS_12", "ITI", "DIPLOMA", "GRADUATE", "POST_GRADUATE", "OTHER",
    )

    val SKILLS = listOf(
        "COMPUTER", "MS_OFFICE", "TYPING", "DRIVING", "SALES", "ACCOUNTS", "MACHINE_OPERATOR",
        "ELECTRICIAN", "WELDING", "PLUMBING", "TAILORING", "COOKING", "HOUSEKEEPING", "SECURITY",
    )

    val LANGUAGES = listOf("HINDI", "ENGLISH", "CHHATTISGARHI")

    val LICENCES = listOf("TWO_WHEELER", "FOUR_WHEELER", "HEAVY_VEHICLE")

    /** Job location meaning "any district". */
    const val ANYWHERE = "ANYWHERE_CG"

    private val labels: Map<String, Pair<String, String>> = mapOf(
        // education
        "CLASS_8" to ("8वीं" to "8th"),
        "CLASS_10" to ("10वीं" to "10th"),
        "CLASS_12" to ("12वीं" to "12th"),
        "ITI" to ("ITI" to "ITI"),
        "DIPLOMA" to ("डिप्लोमा" to "Diploma"),
        "GRADUATE" to ("ग्रेजुएट" to "Graduate"),
        "POST_GRADUATE" to ("पोस्ट ग्रेजुएट" to "Post Graduate"),
        "OTHER" to ("अन्य" to "Other"),
        // skills
        "COMPUTER" to ("कंप्यूटर" to "Computer"),
        "MS_OFFICE" to ("MS Office" to "MS Office"),
        "TYPING" to ("टाइपिंग" to "Typing"),
        "DRIVING" to ("ड्राइविंग" to "Driving"),
        "SALES" to ("सेल्स" to "Sales"),
        "ACCOUNTS" to ("हिसाब-किताब" to "Accounts"),
        "MACHINE_OPERATOR" to ("मशीन चलाना" to "Machine operation"),
        "ELECTRICIAN" to ("बिजली का काम" to "Electrical work"),
        "WELDING" to ("वेल्डिंग" to "Welding"),
        "PLUMBING" to ("प्लंबिंग" to "Plumbing"),
        "TAILORING" to ("सिलाई" to "Tailoring"),
        "COOKING" to ("खाना बनाना" to "Cooking"),
        "HOUSEKEEPING" to ("साफ़-सफ़ाई" to "Housekeeping"),
        "SECURITY" to ("सिक्योरिटी" to "Security"),
        // languages
        "HINDI" to ("हिंदी" to "Hindi"),
        "ENGLISH" to ("अंग्रेज़ी" to "English"),
        "CHHATTISGARHI" to ("छत्तीसगढ़ी" to "Chhattisgarhi"),
        // licences
        "TWO_WHEELER" to ("दोपहिया" to "Two-wheeler"),
        "FOUR_WHEELER" to ("चारपहिया" to "Four-wheeler"),
        "HEAVY_VEHICLE" to ("भारी वाहन" to "Heavy vehicle"),
        // job location
        ANYWHERE to ("पूरे छत्तीसगढ़ में कहीं भी" to "Anywhere in Chhattisgarh"),
    )

    /** A preset key in [language]; anything else is the seeker's own words and is kept. */
    fun label(key: String, language: ResumeLanguage): String {
        val pair = labels[key] ?: return key
        return if (language == ResumeLanguage.HINDI) pair.first else pair.second
    }

    enum class Text(val hindi: String, val english: String) {
        EXPERIENCE("काम का अनुभव", "Work Experience"),
        EDUCATION("पढ़ाई", "Education"),
        SKILLS("हुनर", "Skills"),
        LANGUAGES("भाषाएँ", "Languages"),
        LICENCES("ड्राइविंग लाइसेंस", "Driving Licence"),
        PERSONAL("व्यक्तिगत जानकारी", "Personal Details"),
        CONTACT("संपर्क", "Contact"),
        DOB("जन्म तिथि", "Date of Birth"),
        AGE("उम्र", "Age"),
        ADDRESS("पता", "Address"),
        DISTRICT("ज़िला", "District"),
        MOBILE("फोन", "Phone"),
        EMAIL("ईमेल", "Email"),
        PASSING_YEAR("पास होने का साल", "Passing year"),
        YEARS("साल", "years"),
        YEAR("साल", "year"),
        LESS_THAN_A_YEAR("1 साल से कम", "Less than a year"),
        FRESHER("फ्रेशर — काम सीखने और करने को तैयार", "Fresher — ready to learn and work"),
        DECLARATION("ऊपर दी गई सारी जानकारी सही है।", "I declare that the information above is true."),
        DATE("तारीख", "Date"),
        PLACE("स्थान", "Place"),
        SIGNATURE("हस्ताक्षर", "Signature"),
        LOOKING_FOR("काम की तलाश", "Looking for"),
        JOB_LOCATION("नौकरी की जगह", "Preferred job location"),
        CURRENT_ADDRESS("अभी का पता", "Current address"),
        QUALIFICATION("कक्षा / डिग्री", "Qualification"),
        INSTITUTE("स्कूल / कॉलेज / बोर्ड", "School / College / Board"),
        YEAR_COLUMN("साल", "Year"),
        LANGUAGES_LICENCE("भाषा और लाइसेंस", "Languages & Licence"),
        DECLARATION_TITLE("घोषणा", "Declaration"),
        ;

        fun of(language: ResumeLanguage) = if (language == ResumeLanguage.HINDI) hindi else english
    }

    fun duration(years: Int, language: ResumeLanguage): String = when {
        years <= 0 -> Text.LESS_THAN_A_YEAR.of(language)
        years == 1 -> "1 ${Text.YEAR.of(language)}"
        else -> "$years ${Text.YEARS.of(language)}"
    }

    private val hindiMonths = listOf("जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर")
    private val englishMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /** "2001-08-15" as "15 अगस्त 2001" or "15 Aug 2001"; the raw value if it does not parse. */
    fun date(isoDate: String, language: ResumeLanguage): String {
        val parts = isoDate.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3 || parts[1] !in 1..12) return isoDate
        val months = if (language == ResumeLanguage.HINDI) hindiMonths else englishMonths
        return "${parts[2]} ${months[parts[1] - 1]} ${parts[0]}"
    }
}
