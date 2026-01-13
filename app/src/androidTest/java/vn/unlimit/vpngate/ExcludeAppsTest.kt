package vn.unlimit.vpngate

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.unlimit.vpngate.models.ExcludedApp

@RunWith(AndroidJUnit4::class)
class ExcludeAppsTest {

    private lateinit var app: App
    private lateinit var packageManager: PackageManager

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext() as App
        packageManager = app.packageManager
    }

    @Test
    fun testDatabaseOperations() {
        // Test saving and retrieving excluded apps
        val testApps = listOf(
            ExcludedApp("com.example.test1", "Test App 1"),
            ExcludedApp("com.example.test2", "Test App 2"),
            ExcludedApp("com.android.settings", "Settings")
        )

        // Clear existing data
        app.excludedAppDao.getAllExcludedApps().forEach {
            app.excludedAppDao.deleteExcludedApp(it)
        }

        // Save first 2 test apps (these should be excluded)
        val appsToSave = testApps.take(2)
        appsToSave.forEach { app.excludedAppDao.insertExcludedApp(it) }

        // Verify they were saved
        val savedApps = app.excludedAppDao.getAllExcludedApps()
        assertEquals("Should have 2 saved apps", 2, savedApps.size)

        // Test exclusion check - only saved apps should be excluded
        val isExcluded1 = app.excludedAppDao.isAppExcluded("com.example.test1")
        val isExcluded2 = app.excludedAppDao.isAppExcluded("com.example.test2")
        val isExcluded3 = app.excludedAppDao.isAppExcluded("com.android.settings")

        assertEquals("App should be excluded", 1, isExcluded1)
        assertEquals("App should be excluded", 1, isExcluded2)
        assertEquals("App should not be excluded", 0, isExcluded3)

        // Clean up
        appsToSave.forEach { app.excludedAppDao.deleteExcludedApp(it) }
    }

    @Test
    fun testSystemAppListing() {
        // Test that system apps are included in the installed apps list
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        // Find some known system apps
        val systemApps = installedApps.filter { appInfo ->
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        }

        // Should have at least some system apps
        assertTrue("Should have system apps installed", systemApps.isNotEmpty())

        // Check for some common system apps
        val systemAppPackages = systemApps.map { it.packageName }
        val hasSettings = systemAppPackages.contains("com.android.settings")
        val hasSystemUI = systemAppPackages.any { it.contains("systemui") }

        assertTrue("Should include Settings app", hasSettings || hasSystemUI)
    }

    @Test
    fun testExcludeAppsManagerButtonText() {
        // Test the button text calculation logic directly
        val manager = vn.unlimit.vpngate.utils.ExcludeAppsManager(app)

        // Test initial state (no apps excluded)
        app.excludedAppDao.getAllExcludedApps().forEach {
            app.excludedAppDao.deleteExcludedApp(it)
        }
        val initialCount = manager.getExcludedAppsCount()
        assertEquals("Initial count should be 0", 0, initialCount)

        // Test with some apps excluded
        val testApps = listOf(
            ExcludedApp("com.example.test1", "Test App 1"),
            ExcludedApp("com.example.test2", "Test App 2")
        )
        testApps.forEach { app.excludedAppDao.insertExcludedApp(it) }

        // Test updated button text
        val updatedCount = manager.getExcludedAppsCount()
        assertEquals("Updated count should be 2", 2, updatedCount)

        // Clean up
        testApps.forEach { app.excludedAppDao.deleteExcludedApp(it) }
    }

    @Test
    fun testAppSelectionAdapter() {
        // Test with pre-excluded apps (these should be pre-selected in adapter)
        val preExcludedApps = listOf(
            ExcludedApp("com.example.test1", "Test App 1"),
            ExcludedApp("com.example.test2", "Test App 2")
        )

        val adapter = vn.unlimit.vpngate.adapter.AppSelectionAdapter(preExcludedApps)

        // Should have all pre-excluded apps selected initially
        var selectedApps = adapter.getSelectedApps()
        assertEquals("Should have 2 selected apps initially", 2, selectedApps.size)
        assertTrue("Should include test1", selectedApps.any { it.packageName == "com.example.test1" })
        assertTrue("Should include test2", selectedApps.any { it.packageName == "com.example.test2" })

        // Test with additional apps (mixed excluded and non-excluded)
        val allApps = listOf(
            ExcludedApp("com.example.test1", "Test App 1"), // pre-excluded
            ExcludedApp("com.example.test2", "Test App 2"), // pre-excluded
            ExcludedApp("com.example.test3", "Test App 3"), // not pre-excluded
            ExcludedApp("com.example.test4", "Test App 4")  // not pre-excluded
        )

        adapter.updateApps(allApps)

        // Should still have only the originally pre-excluded apps selected
        selectedApps = adapter.getSelectedApps()
        assertEquals("Should have 2 selected apps after update", 2, selectedApps.size)
        assertTrue("Should include test1", selectedApps.any { it.packageName == "com.example.test1" })
        assertTrue("Should include test2", selectedApps.any { it.packageName == "com.example.test2" })
        assertFalse("Should not include test3", selectedApps.any { it.packageName == "com.example.test3" })
        assertFalse("Should not include test4", selectedApps.any { it.packageName == "com.example.test4" })
    }
}
