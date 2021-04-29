package vn.unlimit.vpngate.models

open class ConnectedSession {
    class Server {
        var _id: String = ""
        var serverName: String = ""
    }

    class ClientInfo {
        var productName: String = ""
        var os: String = ""
        var osVersion: String = ""
        var ip: String = ""
        var hostname: String = ""
    }

    var _id: String = ""
    var username: String = ""
    var sessionId: String = ""
    var serverId: Server? = null
    var clientIp: String = ""
    var connectionName = ""
    var transferBytes: Long = 0
    var _created: Long? = null
    var _updated: Long? = null
}