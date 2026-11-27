package com.amurot.devtools

import com.amurot.devtools.core.AnimationScale
import com.amurot.devtools.core.DevToggles
import com.amurot.devtools.core.Problem
import com.amurot.devtools.core.SettingKey
import com.amurot.devtools.core.SettingsStore
import com.amurot.devtools.core.Toggle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevTogglesTest {
    private class FakeStore : SettingsStore {
        val values = mutableMapOf<SettingKey, String>()
        var refuse = false
        override fun getString(key: SettingKey) = values[key]
        override fun putString(key: SettingKey, value: String): Boolean {
            if (refuse) return false
            values[key] = value
            return true
        }
    }

    private val store = FakeStore()
    private var granted = true
    private var wifi = true
    private val toggles = DevToggles(store, { granted }, { wifi })

    @Test fun `toggle flips a single switch`() {
        assertFalse(toggles.isOn(Toggle.DEV_OPTIONS))
        assertTrue(toggles.toggle(Toggle.DEV_OPTIONS).ok)
        assertTrue(toggles.isOn(Toggle.DEV_OPTIONS))
        toggles.toggle(Toggle.DEV_OPTIONS)
        assertFalse(toggles.isOn(Toggle.DEV_OPTIONS))
    }

    @Test fun `turning on usb debugging also turns on developer options`() {
        toggles.set(Toggle.USB_DEBUGGING, true)
        assertTrue(toggles.isOn(Toggle.DEV_OPTIONS))
        assertTrue(toggles.isOn(Toggle.USB_DEBUGGING))
    }

    @Test fun `turning off developer options leaves adb alone`() {
        toggles.setAll(true)
        toggles.set(Toggle.DEV_OPTIONS, false)
        assertTrue(toggles.isOn(Toggle.USB_DEBUGGING))
        assertTrue(toggles.isOn(Toggle.WIRELESS_DEBUGGING))
    }

    @Test fun `wireless debugging needs wifi`() {
        wifi = false
        assertEquals(Problem.NO_WIFI, toggles.set(Toggle.WIRELESS_DEBUGGING, true).problem)
        assertFalse(toggles.isOn(Toggle.WIRELESS_DEBUGGING))
        assertFalse(toggles.isOn(Toggle.DEV_OPTIONS))
    }

    @Test fun `all three turns everything on then off`() {
        assertTrue(toggles.toggleAll().ok)
        assertTrue(toggles.isAllOn())
        Toggle.entries.take(3).forEach { assertTrue(toggles.isOn(it)) }
        assertTrue(toggles.toggleAll().ok)
        Toggle.entries.take(3).forEach { assertFalse(toggles.isOn(it)) }
    }

    @Test fun `all three completes a partial state instead of turning off`() {
        toggles.set(Toggle.DEV_OPTIONS, true)
        toggles.toggleAll()
        assertTrue(toggles.isAllOn())
    }

    @Test fun `all three without wifi turns on the rest and counts as on`() {
        wifi = false
        assertEquals(Problem.NO_WIFI, toggles.toggleAll().problem)
        assertTrue(toggles.isOn(Toggle.DEV_OPTIONS))
        assertTrue(toggles.isOn(Toggle.USB_DEBUGGING))
        assertTrue(toggles.isAllOn())
        // So the next tap turns them off instead of getting stuck.
        assertTrue(toggles.toggleAll().ok)
        assertFalse(toggles.isOn(Toggle.USB_DEBUGGING))
    }

    @Test fun `stay awake uses all charger flags`() {
        toggles.set(Toggle.STAY_AWAKE, true)
        assertEquals("7", store.values[Toggle.STAY_AWAKE.key])
    }

    @Test fun `nothing is written without the permission`() {
        granted = false
        assertEquals(Problem.NO_PERMISSION, toggles.toggleAll().problem)
        assertEquals(Problem.NO_PERMISSION, toggles.toggle(Toggle.SHOW_TAPS).problem)
        assertTrue(store.values.isEmpty())
    }

    @Test fun `refused writes are reported`() {
        store.refuse = true
        assertEquals(Problem.WRITE_REFUSED, toggles.toggle(Toggle.SHOW_TAPS).problem)
    }

    @Test fun `animation scale cycles and writes all three keys`() {
        assertEquals(1f, toggles.animationScale())
        toggles.cycleAnimationScale()
        assertEquals(0.5f, toggles.animationScale())
        AnimationScale.keys.forEach { assertEquals("0.5", store.values[it]) }
        toggles.cycleAnimationScale()
        assertEquals(0f, toggles.animationScale())
        toggles.cycleAnimationScale()
        assertEquals(1f, toggles.animationScale())
        // An odd value (2x) cycles back to 1x.
        toggles.setAnimationScale(2f)
        toggles.cycleAnimationScale()
        assertEquals(1f, toggles.animationScale())
    }

    @Test fun `animation labels`() {
        assertEquals("Off", AnimationScale.label(0f))
        assertEquals("0.5x", AnimationScale.label(0.5f))
        assertEquals("1x", AnimationScale.label(1f))
        assertEquals("2x", AnimationScale.label(2f))
    }
}
