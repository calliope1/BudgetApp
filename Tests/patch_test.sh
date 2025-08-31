#!/bin/bash

# --- Configuration ---
SECRET="my-very-secret-key"
SERVER="http://127.0.0.1:5000"
# ----------------------

# --- Configuration ---
JSON='{"amount":9.9,"description":"Coffee","date":"2025-08-28"}'
ID="ba4abebc700f51a33a5db1f0a951578a"
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

curl -X PATCH "$SERVER/expenses/$ID" \
 -H "Content-Type: application/json" \
 -H "X-Signature: $SIG" \
 -d "$JSON"