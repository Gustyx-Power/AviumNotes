@echo off
setlocal enabledelayedexpansion

echo ========================================
echo AviumNotes Build ^& Publish (Windows)
echo ========================================
echo.

set "KEYSTORE_PATH=C:\Users\putri\Documents\Project\XMS\Keystore\Keystore-AviumNotes\aviumnotes-release-key"
set "KEYSTORE_PASSWORD=gusti717"
set "KEY_ALIAS=aviumnoteskey"
set "KEY_PASSWORD=gusti717"

echo Checking keystore...
echo Path: %KEYSTORE_PATH%

if not exist "%KEYSTORE_PATH%" (
    echo.
    echo ERROR: Keystore not found at:
    echo %KEYSTORE_PATH%
    echo.
    echo Please verify the path exists.
    pause
    exit /b 1
)

echo Keystore found!
echo.

echo [1/1] Building and publishing to Telegram...
echo This may take a few minutes...
echo.

call gradlew.bat buildAndPublish -PmyKeystorePath=%KEYSTORE_PATH% -PmyKeystorePassword=%KEYSTORE_PASSWORD% -PmyKeyAlias=%KEY_ALIAS% -PmyKeyPassword=%KEY_PASSWORD%

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Build ^& publish failed!
    echo Check the logs above for details.
    pause
    exit /b 1
)

echo.
echo ============================================
echo Build ^& publish completed successfully!
echo ============================================
echo.
echo APK uploaded to Telegram
echo Local copy: dist\AviumNotes-1.0-beta.apk
echo.

pause
