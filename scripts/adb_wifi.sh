#!/bin/sh
# Connect adb to a phone over wifi on a fixed port.
#
#   scripts/adb_wifi.sh [PORT]
#
# The phone is assumed to be at PHONE_HOST (default 130) on this machine's /24
# network, e.g. 192.168.0.130. Set PHONE_IP to give a full address instead.
#
# Wireless debugging picks a new port each time it is switched on. The first
# time after the phone reboots, turn on Wireless debugging and pass the port it
# shows: this connects there and switches the phone to listen on a fixed port
# (ADB_WIFI_PORT, default 5555). Until the next reboot, running with no
# argument reconnects on that fixed port.

set -eu

# This machine's own address on the network it uses to reach the outside world.
local_ip() {
    if command -v ip >/dev/null 2>&1; then
        ip -4 route get 1.1.1.1 2>/dev/null | sed -n 's/.* src \([0-9.]*\).*/\1/p'
    else
        # macOS
        iface=$(route -n get default 2>/dev/null | awk '/interface:/ {print $2}')
        [ -n "$iface" ] && ipconfig getifaddr "$iface"
    fi
}

if [ -z "${PHONE_IP:-}" ]; then
    own_ip=$(local_ip || true)
    if [ -z "$own_ip" ]; then
        echo "Couldn't work out the local network; set PHONE_IP to the phone's address." >&2
        exit 1
    fi
    # Assumes a /24 home network: keep the first three parts and swap in the
    # phone's last part.
    PHONE_IP="${own_ip%.*}.${PHONE_HOST:-130}"
fi

fixed_port="${ADB_WIFI_PORT:-5555}"
temp_port="${1:-}"

connect() {
    adb connect "$PHONE_IP:$1" | grep -qE '^(already )?connected'
}

if connect "$fixed_port"; then
    echo "Connected to $PHONE_IP:$fixed_port"
    exit 0
fi

if [ -z "$temp_port" ]; then
    echo "Nothing listening on $PHONE_IP:$fixed_port." >&2
    echo "Turn on Wireless debugging and pass the port it shows, e.g. make adb-wifi PORT=36047" >&2
    echo "If the phone isn't at $PHONE_IP, set PHONE_IP to its address." >&2
    exit 1
fi

if ! connect "$temp_port"; then
    echo "Could not connect to $PHONE_IP:$temp_port" >&2
    echo "If the phone isn't at $PHONE_IP, set PHONE_IP to its address." >&2
    exit 1
fi

adb -s "$PHONE_IP:$temp_port" tcpip "$fixed_port"
sleep 2
# Restarting adbd drops the temporary connection; clear it so it doesn't
# linger as an offline device.
adb disconnect "$PHONE_IP:$temp_port" >/dev/null

if connect "$fixed_port"; then
    echo "Connected to $PHONE_IP:$fixed_port"
else
    echo "Switched the phone to port $fixed_port but could not connect to it" >&2
    exit 1
fi
