#!/bin/bash

echo "========================================"
echo "Build & Publish (Linux)"
echo "========================================"
echo

KEYSTORE_PATH="$HOME/keystore/aviumnotes-release-key.jks"
KEYSTORE_PASSWORD="gusti717"
KEY_ALIAS="aviumkey"
KEY_PASSWORD="gusti717"

if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "ERROR: Keystore not found!"
    exit 1
fi

echo "[1/1] Building and publishing to Telegram..."
./gradlew buildAndPublish \
    -PmyKeystorePath="$KEYSTORE_PATH" \
    -PmyKeystorePassword="$KEYSTORE_PASSWORD" \
    -PmyKeyAlias="$KEY_ALIAS" \
    -PmyKeyPassword="$KEY_PASSWORD"

if [ $? -ne 0 ]; then
    echo
    echo "ERROR: Build & publish failed!"
    exit 1
fi

echo
echo "Build & publish completed successfully!"
read -p "Press Enter to continue..."
