package com.srijeesolution.rojgaarwaala.utils

/**
 * Home and Free Job are built around what work the seeker wants and where, so both
 * stay locked until the profile has preferred work, a current address and a district.
 *
 * Decided from the profile the server returned, never from the locally cached
 * preference: that cache is written before a save succeeds and survives logout.
 */
object JobPreferenceGate {

    enum class State {
        /** Profile not loaded yet, or the load failed. Never blocks. */
        UNKNOWN,
        PRESENT,
        MISSING,
    }

    /** In the order the profile screen shows them. */
    enum class Field(val label: String) {
        PREFERRED_WORK("पसंदीदा काम"),
        ADDRESS("आपका अभी का पता"),
        DISTRICT("किस जिले में काम ढूँढ रहे हैं"),
    }

    fun missingFields(
        categories: List<String>?,
        legacyCategory: String?,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        district: String?,
    ): List<Field> {
        val missing = mutableListOf<Field>()

        val hasWork = categories.orEmpty().any { it.isNotBlank() } ||
            legacyCategory.orEmpty().split(",").any { it.isNotBlank() }
        if (!hasWork) missing += Field.PREFERRED_WORK

        // The address is set by dropping a map pin, so a saved pin counts even when
        // the place could not be turned into text.
        val hasAddress = !address.isNullOrBlank() || (latitude != null && longitude != null)
        if (!hasAddress) missing += Field.ADDRESS

        if (district.isNullOrBlank()) missing += Field.DISTRICT

        return missing
    }

    fun stateFor(missing: List<Field>): State =
        if (missing.isEmpty()) State.PRESENT else State.MISSING

    fun blocks(state: State, tabIndex: Int): Boolean =
        state == State.MISSING && (tabIndex == MainTabs.HOME || tabIndex == MainTabs.FREE_JOB)

    fun message(missing: List<Field>): String = buildString {
        append("होम और Free Job देखने के लिए प्रोफ़ाइल में यह भरकर सेव करें:")
        missing.forEach { append("\n• ").append(it.label) }
    }
}
