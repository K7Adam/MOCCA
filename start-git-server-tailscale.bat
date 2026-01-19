@echo off
REM Wrapper script to start git server with proper Windows path handling

echo Starting Git Server with Tailscale-compatible config...
echo Host: 127.0.0.1
echo Port: 4097

cd /d "%USERPROFILE%\.opencode"
node git-server.js "%USERPROFILE%\AndroidStudioProjects\MOCCA"

echo.
echo Git Server started. Keep this window open.
echo Close this window to stop the server.
pause
