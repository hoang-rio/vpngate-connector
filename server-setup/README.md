# IPv6 (NAT66) + IPv4 persistence for the SoftEther hub's tap_net

Target host: the paid SoftEther VPS (`YOUR_VPS`), hub `VPNGatePaid`,
eth0 = uplink with the single global IPv6, tap_net = local bridge TAP.

**Problem this solves:** the SoftEther hub mangles ND, so the host can never
learn clients' tun MACs by NUD (entries stay FAILED/INCOMPLETE even though
the clients answer NAs). We bypass it by learning each client's `fd00::/8`
address → MAC pair live off the wire and pinning it permanently.

## What was verified working (2026-08-05)

- Host → phone: `ping6 -c3 fd00::2` round-trips (static neighbor pinned to
  the phone's live MAC, e.g. `5e:e1:e5:36:26:05`).
- Phone → internet (IPv6): echo request `fd00::2 → 2001:4860:4860::8888`
  leaves tap_net, is MASQUERADE'd on eth0, reply returns and is delivered.
- IPv4: DHCP via dnsmasq (`10.21.0.2-200`, gateway `10.21.0.1`) + NAT to eth0 works.
  Host tap_net owns `10.21.0.1` (nat66-restore.service sets it at boot). dnsmasq
  refuses to serve DHCP on an address-less interface, so the host MUST have
  this address or clients get no lease ("DHCP packet received on tap_net
  which has no address").
- NAT modules are not auto-loaded on CentOS: `iptable_nat` + `ip6table_nat` are
  modprobe'd by nat66-rules.sh and pinned in `/etc/modules-load.d/ip6-nat.conf`.
  If `ip6tables -t nat` says "Bad argument 'nat'", the module is missing.
- The hub DOES deliver host→phone unicast fine; the ONLY host-side blocker
  was NUD, which the live pin removes.

### Multi-client (2026-08-06)

The app now derives a unique stable ULA per install (`fd00::<hex from ANDROID_ID>`,
fallback random persisted). The server no longer pins one address — ip6-pin.sh
watches every `fd00::/8` source on tap_net and pins each addr → its observed MAC,
so concurrent clients each get their own permanent neighbor and replies flow to
all of them.

## Files

| File | Purpose |
| --- | --- |
| `setup.sh` | idempotent installer (root on the VPS; interactive or env-var driven) |
| `nat66-rules.sh` | ensures IPv4+IPv6 FORWARD/NAT rules |
| `ip6-pin.sh` | live multi-client `fd00::/8` → MAC pin learner |
| `nat66-restore.service` | boot: wait for tap_net, set addr/sysctl/rules |
| `ip6-pin.service` | keeps the pin learner alive (`Restart=always`) |
| `sysctl-ipv6.conf` | forwarding + `accept_ra=0` drop-in |
| `ndppd.conf` | answers global NS from the phone (`::/0 static`) |
| `radvd.conf` | RA with NO prefix, `AdvDefaultLifetime 0` |
| `dnsmasq.conf` | IPv4 DHCP on tap_net |

## Deploy

Set the host once, then reuse it:

```sh
VPS=YOUR_VPS                          # e.g. VPS=203.0.113.10
scp -r server-setup/nat66 root@$VPS:/root/
ssh root@$VPS
bash /root/nat66/setup.sh        # interactive: prompts for values
```

**Unattended** (another server / CI): supply env vars, no prompts needed.

```sh
VPN_SERVER_PW='<server admin pw>' VPN_HOST=localhost VPN_PORT=443 \
HUB=VPNGatePaid EXT_IF=eth0 bash /root/nat66/setup.sh
```

`setup.sh` does: install packages (if missing), create the hub ↔ tap_net
bridge when absent, disable SecureNAT (dnsmasq is the only DHCP server),
install + enable nat66-restore / ip6-pin / dnsmasq / ndppd / radvd.

### Prerequisites on the VPS

- SoftEther local bridge active: `vpncmd localhost /SERVER /CMD BridgeList`
  → tap_net exists (created by the bridge, survives as long as the bridge does;
  if your SoftEther setup does not recreate the bridge across reboots, ensure
  that first).
- dnsmasq, radvd, ndppd, tcpdump, ip6tables installed:
  `yum install dnsmasq radvd ndppd tcpdump ip6tables` (CentOS 7; use `dnf` on 8+).
- Single global IPv6 on eth0; IPv6 forwarding default route present.

## Operational notes

- Do NOT restart ndppd with the old `fd00::/8` + `fe80::/10` relay rules: those
  poison host ND. Only the `::/0 { static }` rule is used.
- ip6-pin.sh re-pins on every observed frame, so Android's per-session tun
  MAC rotation is self-healing. Pins idle for 30 min are pruned. Only the
  host's own gateway (`fd00::1`) and host frames are ignored.
- radvd advertises no prefix and zero default lifetime, which keeps Android
  from SLAAC-rotating addresses; the app supplies the `fd00::/8` ULA + the
  `::/0` route.
- Docker caveat: nat66-rules.sh sets the IPv6 FORWARD policy to DROP and adds
  tap_net accept rules without touching the existing DOCKER chains. If you
  run Docker with IPv6, review `ip6tables -L FORWARD -n` after deploy.
- ip6-pin.sh needs tcpdump (`yum install tcpdump`). Tracks learned pins in
  `/var/lib/nat66/ip6map`; raw pins live in the kernel as `nud permanent`.
- To watch it live: `journalctl -u ip6-pin -f` (or `ip -6 neigh show dev tap_net`).

## Manual one-time bits NOT covered by setup.sh

The SoftEther local bridge (`BridgeCreate VPNGatePaid tap_net`) and the hub
config live in the SoftEther server config; confirm `BridgeList` shows the
bridge after a reboot.
