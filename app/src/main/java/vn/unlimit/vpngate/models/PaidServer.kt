package vn.unlimit.vpngate.models

class PaidServer {
    var l2tpSupport: Int = 1
    var serverCountryCode: String = ""
    var isCommunity: Boolean = false
    var public: Int = 1
    var serverName: String = ""
    var serverLocation = "Singapore"
    var serverDomain = ""
    var serverIp = ""
    var tcpPort = 0
    var udpPort = 0
    var maxSession = 0
    var sessionCount = 0
    var ovpnContent = ""
    var serverStatus = ""
}