package vn.unlimit.vpngate.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import vn.unlimit.vpngate.models.VPNGateItem

@Dao
interface VPNGateItemDao {
    @Query("SELECT * FROM vpngateitem ORDER BY :orderBy ASC")
    fun getAllASC(orderBy: String = "uid"): List<VPNGateItem>

    @Query("SELECT * FROM vpngateitem ORDER BY :orderBy DESC")
    fun getAllDESC(orderBy: String = "uid"): List<VPNGateItem>

    @Insert
    fun insertAll(vararg vpnGateItem: VPNGateItem)

    @Query("DELETE FROM vpngateitem")
    fun deleteAll()

    @Query("SELECT COUNT(hostName) FROM vpngateitem")
    fun count(): Int
}