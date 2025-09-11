#!/bin/bash

# --- Configuration ---
#SECRET="my-very-secret-key"
#SERVER="http://127.0.0.1:5000"

SECRET="my-very-secret-key"
SERVER="http://127.0.0.1:5000"
# ----------------------

# --- Configuration ---
JSON='{"amount":110.0,"date":"2025-08-23","description":"TestTwo"}'
ID="942a68e8b344731532281cdc00a7e88f"
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
echo "Patching JSON: $JSON"
echo "Patching ID: $ID"

curl -X PATCH "$SERVER/expenses/id/$ID" \
 -H "Content-Type: application/json" \
 -H "X-Signature: $SIG" \
 -d "$JSON"