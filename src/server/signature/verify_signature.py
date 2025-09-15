import hmac
import hashlib
from config.secrets import SHARED_SECRET

def verify_signature(body_bytes, signature_hex):
    # compute HMAC-SHA256 and compare hex
    mac = hmac.new(SHARED_SECRET, body_bytes, hashlib.sha256)
    expected = mac.hexdigest()
    #print(f"Body: {body_bytes}, Expected: {expected}, Received: {signature_hex}")
    return hmac.compare_digest(expected, signature_hex)