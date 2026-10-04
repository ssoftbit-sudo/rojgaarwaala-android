package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.LocationPermissionPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LocationPermissionPolicyTest {

    @Test
    fun `while using the app is enough foreground permission`() {
        assertTrue(LocationPermissionPolicy.hasForegroundLocation(fineGranted = true, coarseGranted = false))
        assertTrue(LocationPermissionPolicy.hasForegroundLocation(fineGranted = false, coarseGranted = true))
        assertFalse(LocationPermissionPolicy.hasForegroundLocation(fineGranted = false, coarseGranted = false))
    }

    @Test
    fun `manifest never asks for background location`() {
        val manifest = listOf("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml")
            .map { File(it) }
            .first { it.exists() }
            .readText()

        // The only mention allowed is the rule that strips it from merged libraries.
        val mention = Regex("""<uses-permission[^>]*ACCESS_BACKGROUND_LOCATION[^>]*>""").findAll(manifest).toList()
        assertTrue(mention.size == 1 && mention[0].value.contains("tools:node=\"remove\""))
        assertFalse(manifest.contains("FactoryArrivalReceiver"))
    }
}
