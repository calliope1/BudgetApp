#!/bin/bash

# --- Configuration ---
SECRET="my-very-secret-key"
SERVER="http://127.0.0.1:5000"
# ----------------------

# --- Configuration ---
ID="942a68e8b344731532281cdc00a7e88f"
JSON='{"id":"942a68e8b344731532281cdc00a7e88f"}'
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
echo "Deleting with JSON: $JSON"
echo "Deleting ID: $ID"

curl -X DELETE "$SERVER/expenses/id/$ID" \
 -H "Content-Type: application/json" \
 -H "X-Signature: $SIG" \
 -d "$JSON"
