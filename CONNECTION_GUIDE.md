# MOCCA Connection Guide

How to connect the MOCCA Android app to your OpenCode server.

## Prerequisites

- OpenCode CLI installed on your development machine
- MOCCA app installed on your Android device or emulator

## Step 1: Start the OpenCode Server

On your development machine, start the OpenCode server with credentials:

```bash
# Set credentials (optional)
export OPENCODE_SERVER_USERNAME=opencode        # default: "opencode"
export OPENCODE_SERVER_PASSWORD=your_password   # required for remote access

# Start the server
opencode serve --port 4096
```

The server will listen on `127.0.0.1:4096` by default.

### For LAN/Tailscale access

To accept connections from devices on your network, bind to all interfaces:

```bash
opencode serve --port 4096 --hostname 0.0.0.0
```

## Step 2: Configure the App

Open MOCCA → swipe to Dashboard (right panel) → tap **[SETTINGS]**.

### Emulator

No configuration needed. The app auto-detects the Android emulator and connects to the host via `10.0.2.2:4096`.

### LAN (same Wi-Fi network)

1. Find your machine's LAN IP: `ipconfig` (Windows) or `ifconfig` / `ip addr` (macOS/Linux)
2. In Settings, enter:
   - **Host**: Your LAN IP (e.g., `192.168.1.100`)
   - **Port**: `4096`
   - **Username**: Value of `OPENCODE_SERVER_USERNAME` (default: `opencode`)
   - **Password**: Value of `OPENCODE_SERVER_PASSWORD`

### Tailscale

1. Install Tailscale on both your dev machine and Android device
2. In Settings, enter:
   - **Host**: Your Tailscale hostname or IP (e.g., `my-laptop` or `100.x.x.x`)
   - **Port**: `4096`
   - **Username**: Value of `OPENCODE_SERVER_USERNAME`
   - **Password**: Value of `OPENCODE_SERVER_PASSWORD`

## Step 3: Verify Connection

After saving settings, the app will automatically attempt to connect. Check the connection indicator in the top bar:

| Indicator | Meaning |
|-----------|---------|
| Green dot | Connected (Excellent/Good quality) |
| Yellow dot | Connected (Poor quality / high latency) |
| Red dot | Disconnected or Error |
| Gray dot | Not configured |

The app will automatically reconnect if the connection drops.

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "Not Configured" | Go to Settings and enter server details |
| Connection refused | Verify OpenCode server is running: `curl http://<host>:4096/global/health` |
| 401 Unauthorized | Check username/password match `OPENCODE_SERVER_USERNAME` / `OPENCODE_SERVER_PASSWORD` |
| Timeout on LAN | Ensure `--hostname 0.0.0.0` is set when starting the server |
| Timeout on Tailscale | Verify both devices are on the same Tailscale network: `tailscale status` |
| Emulator can't connect | The app uses `10.0.2.2` automatically — ensure OpenCode is listening on `127.0.0.1` or `0.0.0.0` |

## Server Health Check

Test server connectivity from any machine:

```bash
# No auth
curl http://<host>:4096/global/health

# With auth
curl -u opencode:your_password http://<host>:4096/global/health
```

Expected response: `{"healthy":true,"version":"..."}`
