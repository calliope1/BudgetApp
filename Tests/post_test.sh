#!/bin/bash

# --- Configuration ---
SECRET="my-very-secret-key"
SERVER="http://127.0.0.1:5000"
JSON='{"amount":3.5,"description":"Coffee","date":"2025-08-28"}'
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
echo "Posting JSON: $JSON"