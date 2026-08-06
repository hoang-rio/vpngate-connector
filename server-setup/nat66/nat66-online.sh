#!/bin/bash
# Re-apply tap_net config whenever the TAP device is (re)created.
# The SoftEther local bridge deletes+recreates tap_net when vpnserver is
# (re)started, wiping the addresses that nat66-restore.service applied at boot.
# udev calls this on every "add" of tap_net so DHCP/NAT keep working.
set -euo pipefail

[ -d /sys/class/net/tap_net ] || exit 0

ip link set dev tap_net up
ip -4 addr replace 10.21.0.1/19 dev tap_net
ip -6 addr replace fd00::1/64 dev tap_net nodad
sysctl -q -w net.ipv4.ip_forward=1
/etc/nat66/nat66-rules.sh
systemctl --no-block restart dnsmasq ndppd radvd 2>/dev/null || true

exit 0
