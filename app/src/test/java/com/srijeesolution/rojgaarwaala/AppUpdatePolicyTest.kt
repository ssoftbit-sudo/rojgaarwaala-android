package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.AppUpdatePolicy
import com.srijeesolution.rojgaarwaala.utils.AppUpdatePolicy.Prompt
import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdatePolicyTest {

  @Test
  fun `installed build equal to latest is not prompted`() {
    assertEquals(Prompt.NONE, AppUpdatePolicy.decide(47, 18, 47, false))
  }

  @Test
  fun `installed build newer than latest is not prompted`() {
    assertEquals(Prompt.NONE, AppUpdatePolicy.decide(48, 18, 47, true))
  }

  @Test
  fun `older build gets an optional prompt when force is off`() {
    assertEquals(Prompt.OPTIONAL, AppUpdatePolicy.decide(47, 18, 48, false))
  }

  @Test
  fun `build below min with force on must update`() {
    assertEquals(Prompt.REQUIRED, AppUpdatePolicy.decide(47, 48, 48, true))
  }

  @Test
  fun `build below min with force off is only an optional prompt`() {
    assertEquals(Prompt.OPTIONAL, AppUpdatePolicy.decide(47, 48, 48, false))
  }

  @Test
  fun `force on but build meets min falls back to optional`() {
    assertEquals(Prompt.OPTIONAL, AppUpdatePolicy.decide(47, 40, 48, true))
  }

  @Test
  fun `min above latest still prompts`() {
    assertEquals(Prompt.OPTIONAL, AppUpdatePolicy.decide(47, 48, 18, false))
  }

  @Test
  fun `missing config values do not prompt`() {
    assertEquals(Prompt.NONE, AppUpdatePolicy.decide(47, null, null, null))
  }
}
