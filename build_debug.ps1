# Build debug APK
# No JAVA_HOME override needed if JDK 17+ is configured in gradle.properties or system PATH

.\gradlew.bat :androidApp:assembleDebug

Write-Host ""
Write-Host "Build complete! APK at: androidApp\build\outputs\apk\debug\androidApp-debug.apk"
Write-Host "Install with: adb install androidApp\build\outputs\apk\debug\androidApp-debug.apk"
