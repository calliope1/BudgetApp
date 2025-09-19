This is a simple server-based budgeting app. The app (`/src/app`) is an Android app that gives everyone that connects to the app access to a shared pool of expenses which are automatically tallied for that week (week starting Monday) and deducted from a weekly budget. Users may add, edit, or delete entries. This is controlled by a Python Flask server (`/src/server`) that uses a shared secret to grant writing permissions to the server.

# App
The app is quite intuitive, but you will need to build it from the Kotlin files to obtain an APK. The apk file found in this repo (`/src/apk/app.apk`) will compile, but it communicates to `http://127.0.0.1:5000` with secret `my-very-secret-key`, so it will only work on a local network server using that secret.

![Budget App main screen, add expense dialogue, and inter-week swiping UI](https://github.com/user-attachments/assets/0f206248-3904-4f11-ab8d-88b89f5b1bde)

There is an [Infinite Tape](https://en.wikipedia.org/wiki/Turing_machine) of weeks (starting Monday) that each total all of the expenses for their respective weeks and subtract that from the weekly budget. Swiping left or right will move to other weeks, and pressing the refresh button/pulling down will refresh the week that you're looking at, as will adding, editing, or deleting expenses. The app also (mostly) supports French (France).

Not shown in the image above is the settings button, which lets you input a custom server url and shared key, so that if you want to use the app and don't feel like re-building the apk yourself (instead, use the pre-constructed one in `src/apk`).

## Building
If you don't want to use the pre-built apk, be sure to create a `local.properties` file with the following:
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


## Vision
I needed a simple budgeting app (essentially [Moneyfy](https://www.moneyfy.com), my preferred budgeting app) that would have a shared pool of expenses with multiple users. However, no such thing existed and so I set out to create one. Initially, this was done extremely quickly to serve a purpose. Now that it's become a bit more of a passion project with customisability, I am releasing it.

# Server

## Secrets
In `server/config/secrets.py` there is a `SHARED_SECRET` variable that must be used to validate API requests. When requesting, include two headers (except `GET`):
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

# Acknowledgements
`pig.xml` is the Pig SVG Vector in the Animal Outlined Sepia Icons collection by author Aslan. It is unedited.

<div align="left">
  <a href="https://www.svgrepo.com/svg/500133/pig">
    <img src="https://www.svgrepo.com/show/500133/pig.svg" alt="App icon, a cartoon pig" width=200 loading="lazy">
  </a>
</div>

`edit_icon.xml` and `delete_icon.xml` are both shared by Ananthanath A X Kalaiism under Public Domain.
