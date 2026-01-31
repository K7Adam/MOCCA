$ips = @('192.168.0.224', '192.168.0.203', '192.168.0.134')
$ports = @(5555, 37000, 38000, 39000, 40000, 41000, 42000, 43000, 44000, 45000)

foreach ($ip in $ips) {
    Write-Host "Scanning $ip..." -ForegroundColor Cyan
    foreach ($port in $ports) {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.ReceiveTimeout = 200
        $tcp.SendTimeout = 200
        try {
            $result = $tcp.BeginConnect($ip, $port, $null, $null)
            if ($result.AsyncWaitHandle.WaitOne(200)) {
                Write-Host "  OPEN: ${ip}:${port}" -ForegroundColor Green
            }
            $tcp.Close()
        } catch {
            # Port closed
        }
    }
}
Write-Host "Scan complete" -ForegroundColor Yellow
