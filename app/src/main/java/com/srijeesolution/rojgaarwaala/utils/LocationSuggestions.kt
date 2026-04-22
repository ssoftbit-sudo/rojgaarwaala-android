package com.srijeesolution.rojgaarwaala.utils

object LocationSuggestions {
    val districtList = listOf(
        "Ahmedabad", "Amreli", "Anand", "Aravalli", "Banaskantha", "Bharuch",
        "Bhavnagar", "Botad", "Chhota Udaipur", "Dahod", "Dang", "Devbhoomi Dwarka",
        "Gandhinagar", "Gir Somnath", "Jamnagar", "Junagadh", "Kheda", "Kutch",
        "Mahisagar", "Mehsana", "Morbi", "Narmada", "Navsari", "Panchmahal",
        "Patan", "Porbandar", "Rajkot", "Sabarkantha", "Surat", "Surendranagar",
        "Tapi", "Vadodara", "Valsad"
    )

    fun filter(query: String, source: List<String> = districtList): List<String> {
        if (query.isBlank()) return source.take(8)
        return source
            .filter { it.contains(query, ignoreCase = true) }
            .sortedBy { it.lowercase() }
            .take(12)
    }
}
