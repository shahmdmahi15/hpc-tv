# ========================================================
#  HPCTV - Automated Release APK Build & Deploy Script
# ========================================================
$ErrorActionPreference = "Stop"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  HPCTV - Building Android TV Release APK" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectDir

# 1. Run assembleRelease via Windows Gradle wrapper
Write-Host "`n[1/2] Running ./gradlew assembleRelease..." -ForegroundColor Yellow
.\gradlew.bat assembleRelease

# 2. Copy compiled APK to hpc-web public directory
Write-Host "`n[2/2] Copying release APK to hpc-web/public/tv.apk..." -ForegroundColor Yellow
$ApkSrc = Join-Path $ProjectDir "app\build\outputs\apk\release\app-release.apk"
$WslDest = "\\wsl.localhost\Ubuntu-26.04\home\shahmdmahi\projects\personal\hpc\hpc-web\public\tv.apk"

if (Test-Path $ApkSrc) {
    if (Test-Path "\\wsl.localhost\Ubuntu-26.04\home\shahmdmahi\projects\personal\hpc\hpc-web\public") {
        Copy-Item $ApkSrc -Destination $WslDest -Force
        Write-Host "  [OK] Successfully deployed to hpc-web/public/tv.apk!" -ForegroundColor Green
        Write-Host "  Download URL: https://<server>:3000/tv.apk" -ForegroundColor White
    } else {
        Write-Host "  [Notice] WSL path not reachable, APK is located at: $ApkSrc" -ForegroundColor Yellow
    }
} else {
    Write-Host "  [Error] Could not find built APK at $ApkSrc" -ForegroundColor Red
}

Write-Host "========================================================" -ForegroundColor Cyan
