This is a simple server-based budgeting app. The app (`/src/app`) is an Android app that gives everyone that connects to the app access to a shared pool of expenses which are automatically tallied for that week (week starting Monday) and deducted from a weekly budget. Users may add, edit, or delete entries. This is controlled by a Python Flask server (`/src/server`) that uses a shared secret to grant writing permissions to the server.

# App
The app is quite intuitive, but you will need to build it from the Kotlin files to obtain an APK.

<div align="center">
  <img src="https://cr638.user.srcf.net/files/budgetapp/budget_app_bordered.jpg" alt="Budget App main screen" width=300>
</div>

## Building
Be sure to create a `local.properties` file with the following:
```properties
SERVER_URL = "https://sever-url"
SHARED_SECRET = "shared-secret"
```
replacing the server url and shared secret as appropriate. OR in `ExpenseViewModel.kt`, replace
```Kotlin
private val SERVER_URL = BuildConfig.SERVER_URL
private val SHARED_SECRET = BuildConfig.SHARED_SECRET
```
with
```Kotlin
private val SERVER_URL = "https://server-url"
private val SHARED_SECRET = "shared-secret"
```
These should both correspond to the server, which needs to be running for the app to work. Then build the APK and run it.

This app was built extremely quickly with heavy help from AI; I think I have spent about eight hours total on it, most of which was spent getting to grips with the Android Studio phone emulator. I wanted something very simple (essentially [Moneyfy](https://www.moneyfy.com), my preferred budgeting app) that would have a shared pool of expenses. However, no such thing existed and so I set out to create one. Now that it functions well enough, I am releasing it. That being said, anyone that wants to can add extra features that they may feel are missing, such as:
* Additional languages
* Editing server url
* Editing shared secret
These shouldn't be hard to implement, but I'm fairly happy with where the project is at for the moment, so I won't be adding these features any time soon. The only major update that I would consider is editing the server and app so that expenses are loaded per day, and then making it so that the app only loads expenses within a given week (swapping to different weeks by swiping left/right). In particular, the former would allow me to distribute a pre-compiled APK.

# Server

## Secrets
In `Server/signature/secrets.py` there is a `SHARED_SECRET` variable that must be used to validate API requests. When requesting, include two headers (except `GET`):
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
