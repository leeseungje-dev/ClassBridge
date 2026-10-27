#!/bin/sh
set -eu

# Restrict access to files created by this script
umask 077

# Restore the wallet from the secret file
mkdir -p "$CLASSBRIDGE_WALLET_PATH"

base64 --decode /etc/secrets/wallet.zip.b64 > /tmp/wallet.zip

unzip -oq /tmp/wallet.zip -d "$CLASSBRIDGE_WALLET_PATH"

rm /tmp/wallet.zip

# Start the Spring Boot application
exec java -jar /app/app.jar