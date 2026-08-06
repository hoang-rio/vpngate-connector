#!/bin/bash
# Idempotently ensure the IPv4+IPv6 NAT / forwarding rules for tap_net -> eth0.
# IPv6: NAT66 MASQUERADE + FORWARD accept. IPv4: same shape (phone already works).
set -euo pipefail

# IPv6 NAT needs the ip6table_nat module; IPv4 needs iptable_nat.
# Neither is always auto-loaded on CentOS, so ensure both up-front.
modprobe iptable_nat 2>/dev/null || true
modprobe ip6table_nat 2>/dev/null || true

eth=eth0
tap=tap_net

ensure6() {
  # usage: ensure6 [-t TABLE] RULE...
  # iptables 1.4.x requires -t BEFORE the command (-C/-A), else "Bad argument".
  local table=()
  if [ "${1:-}" = "-t" ]; then table=("$1" "$2"); shift 2; fi
  if ! ip6tables ${table[@]+"${table[@]}"} -C "$@" 2>/dev/null; then
    ip6tables ${table[@]+"${table[@]}"} -A "$@"
  fi
}
ensure4() {
  local table=()
  if [ "${1:-}" = "-t" ]; then table=("$1" "$2"); shift 2; fi
  if ! iptables ${table[@]+"${table[@]}"} -C "$@" 2>/dev/null; then
    iptables ${table[@]+"${table[@]}"} -A "$@"
  fi
}

# --- IPv4 FORWARD + MASQUERADE (must apply even if IPv6 NAT is unavailable) ---
ensure4 FORWARD -i "$tap" -j ACCEPT
ensure4 FORWARD -o "$tap" -j ACCEPT
ensure4 FORWARD -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT
ensure4 -t nat POSTROUTING -s 10.21.0.0/19 -o "$eth" -j MASQUERADE

# --- IPv6 FORWARD + NAT66 ---
ensure6 FORWARD -i "$tap" -j ACCEPT
ensure6 FORWARD -o "$tap" -j ACCEPT
ensure6 FORWARD -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT
[ "$(ip6tables -L FORWARD -n | awk 'NR==1{print $4}')" = DROP ] || ip6tables -P FORWARD DROP
ensure6 -t nat POSTROUTING -s fd00::/8 -o "$eth" -j MASQUERADE

exit 0
