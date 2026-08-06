#!/bin/bash
# Idempotently ensure the IPv4+IPv6 NAT / forwarding rules for tap_net -> eth0.
# IPv6: NAT66 MASQUERADE + FORWARD accept. IPv4: same shape (phone already works).
set -euo pipefail

# IPv6 NAT needs the ip6table_nat module; IPv4 needs iptable_nat.
# Neither is always auto-loaded on CentOS, so ensure both up-front.
modprobe iptable_nat 2>/dev/null || true
modprobe ip6table_nat 2>/dev/null || true

# IPv4 forwarding is NOT enabled by default on fresh servers; without it all
# forwarded client traffic is dropped silently (DHCP itself is unaffected).
sysctl -q -w net.ipv4.ip_forward=1

# Policy routing: Tailscale (and some Docker/VPN installs) adds rules such as
# "from all lookup 52" (pref 5270) that run BEFORE the main table. They usually
# hold only connected routes, but they can shadow the main table for non-local
# sources like the tap_net subnet, so VPN clients get "Network is unreachable".
# Force the tap subnet to always resolve via the main table (pref 5200 < 5210).
ip rule show 2>/dev/null | grep -q '^5200:.*from 10.21.0.0/19' || \
  ip rule add from 10.21.0.0/19 lookup main pref 5200 2>/dev/null || true

eth=eth0
tap=tap_net

ensure() {
  # usage: ensure TOOL [-t TABLE] RULE...
  # iptables 1.4.x requires -t BEFORE the command (-C/-A), else "Bad argument".
  local tool="$1"; shift
  local table=()
  if [ "${1:-}" = "-t" ]; then table=("$1" "$2"); shift 2; fi
  if ! "$tool" ${table[@]+"${table[@]}"} -C "$@" 2>/dev/null; then
    "$tool" ${table[@]+"${table[@]}"} -A "$@"
  fi
}

# FORWARD accepts must come BEFORE any pre-existing catch-all DROP/REJECT rule
# (Docker/hardening setups commonly add one). Delete any existing copy and
# insert at the top of the chain so tap_net traffic is accepted first.
ensureFirst() {
  local tool="$1"; shift
  local table=()
  if [ "${1:-}" = "-t" ]; then table=("$1" "$2"); shift 2; fi
  "$tool" ${table[@]+"${table[@]}"} -D "$@" 2>/dev/null || true
  "$tool" ${table[@]+"${table[@]}"} -I "$@" 2>/dev/null || true
}

# Apply to the nft backend, and to the legacy backend too when it exists
# (on RHEL8+/Debian10+ a packet is evaluated against BOTH table sets, so a
# DROP in the legacy tables would silently kill traffic our nft rules allow).
for t in iptables iptables-legacy; do
  command -v "$t" >/dev/null 2>&1 || continue
  ensureFirst "$t" FORWARD -i "$tap" -j ACCEPT
  ensureFirst "$t" FORWARD -o "$tap" -j ACCEPT
  ensureFirst "$t" FORWARD -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT
  ensure "$t" -t nat POSTROUTING -s 10.21.0.0/19 -o "$eth" -j MASQUERADE
done

for t in ip6tables ip6tables-legacy; do
  command -v "$t" >/dev/null 2>&1 || continue
  ensureFirst "$t" FORWARD -i "$tap" -j ACCEPT
  ensureFirst "$t" FORWARD -o "$tap" -j ACCEPT
  ensureFirst "$t" FORWARD -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT
  [ "$("$t" -L FORWARD -n | awk 'NR==1{print $4}')" = DROP ] || "$t" -P FORWARD DROP
  ensure "$t" -t nat POSTROUTING -s fd00::/8 -o "$eth" -j MASQUERADE
done

exit 0
