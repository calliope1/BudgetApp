import hmac
import hashlib

from signature.secrets import SHARED_SECRET

#SHARED_SECRET = b'my-very-secret-key'

def verify_signature(body_bytes, signature_hex):
    # compute HMAC-SHA256 and compare hex
    mac = hmac.new(SHARED_SECRET, body_bytes, hashlib.sha256)
    expected = mac.hexdigest()
    return hmac.compare_digest(expected, signature_hex)