package vn.unlimit.vpngate.services

import com.google.firebase.messaging.FirebaseMessagingService
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.api.UserApiRequest
import vn.unlimit.vpngate.utils.PaidServerUtil

class PaidServerFirebaseMessageService: FirebaseMessagingService() {
    private val paidServerUtil = App.getInstance().paidServerUtil
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if(paidServerUtil.isLoggedIn()) {
            //Send update fcmPushId to sever
            val userApiRequest = UserApiRequest()
            val sessionId = paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY)
            if (sessionId != null) {
                userApiRequest.addDevice(token, sessionId)
            }
        }
    }
}