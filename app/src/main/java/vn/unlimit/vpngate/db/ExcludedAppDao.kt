package vn.unlimit.vpngate.db

import androidx.room.*
import vn.unlimit.vpngate.models.ExcludedApp

@Dao
interface ExcludedAppDao {
    @Query("SELECT * FROM excluded_apps")
    fun getAllExcludedApps(): List<ExcludedApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExcludedApp(excludedApp: ExcludedApp)

    @Delete
    fun deleteExcludedApp(excludedApp: ExcludedApp)

    @Query("DELETE FROM excluded_apps WHERE packageName = :packageName")
    fun deleteByPackageName(packageName: String)

    @Query("SELECT COUNT(*) FROM excluded_apps WHERE packageName = :packageName")
    fun isAppExcluded(packageName: String): Int
}
