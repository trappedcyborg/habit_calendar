C:\src\flutter\flutter\bin\flutter.bat build apk --release
@echo off
setlocal enabledelayedexpansion

:: 1. Extract version number from pubspec.yaml
for /f "tokens=2 delims=: " %%a in ('findstr /b "version:" pubspec.yaml') do (
    for /f "tokens=1 delims=+" %%b in ("%%a") do set APP_VERSION=%%b
)

echo ========================================================
echo Building APK for version: %APP_VERSION%
echo ========================================================

:: 2. Run Flutter build
call C:\src\flutter\flutter\bin\flutter.bat build apk --release

:: 3. Overwrite/Move app-release.apk to versioned name & delete sha1
move /Y "build\app\outputs\flutter-apk\app-release.apk" "build\app\outputs\flutter-apk\GridCalendarView_%APP_VERSION%.apk"
if exist "build\app\outputs\flutter-apk\app-release.apk.sha1" (
    del /f /q "build\app\outputs\flutter-apk\app-release.apk.sha1"
)

echo.
echo ========================================================
echo SUCCESS: Saved as GridCalendarView_%APP_VERSION%.apk
echo ========================================================