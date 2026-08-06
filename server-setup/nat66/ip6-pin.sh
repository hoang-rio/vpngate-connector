#!/bin/bash
# Live multi-client IPv6 pin learner for tap_net.
#
# The SoftEther hub mangles ND, so the host never learns clients' tun MACs via
# NUD. Instead of one hardcoded pin, this daemon watches every IPv6 frame on
# tap_net and, for each fd00::/8 source address, pins addr -> observed MAC.
# That supports any number of concurrent clients (each with a unique ULA from
# the app) and self-heals Android's per-session MAC rotation.
set -u

IF=tap_net
MAP=/var/lib/nat66/ip6map
STALE_SECS=1800

command -v tcpdump >/dev/null 2>&1 || exit 1
mkdir -p /var/lib/nat66
: > "$MAP"

# Wait for the SoftEther local bridge to create tap_net (up to 120s).
for i in $(seq 1 120); do [ -d "/sys/class/net/$IF" ] && break; sleep 1; done
[ -d "/sys/class/net/$IF" ] || exit 1

HOST_MAC=$(tr 'A-F' 'a-f' < "/sys/class/net/$IF/address")

now_s() { date +%s; }

prune() {
    local now stale
    now=$(now_s); stale=$STALE_SECS
    # Drop pins for addresses not seen within the window.
    awk -v now="$now" -v stale="$stale" '$1 + stale < now' "$MAP" | while read -r _ addr _; do
        ip -6 neigh del "$addr" dev "$IF" 2>/dev/null
    done
    awk -v now="$now" -v stale="$stale" '$1 + stale >= now' "$MAP" > "$MAP.tmp" && mv "$MAP.tmp" "$MAP"
}

while true; do
    tcpdump -ei "$IF" -l -n 'ip6' 2>/dev/null | while IFS= read -r line; do
        srcmac=$(printf '%s\n' "$line" | awk '{print $2}')
        srcip=$(printf '%s\n' "$line" | sed -nE 's/.*length [0-9]*: ([0-9a-fA-F:]*)(\.?[0-9]*) > .*/\1/p')
        case "$srcip" in
            fd00:*)
                srcip=$(printf '%s\n' "$srcip" | tr 'A-F' 'a-f')
                [ "$srcip" = "fd00::1" ] && continue          # host's own gateway
                srcmac=$(printf '%s\n' "$srcmac" | tr 'A-F' 'a-f')
                [ "$srcmac" = "$HOST_MAC" ] && continue       # host's own frames
                ip -6 neigh replace "$srcip" lladdr "$srcmac" dev "$IF" nud permanent
                grep -v " $srcip " "$MAP" > "$MAP.tmp" 2>/dev/null || : > "$MAP.tmp"
                printf '%s %s %s\n' "$(now_s)" "$srcip" "$srcmac" >> "$MAP.tmp"
                mv "$MAP.tmp" "$MAP"
                prune
                ;;
        esac
    done
    # tcpdump exited (e.g. interface flap) — retry shortly.
    sleep 2
done
