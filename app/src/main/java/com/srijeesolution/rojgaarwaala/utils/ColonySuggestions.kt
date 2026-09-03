package com.srijeesolution.rojgaarwaala.utils

object ColonySuggestions {

    private val genericColonies = listOf(
        "Civil Lines", "Main Road", "Station Road", "Bus Stand Area", "Industrial Area",
        "Housing Board", "New Market", "Old City", "Near Railway Station",
    )

    private val districtColonies = mapOf(
        "raipur" to listOf(
            "Pandri", "Shankar Nagar", "Telibandha", "Avanti Vihar", "Devendra Nagar",
            "Gudhiyari", "Mowa", "Bhatagaon", "Khamtarai", "Urla", "Magarpatta",
        ),
        "bilaspur" to listOf(
            "Koni", "Sarkanda", "Torwa", "Belha", "Seepat Road", "Mangla", "Jarhabhata",
        ),
        "durg" to listOf(
            "Supela", "Chandra Nagar", "Padmanabhpur", "Bhilai Charoda", "Risali",
        ),
        "bhilai" to listOf(
            "Supela", "Sector 1", "Sector 4", "Sector 6", "Sector 10", "Risali", "Charoda",
        ),
        "korba" to listOf(
            "CSEB Colony", "Balco Nagar", "Darri", "Katghora Road", "Pali",
        ),
        "rajnandgaon" to listOf(
            "Ambagarh Chowki Road", "Dongargaon Road", "Station Road", "Gandai",
        ),
        "dhamtari" to listOf("Station Road", "Kurud Road", "Civil Lines"),
        "jagdalpur" to listOf("Civil Lines", "Geedam Road", "Kanker Road", "Municipal Colony"),
        "raigarh" to listOf("Station Road", "Kharsia Road", "Civil Lines"),
        "ambikapur" to listOf("Civil Lines", "Station Road", "Bishunpur Road"),
    )

    fun forDistrict(district: String?): List<String> {
        val key = district.orEmpty().trim().lowercase()
        if (key.isEmpty() || HomeLocationDefaults.skipsDistrictFilter(district)) {
            return genericColonies
        }

        val exact = districtColonies[key].orEmpty()
        val partial = districtColonies.entries
            .filter { (name, _) -> key.contains(name) || name.contains(key) }
            .flatMap { it.value }

        return (exact + partial + genericColonies).distinct()
    }

    fun filter(query: String, district: String?): List<String> {
        val source = forDistrict(district)
        if (query.isBlank()) return source.take(10)
        return source
            .filter { it.contains(query, ignoreCase = true) }
            .take(12)
    }
}
