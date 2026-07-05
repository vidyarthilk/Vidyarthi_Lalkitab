@echo off
cd /d "%~dp0"
echo.
echo === Vidyarthi Lalkitab - Keystore setup (Point A) ===
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\setup-keystore.ps1"
if errorlevel 1 (
    echo.
    echo FAILED - read the error above. Window stays open.
    pause
    exit /b 1
)
echo.
echo Done. Check folder: release\vidyarthi-lalkitab.jks
pause
