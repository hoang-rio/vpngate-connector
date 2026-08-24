#!/bin/bash
# One-shot full host setup for the vpngate paid SoftEther server.
#   1. Install required packages (dnsmasq, radvd, ndppd, tcpdump + EPEL)
#   2. Create the hub <-> tap_net local bridge (via vpncmd)
#   3. Check & disable SecureNAT (dnsmasq must be the only DHCP server)
#   4. Install NAT66 host config: tap_net addresses, sysctl, iptables rules, units
#   5. Apply addresses/rules and enable dnsmasq/ndppd/radvd/ip6-pin at boot
# Prompts for: VPN host:port, Hub name, Server admin password, external iface.
# Non-interactive runs (stdin is not a TTY) take values from env vars instead:
#   VPN_HOST, VPN_PORT, HUB, EXT_IF, VPN_SERVER_PW (defaults apply when unset).
# Idempotent: safe to re-run. Requires root.
set -euo pipefail

SRC="$(cd "$(dirname "$0")" && pwd)"
[ "$(id -u)" -eq 0 ] || { echo "Run as root." >&2; exit 1; }

# ---- defaults ---------------------------------------------------------------
VPN_BIN="${VPN_BIN:-/usr/local/vpnserver/vpncmd}"
VPN_HOST="${VPN_HOST:-localhost}"
VPN_PORT="${VPN_PORT:-443}"
HUB="${HUB:-VPNGatePaid}"
EXT_IF="${EXT_IF:-eth0}"
IP4_NET="10.21.0.0/19"
IP6_NET="fd00::/8"
IP4_ADDR="10.21.0.1/19"
IP6_ADDR="fd00::1/64"
SERVER_PW="${VPN_SERVER_PW:-}"

# ---- prompts (skipped when run non-interactively) ---------------------------------
# Interactive runs prompt for each value; automated runs supply them via env vars
# (VPN_HOST, VPN_PORT, HUB, EXT_IF, VPN_SERVER_PW) and fall back to defaults.
if [ -t 0 ]; then
  read -p "VPN server host:port [${VPN_HOST}:${VPN_PORT}]: " _in
  if [ -n "$_in" ]; then
    case "$_in" in
      *:*) VPN_HOST="${_in%%:*}"; VPN_PORT="${_in##*:}" ;;
      *)   VPN_HOST="$_in" ;;
    esac
  fi
  read -p "Virtual Hub name [${HUB}]: " _in; HUB="${_in:-$HUB}"
  read -s -p "Server admin password: " SERVER_PW; echo
  read -p "External (internet) interface [${EXT_IF}]: " _in; EXT_IF="${_in:-$EXT_IF}"
fi
[ -n "$SERVER_PW" ] || { echo "Password is required (set VPN_SERVER_PW for unattended runs)." >&2; exit 1; }

[ -x "$VPN_BIN" ] || { echo "vpncmd not found at $VPN_BIN. Install SoftEther VPN server first." >&2; exit 1; }

# ---- helpers -----------------------------------------------------------------
# Run vpncmd as server admin, feeding console lines on stdin. The console
# supports "Hub <name>" so hub commands need no hub password when the caller
# has server-admin rights.
vpn_lines() {
  { for c in "$@"; do echo "$c"; done; echo quit; } \
    | timeout 30 "$VPN_BIN" "$VPN_HOST:$VPN_PORT" /SERVER /PASSWORD:"$SERVER_PW" 2>&1
}

# ---- [1/6] packages -----------------------------------------------------------
echo "==> [1/6] Installing required packages"
_missing=""
for _p in dnsmasq radvd ndppd tcpdump; do
  rpm -q "$_p" >/dev/null 2>&1 || _missing="$_missing $_p"
done
if [ -n "$_missing" ]; then
  yum install -y epel-release >/dev/null 2>&1 || true
  yum install -y $_missing
else
  echo "  all required packages already present"
fi

# ---- [2/6] hub <-> tap_net bridge ----------------------------------------------
echo "==> [2/6] Creating local bridge $HUB <-> tap_net"
out=$(vpn_lines BridgeList)
if echo "$out" | awk -F'|' -v h="$HUB" 'BEGIN{f=0} /^[ ]*[0-9]+/ {gsub(/ /,"",$2); if ($2==h) f=1} END{exit f?0:1}'; then
  echo "  bridge for '$HUB' already exists"
else
  vpn_lines "BridgeCreate $HUB /DEVICE:net /TAP:yes" | tail -n 3
fi

# ---- [3/6] SecureNAT check + disable --------------------------------------------
echo "==> [3/6] Checking SecureNAT on $HUB"
sn=$(vpn_lines "Hub $HUB" SecureNatStatusGet | grep "Kernel-mode NAT is Active" | sed 's/.*| *//' || true)
echo "  Kernel-mode NAT is Active: ${sn:-unknown}"
if [ "${sn:-No}" = "Yes" ]; then
  vpn_lines "Hub $HUB" SecureNatDisable | tail -n 2
  echo "  SecureNAT disabled (dnsmasq is the DHCP server)."
else
  echo "  SecureNAT already disabled."
fi

# ---- [4/6] install host configs --------------------------------------------------
echo "==> [4/6] Installing host configs"
mkdir -p /etc/nat66
install -m 0755 "$SRC/nat66-rules.sh" /etc/nat66/nat66-rules.sh
sed -i "s/^eth=eth0/eth=$EXT_IF/" /etc/nat66/nat66-rules.sh
install -m 0755 "$SRC/ip6-pin.sh"     /etc/nat66/ip6-pin.sh
install -m 0755 "$SRC/nat66-online.sh" /etc/nat66/nat66-online.sh
install -m 0644 "$SRC/90-nat66-tap.rules" /etc/udev/rules.d/90-nat66-tap.rules
install -m 0644 "$SRC/sysctl-ipv4.conf" /etc/sysctl.d/10-ipv4-forward.conf
install -m 0644 "$SRC/sysctl-ipv6.conf" /etc/sysctl.d/10-ipv6-forward.conf
install -m 0644 "$SRC/ip6-nat.conf"   /etc/modules-load.d/ip6-nat.conf
install -m 0644 "$SRC/ndppd.conf"     /etc/ndppd.conf
install -m 0644 "$SRC/radvd.conf"     /etc/radvd.conf
install -m 0644 "$SRC/dnsmasq.conf"   /etc/dnsmasq.conf
install -m 0644 "$SRC/nat66-restore.service" /etc/systemd/system/nat66-restore.service
install -m 0644 "$SRC/ip6-pin.service"       /etc/systemd/system/ip6-pin.service
sysctl --system >/dev/null

# ndppd/radvd/dnsmasq all need tap_net (with its addresses) before they start;
# nat66-restore.service waits for the SoftEther bridge then configures it.
for u in ndppd radvd dnsmasq; do
  mkdir -p "/etc/systemd/system/$u.service.d"
  printf '[Unit]\nAfter=nat66-restore.service\n' > "/etc/systemd/system/$u.service.d/order.conf"
done
systemctl daemon-reload

# tap_net is deleted+recreated whenever vpnserver restarts, wiping its
# addresses; udev re-applies them on every appearance of the interface.
udevadm control --reload
udevadm trigger --subsystem-match=net
udevadm settle

# ---- [5/6] apply addresses + NAT rules ----------------------------------------------
echo "==> [5/6] Applying tap_net addresses + NAT rules"
echo "  waiting for tap_net..."
i=0
until [ -d /sys/class/net/tap_net ]; do
  i=$((i+1))
  [ $i -ge 60 ] && { echo "  ERROR: tap_net never appeared. Is vpnserver running and the bridge created?" >&2; exit 1; }
  sleep 1
done
ip link set dev tap_net up
sysctl -q -w net.ipv6.conf.tap_net.forwarding=1 net.ipv6.conf.tap_net.accept_ra=0
ip -4 addr replace "$IP4_ADDR" dev tap_net
ip -6 addr replace "$IP6_ADDR" dev tap_net nodad
/etc/nat66/nat66-rules.sh
systemctl enable nat66-restore.service ip6-pin.service
systemctl restart nat66-restore.service ip6-pin.service

# ---- [6/6] dnsmasq / ndppd / radvd ---------------------------------------------------
echo "==> [6/6] Enabling + restarting dnsmasq / ndppd / radvd"
systemctl enable ndppd radvd dnsmasq
systemctl restart ndppd radvd dnsmasq

echo
v4=$(ip -4 addr show dev tap_net | awk '/inet /{print $2}')
v6=$(ip -6 addr show dev tap_net | awk '/inet6 fd00/{print $2}')
if [ -z "$v4" ] || [ -z "$v6" ]; then
  echo "ERROR: tap_net has no address (IPv4='$v4' IPv6='$v6'). Check: journalctl -u nat66-restore -u dnsmasq" >&2
  exit 1
fi
echo "Done. Verification:"
echo "  IPv4 tap_net : $v4"
echo "  IPv6 tap_net : $v6"
systemctl is-enabled nat66-restore ip6-pin ndppd radvd dnsmasq
iptables -t nat -S POSTROUTING  | grep -E "$IP4_NET" || true
ip6tables -t nat -S POSTROUTING | grep -E "$IP6_NET" || true
ip rule show | grep '^5200:.*from 10.21.0.0/19' || true
