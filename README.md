# VPN Gate Connector
[![pipeline status](https://gitlab.com/hoangrio/vpngate-connector/badges/master/pipeline.svg)](https://gitlab.com/hoangrio/vpngate-connector/commits/master)

## A VPN Gate Client for android

Supports multiple VPN protocols — **SoftEther VPN** (native implementation, no third-party client required), **OpenVPN**, **MS-SSTP**, and **L2TP/IPsec** (Android 12 and below only) — across both free and paid VPN Gate servers.

**Available in Google Play Store**

<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" style="width: 300px;">


Free Version: https://play.google.com/store/apps/details?id=vn.unlimit.vpngate

Pro Version: https://play.google.com/store/apps/details?id=vn.unlimit.vpngatepro

# Protocol Support

| Protocol | Transport | Free Server | Paid Server |
|----------|-----------|:-----------:|:-----------:|
| SoftEther VPN | TCP | ✅ | ✅ |
| SoftEther VPN | UDP | 🚧 Planned | 🚧 Planned |
| OpenVPN | TCP | ✅ | ✅ |
| OpenVPN | UDP | ✅ | ✅ |
| MS-SSTP | TCP | ✅ | ✅ |
| L2TP/IPsec | — | ✅ ⚠️ | ✅ ⚠️ |

### SoftEther VPN
Native SoftEther VPN protocol implementation via the [SoftEther-Android-Module](https://github.com/hoang-rio/SoftEther-Android-Module) submodule (no third-party VPN client required).

**Authentication methods:**

| Method | Free Server | Paid Server |
|--------|:-----------:|:-----------:|
| Anonymous | ✅ | — |
| Hashed Password | ✅ | — |
| Plain Password (RADIUS) | — | ✅ |

- Free servers authenticate as `vpn`/`vpn` against the `vpngate` virtual hub
- Paid servers authenticate with user credentials against the `VPNGatePaid` virtual hub via RADIUS

### OpenVPN
Powered by [OpenVPN for Android](https://github.com/schwabe/ics-openvpn). Supports TCP and UDP transports with automatic or user-selected protocol.

### MS-SSTP
Powered by [Open SSTP Client](https://github.com/kittoku/Open-SSTP-Client). Connects over HTTPS/TLS using the standard Microsoft SSTP protocol with username/password authentication.

### L2TP/IPsec
Uses the Android OS built-in L2TP/IPsec client. Available on both free and paid servers.

> ⚠️ **Deprecated by Android**: Google deprecated the built-in L2TP/IPsec VPN in **Android 12** (API 31) and fully removed it in **Android 13** (API 33). This protocol only works on devices running **Android 12 or below**. For Android 13+, please use SoftEther VPN, OpenVPN, or MS-SSTP instead.

# LICENSE

This project is under GPLv3 LICENSE. It mean if you use this project or a part of this project in your project it must be open source.

This project use another open source project as library detail bellow.
* [**OpenVPN for Android**](https://github.com/schwabe/ics-openvpn) under GPLv2 LICENSE (https://github.com/schwabe/ics-openvpn/blob/master/doc/LICENSE.txt)
* [**glide**](https://github.com/bumptech/glide) under Apache License, Version 2.0 (https://github.com/bumptech/glide/blob/master/LICENSE)
* [**Open SSTP Client for Android**](https://github.com/kittoku/Open-SSTP-Client) under MIT License (https://github.com/kittoku/Open-SSTP-Client/blob/main/LICENSE)
* [**SoftEther-Android-Module**](https://github.com/hoang-rio/SoftEther-Android-Module) under Apache License, Version 2.0 (https://github.com/hoang-rio/SoftEther-Android-Module/blob/main/LICENSE)

Made with ♥ and a lot of coffee by [hoangrio](https://github.com/hoang-rio)