# Client Info Reporting Implementation Plan

## Overview
Add client information reporting to the SoftEther server via the login PACK during authentication. This helps server admins identify connected clients in session lists.

## Fields to Report

| Field | Value Source | PACK Key |
|-------|-------------|----------|
| Client Product Name | "VPN Gate Connector" (free) / "VPN Gate Connector Pro" (pro) | `ClientProductName` |
| Client Version | `BuildConfig.VERSION_NAME` (e.g., "2.3.2") | `ClientProductVer` |
| Client Build | `BuildConfig.VERSION_CODE` (e.g., 132) | `ClientProductBuild` |
| Client OS Name | `Build.MANUFACTURER` + `Build.MODEL` | `ClientOsName` |
| Client OS Version | `Build.VERSION.RELEASE` (Android version) | `ClientOsVersion` |
| Client OS Product ID | `Build.FINGERPRINT` or `Build.ID` | `ClientOsProductId` |
| Client Host Name | `InetAddress.getLocalHost().getHostName()` | `ClientHostName` |
| Client IP Address | Local interface IP (non-loopback) | `ClientIpAddress` |
| Client Port | Local UDP port (RUDP) or 0 | `ClientPort` |
| Server Host Name | From `ConnectionConfig.serverHost` | `ServerHostName` |
| Server IP Address | Resolved `serverHost` to IP | `ServerIpAddress` |
| Server Port | From `ConnectionConfig.serverPort` | `ServerPort` |

## Implementation Steps

### 1. Add Client Info Data Class (Kotlin)
**File:** `SoftEtherClient/src/main/java/vn/unlimit/softether/model/ClientInfo.kt` (new)

```kotlin
data class ClientInfo(
    val productName: String,          // "VPN Gate Connector" / "VPN Gate Connector Pro"
    val productVersion: String,       // BuildConfig.VERSION_NAME
    val productBuild: Int,            // BuildConfig.VERSION_CODE
    val osName: String,               // Build.MANUFACTURER + " " + Build.MODEL
    val osVersion: String,            // Build.VERSION.RELEASE
    val osProductId: String,          // Build.FINGERPRINT
    val hostName: String,             // Local hostname
    val clientIpAddress: String,      // Local non-loopback IP
    val clientPort: Int,              // Local RUDP port or 0
    val serverHostName: String,       // ConnectionConfig.serverHost
    val serverIpAddress: String,      // Resolved server IP
    val serverPort: Int               // ConnectionConfig.serverPort
)
```

### 2. Add Helper to Build ClientInfo
**File:** `SoftEtherClient/src/main/java/vn/unlimit/softether/controller/ConnectionController.kt`

Add private function `buildClientInfo(config: ConnectionConfig, rudpPort: Int): ClientInfo`

### 3. Update NativeConnectWithHub Signature
**File:** `SoftEtherClient/src/main/cpp/jni/softether_jni.c` and `.h`

Add new JNI function or extend existing one to accept client info:
```c
JNIEXPORT jint JNICALL Java_vn_unlimit_softether_client_SoftEtherClient_nativeConnectWithHub(
    JNIEnv *env, jobject thiz, jlong handle, jstring host, jint port,
    jstring username, jstring password, jstring hubName, jboolean useTcp,
    jstring clientProductName, jstring clientVersion, jint clientBuild,
    jstring clientOsName, jstring clientOsVersion, jstring clientOsProductId,
    jstring clientHostName, jstring clientIpAddress, jint clientPort,
    jstring serverHostName, jstring serverIpAddress, jint serverPort
);
```

### 4. Update Kotlin JNI Declarations
**File:** `SoftEtherClient/src/main/java/vn/unlimit/softether/client/SoftEtherClient.kt`

Add new native method with client info parameters.

### 5. Update ConnectionController.connect()
**File:** `SoftEtherClient/src/main/java/vn/unlimit/softether/controller/ConnectionController.kt`

- Build `ClientInfo` using Android Build constants
- Pass to `nativeConnectWithHub`

### 6. Update C Protocol Layer
**File:** `SoftEtherClient/src/main/cpp/softether-core/src/proto/softether_protocol.h`

Add client info fields to `softether_connection_t` struct.

**File:** `SoftEtherClient/src/main/cpp/softether-core/src/proto/softether_protocol.c`

- Update `softether_connect_with_hub` to accept client info
- Update `build_login_pack` to include all client info fields in PACK
- Add helper functions for hostname/IP resolution

### 7. PACK Field Mapping

| Client Info | PACK Field Name | Type |
|-------------|----------------|------|
| Product Name | `ClientProductName` | String |
| Product Version | `ClientProductVer` | String |
| Product Build | `ClientProductBuild` | Int |
| OS Name | `ClientOsName` | String |
| OS Version | `ClientOsVersion` | String |
| OS Product ID | `ClientOsProductId` | String |
| Host Name | `ClientHostName` | String |
| Client IP | `ClientIpAddress` | String |
| Client Port | `ClientPort` | Int |
| Server Host Name | `ServerHostName` | String |
| Server IP | `ServerIpAddress` | String |
| Server Port | `ServerPort` | Int |

Note: Use exact SoftEther field names for compatibility with server session list display.

## Data Flow

```
ConnectionController.connect()
    │
    ├── Build ClientInfo from Android Build constants
    ├── Resolve server IP from config.serverHost
    ├── Call SoftEtherClient.nativeConnectWithHub(..., clientInfo)
    │
    ├── JNI nativeConnectWithHub()
    │   ├── Convert Java strings to C strings
    │   ├── Call softether_connect_with_hub(conn, ..., client_info)
    │
    ├── softether_connect_with_hub()
    │   ├── Store client_info in conn->client_info
    │   ├── Call perform_authentication_http()
    │   │   ├── Call build_login_pack(conn, ..., conn->rudp, ...)
    │   │   │   ├── Add client info fields to PACK
    │   │   │   └── Return PACK buffer
    │   │   └── Send PACK to server
    └── ...
```

## Testing
1. Connect to SoftEther server
2. Check server session list for reported client info
3. Verify all fields populate correctly for both free/pro flavors
4. Test with various Android versions/devices

## Notes
- PACK field names must match SoftEther's expected format
- String fields should be UTF-8 encoded
- Server expects these exact field names for session list display
- Client IP/Port are informational; actual UDP communication uses RUDP ports
- Free flavor: productName="VPN Gate Connector", versionCode=132, versionName="2.3.2"
- Pro flavor: productName="VPN Gate Connector Pro", versionCode=123, versionName="2.2.3"