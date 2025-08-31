#!/bin/bash

# --- Configuration ---
SECRET="my-very-secret-key"
SERVER="http://127.0.0.1:5000"
# ----------------------

# --- Configuration ---
ID="17742899d9971401b5a211949c22c54c"
JSON='{"weekly_budget":0.0}'
# ----------------------

# Compute HMAC-SHA256 signature with Python
SIG=$(py - <<PY
import hmac, hashlib
secret = b"$SECRET"
body = b'''$JSON'''
sig = hmac.new(secret, body, hashlib.sha256).hexdigest()
print(sig)
PY
)

echo "Using signature: $SIG"
echo "Patching budget with JSON: $JSON"

curl -X PUT "$SERVER/budget" \
 -H "Content-Type: application/json" \
 -H "X-Signature: $SIG" \
 -d "$JSON"

