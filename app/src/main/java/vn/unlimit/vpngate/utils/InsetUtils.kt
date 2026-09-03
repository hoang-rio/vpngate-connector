package vn.unlimit.vpngate.utils

import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat

object InsetUtils {
    /**
     * Returns the system-bar insets without going through
     * [WindowInsetsCompat.getInsets] with [WindowInsetsCompat.Type.systemBars].
     *
     * On AndroidX >= 1.16, [WindowInsetsCompat.Type.systemBars] includes the
     * SYSTEM_OVERLAYS bit, and on API 34+ devices `TypeImpl34.toPlatformType`
     * maps it to the platform `WindowInsets.Type.systemOverlays()` with no
     * SDK_INT guard and no try/catch. On some devices that platform method
     * fails to resolve and the app throws NoSuchMethodError during inset
     * dispatch ("No static method systemOverlays()"). Reading the raw platform
     * `systemWindowInset*` values (deprecated, available since API 20) avoids
     * that AndroidX code path entirely on every API level.
     */
    fun getSystemBarsInsets(windowInsets: WindowInsetsCompat): Insets {
        val platform = windowInsets.toWindowInsets() ?: return Insets.NONE
        @Suppress("DEPRECATION")
        return Insets.of(
            platform.systemWindowInsetLeft,
            platform.systemWindowInsetTop,
            platform.systemWindowInsetRight,
            platform.systemWindowInsetBottom
        )
    }
}
