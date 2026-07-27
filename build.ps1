# 1. Read version from pubspec.yaml
$pubspec = Get-Content 'pubspec.yaml' -Raw
if ($pubspec -match 'version:\s*([^\+\s\r\n]+)') {
    $ver = $Matches[1]
} else {
    $ver = "1.5.1"
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "[1/3] Building APK for version $ver..." -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 2. Run Flutter build
& "C:\src\flutter\flutter\bin\flutter.bat" build apk --release

# 3. Rename APK and delete sha1
$dir = "build\app\outputs\flutter-apk"
$src = Join-Path $dir "app-release.apk"
$dest = Join-Path $dir "GridCalendarView_$ver.apk"
$sha = Join-Path $dir "app-release.apk.sha1"

if (Test-Path $src) {
    Move-Item -Path $src -Destination $dest -Force
    Write-Host "`n[2/3] SUCCESS: Moved APK to GridCalendarView_$ver.apk" -ForegroundColor Green
} else {
    Write-Host "`n[ERROR] app-release.apk was not found!" -ForegroundColor Red
}

if (Test-Path $sha) {
    Remove-Item -Path $sha -Force
    Write-Host "[3/3] Cleaned up app-release.apk.sha1" -ForegroundColor Green
}