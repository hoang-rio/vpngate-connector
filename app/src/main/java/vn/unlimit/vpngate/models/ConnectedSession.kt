package vn.unlimit.vpngate.models

open class ConnectedSession {
    class Server {
        var _id: String = ""
        var serverName: String = ""
    }

    class ClientInfo {
        var os: String = ""
        var ip: String = ""
        var hostname: String = ""
    }

    var _id: String = ""
    var username: String = ""
    var clientInfo: ClientInfo? = null
    var sessionId: String = ""
    var serverId: Server? = null
    var clientIp: String = ""
    var transferBytes: Long = 0
    var _created: Long? = null
    var _updated: Long? = null
}