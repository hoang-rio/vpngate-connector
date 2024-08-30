package vn.unlimit.vpngate.db

import androidx.room.Database
import androidx.room.RoomDatabase
import vn.unlimit.vpngate.models.VPNGateItem

@Database(entities = [VPNGateItem::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun vpnGateItemDao() : VPNGateItemDao
}