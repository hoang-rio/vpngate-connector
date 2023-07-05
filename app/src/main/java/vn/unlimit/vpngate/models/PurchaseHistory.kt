package vn.unlimit.vpngate.models

import android.content.res.Resources
import de.blinkt.openvpn.core.OpenVPNService
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

// {"_id":"5fb242d9ec65d8001adc222b","currencyPrice":129000,"currency":"VND","userId":"5f7ec06d535b2b001a8bbf06","dataSizeStr":"15GB","dataSize":16106127360,"_created":1605518041422}
class PurchaseHistory {
    var currencyPrice: String = ""
    var currency: String = ""
    var dataSizeStr: String = ""
    var dataSize: Double = 0.0
    private var _created: Long = 0

    fun getDisplayDataSize(res: Resources): String {
        return OpenVPNService.humanReadableByteCount(dataSize.roundToLong(), false, res)
    }

    fun getCreated(): String {
        val dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.DEFAULT,
            DateFormat.SHORT,
            Locale.getDefault()
        )
        return dateFormat.format(Date(_created))
    }
}