package com.srijeesolution.rojgaarwaala.utils

/**
 * Turns Geocoder address parts into the profile fields the backend stores.
 *
 * City / state are never typed by the user; colony and pincode can still be edited
 * on the profile screen after the pin is saved.
 */
data class MapAddressParts(
    val address: String,
    val city: String,
    val state: String,
    val colony: String,
    val pincode: String,
)

object MapAddressMapper {

    fun fromParts(
        addressLine: String?,
        locality: String?,
        subAdminArea: String?,
        adminArea: String?,
        subLocality: String?,
        thoroughfare: String?,
        featureName: String?,
        postalCode: String?,
    ): MapAddressParts {
        val city = firstNonBlank(locality, subAdminArea)
        val state = adminArea.orEmpty().trim()
        val colony = firstNonBlank(
            subLocality,
            thoroughfare,
            featureName?.takeUnless { it.isBlank() || it.all { ch -> ch.isDigit() || ch == '-' } },
        )
        val pincode = postalCode.orEmpty().filter { it.isDigit() }
        val address = addressLine?.trim().orEmpty().ifBlank {
            listOf(colony, city, state, pincode).filter { it.isNotBlank() }.joinToString(", ")
        }
        return MapAddressParts(
            address = address,
            city = city,
            state = state,
            colony = colony,
            pincode = pincode,
        )
    }

    private fun firstNonBlank(vararg values: String?): String =
        values.map { it.orEmpty().trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
}
