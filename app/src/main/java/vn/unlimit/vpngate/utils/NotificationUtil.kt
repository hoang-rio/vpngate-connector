package vn.unlimit.vpngate.utils

import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity

class NotificationUtil(appCompatActivity: AppCompatActivity) {
    private var mAppCompatActivity: AppCompatActivity = appCompatActivity

    companion object {
        private const val REQUEST_PERMISSION_CODE = 1010
    }

    fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && this.mAppCompatActivity.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            this.mAppCompatActivity.requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_PERMISSION_CODE
            )
        }
    }
}