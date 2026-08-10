package vn.unlimit.vpngate.utils

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Per-install stable IPv6 ULA (fd00::/8) shared by every VPN protocol
 * (SoftEther, OpenVPN, SSTP) so a device always sources the same address
 * through the server's NAT66. Mirrors SoftEtherVpnService.deriveUniqueLocalAddressV6
 * (same ANDROID_ID derivation, same persisted fallback) so all three protocols
 * end up with the identical address on a given device.
 */
object Ipv6Ula {
    private const val SOFTETHER_PREFS = "softether_vpn"
    private const val KEY_ULA_V6 = "ula_v6"
    private const val FALLBACK_PREFS = "vpngate_ipv6"

    fun getOrDerive(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }
        if (!androidId.isNullOrBlank()) {
            val hex = androidId.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.lowercase()
            if (hex.length >= 4) {
                return buildUla(hex)
            }
        }

        val softetherPrefs = context.getSharedPreferences(SOFTETHER_PREFS, Context.MODE_PRIVATE)
        softetherPrefs.getString(KEY_ULA_V6, null)?.let { return it }

        val prefs = context.getSharedPreferences(FALLBACK_PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_ULA_V6, null)?.let { return it }

        val generated = buildUla(UUID.randomUUID().toString().replace("-", "").take(16))
        prefs.edit().putString(KEY_ULA_V6, generated).apply()
        return generated
    }

    private fun buildUla(hex: String): String {
        val groups = hex.chunked(4).joinToString(":")
        return "fd00::$groups"
    }
}
