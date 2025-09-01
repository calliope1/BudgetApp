This is the server files for a very simple budgeting app that I developed. It is designed for highly local distribution with an associated Kotlin app and uses an API to allow updating of budget items. The budget app itself then interprets these and updates the server as required. This is currently in a 'development' state, so one would need to change the secret key and server settings in order to use it as one's own api.

## Secrets
In `Server/signature/verify_signature.py` there is a `SHARED_SECRET` variable that must be used to validate API requests. When requesting, include two headers (except `GET`):
* `Content-Type: application/json`
* `X-Signature: <SIG>`, where `<SIG>` is obtained by combining hashes for `SHARED_SECRET` and the json that you are sending. This server uses Python's `hmac` and `hashlib`:
```Python
sig = hmac.new(SHARED_SECRET, body, hashlib.sha256).hexdigest()
```
The server will compare the supplied signature with its own constructed hex digest.
* Also, of course, include the json as data.

In `Tests` there are sample bash files for the various functions.

# API
Schemata for all json data files are found under `Server/schemata/requests/<suffix>/*.schema.json`.
## /expenses
* `POST`: json must include `"amount"`, `"description"` and `"date"` (in `YYYY-MM-DD` format). If server accepts the signature, a new expense is added to `Server/data/data.json` with those properties and a newly constructed unique id.
* `GET`: No headers required, the full json `Server/data/data.json` is returned.
### /expenses/<expense_id>
* `PATCH`: Same json as `POST /expenses`. If an item with id `<expense_id>` exists then the first item matching that id will be updated with the data supplied. Must supply all data (amount, description and date), even if not changing everything.
* `DELETE`: json must contain `"id"`, which matches `<expense_id>`. If an entry with that id exists then it will delete the first entry that matches that id.
## /budget
* `GET`: No headers required, the full json `Server/data/budget.json` is returned.
* `PUT`: json must contain `"weekly_budget"`. If so, `Server/data/budget.json` is replaced by `{"weekly_budget": <weekly_budget>}`.
