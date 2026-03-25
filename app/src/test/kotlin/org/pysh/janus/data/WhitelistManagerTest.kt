package org.pysh.janus.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhitelistManagerTest {

    private lateinit var context: Context
    private lateinit var manager: WhitelistManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(WhitelistManager.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        manager = WhitelistManager(context)
    }

    // ── Whitelist ──

    @Test
    fun getWhitelist_returnsEmptySetByDefault() {
        assertTrue(manager.getWhitelist().isEmpty())
    }

    @Test
    fun saveWhitelist_singlePackage_roundTrips() {
        val packages = setOf("com.example.app")
        manager.saveWhitelist(packages)
        assertEquals(packages, manager.getWhitelist())
    }

    @Test
    fun saveWhitelist_multiplePackages_roundTrips() {
        val packages = setOf("com.example.a", "com.example.b", "com.example.c")
        manager.saveWhitelist(packages)
        assertEquals(packages, manager.getWhitelist())
    }

    @Test
    fun saveWhitelist_emptySet_clearsWhitelist() {
        manager.saveWhitelist(setOf("com.example.app"))
        manager.saveWhitelist(emptySet())
        assertTrue(manager.getWhitelist().isEmpty())
    }

    @Test
    fun saveWhitelist_overwritesPrevious() {
        manager.saveWhitelist(setOf("com.old.app"))
        manager.saveWhitelist(setOf("com.new.app"))
        assertEquals(setOf("com.new.app"), manager.getWhitelist())
    }

    // ── Activation ──

    @Test
    fun isActivated_returnsFalseByDefault() {
        assertFalse(manager.isActivated())
    }

    @Test
    fun setActivated_makesIsActivatedReturnTrue() {
        manager.setActivated()
        assertTrue(manager.isActivated())
    }

    // ── Tracking ──

    @Test
    fun isTrackingDisabled_returnsFalseByDefault() {
        assertFalse(manager.isTrackingDisabled())
    }

    @Test
    fun setTrackingDisabled_true() {
        manager.setTrackingDisabled(true)
        assertTrue(manager.isTrackingDisabled())
    }

    @Test
    fun setTrackingDisabled_toggles() {
        manager.setTrackingDisabled(true)
        assertTrue(manager.isTrackingDisabled())
        manager.setTrackingDisabled(false)
        assertFalse(manager.isTrackingDisabled())
    }

    // ── Keep Alive ──

    @Test
    fun isKeepAliveEnabled_returnsFalseByDefault() {
        assertFalse(manager.isKeepAliveEnabled())
    }

    @Test
    fun setKeepAliveEnabled_toggles() {
        manager.setKeepAliveEnabled(true)
        assertTrue(manager.isKeepAliveEnabled())
        manager.setKeepAliveEnabled(false)
        assertFalse(manager.isKeepAliveEnabled())
    }

    @Test
    fun getKeepAliveInterval_returns10ByDefault() {
        assertEquals(10, manager.getKeepAliveInterval())
    }

    @Test
    fun setKeepAliveInterval_persistsValue() {
        manager.setKeepAliveInterval(60)
        assertEquals(60, manager.getKeepAliveInterval())
    }

    @Test
    fun setKeepAliveInterval_boundaryValues() {
        manager.setKeepAliveInterval(1)
        assertEquals(1, manager.getKeepAliveInterval())
        manager.setKeepAliveInterval(300)
        assertEquals(300, manager.getKeepAliveInterval())
    }

    // ── Cast Rotation ──

    @Test
    fun getCastRotation_returns0ByDefault() {
        assertEquals(0, manager.getCastRotation())
    }

    @Test
    fun setCastRotation_left() {
        manager.setCastRotation(1)
        assertEquals(1, manager.getCastRotation())
    }

    @Test
    fun setCastRotation_right() {
        manager.setCastRotation(3)
        assertEquals(3, manager.getCastRotation())
    }

    @Test
    fun setCastRotation_none() {
        manager.setCastRotation(1)
        manager.setCastRotation(0)
        assertEquals(0, manager.getCastRotation())
    }

    // ── Cast Keep Alive ──

    @Test
    fun isCastKeepAlive_returnsFalseByDefault() {
        assertFalse(manager.isCastKeepAlive())
    }

    @Test
    fun setCastKeepAlive_toggles() {
        manager.setCastKeepAlive(true)
        assertTrue(manager.isCastKeepAlive())
        manager.setCastKeepAlive(false)
        assertFalse(manager.isCastKeepAlive())
    }

    // ── Persistence across instances ──

    @Test
    fun allSettings_persistAcrossInstances() {
        manager.setActivated()
        manager.setTrackingDisabled(true)
        manager.setKeepAliveEnabled(true)
        manager.setKeepAliveInterval(120)
        manager.setCastRotation(1)
        manager.setCastKeepAlive(true)
        manager.saveWhitelist(setOf("com.example.a", "com.example.b"))

        val manager2 = WhitelistManager(context)
        assertTrue(manager2.isActivated())
        assertTrue(manager2.isTrackingDisabled())
        assertTrue(manager2.isKeepAliveEnabled())
        assertEquals(120, manager2.getKeepAliveInterval())
        assertEquals(1, manager2.getCastRotation())
        assertTrue(manager2.isCastKeepAlive())
        assertEquals(setOf("com.example.a", "com.example.b"), manager2.getWhitelist())
    }
}
