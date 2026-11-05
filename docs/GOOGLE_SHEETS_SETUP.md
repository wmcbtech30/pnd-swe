# Google Sheets connection — litmus_2

## Google Cloud setup

Use an existing Google Cloud project. Enable **Google Drive API** and **Google
Sheets API**. Configure Google Auth Platform branding, audience and Data Access.
During development, keep the audience in Testing and add each tester's Google
account. Workspace administrators can separately restrict third-party apps.

Create a client of type **Android**, with:

- Package: `com.paninda.app`
- SHA-1 of this workspace's current debug APK:
  `E3:CC:05:81:10:48:DA:34:32:9C:33:BE:DA:E4:7F:33:46:21:07:48`

The native AuthorizationClient resolves the Android registration by package and
certificate. This implementation does not need a Web client ID, client secret,
API key, service-account key or `google-services.json`. Never put a client secret
in the APK. A different debug/release/Play signing key needs its own Android
client registration. Verify the actual APK certificate before distributing it:

```sh
apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk
```

Requested scopes:

- `https://www.googleapis.com/auth/drive.file`
- `openid`
- `https://www.googleapis.com/auth/userinfo.email`

`drive.file` confines access to app-authorized files and supports Sheets API
calls on those files. The native list shows workbooks created and marked by
this app, including those shared within its accessible set. This is **not**
a full Google Drive picker. Arbitrary spreadsheet selection/import and Excel
are pending; an Android storage picker URI would not grant Sheets API access.

Google documents standard Drive/Sheets API use as having no additional cost
within applicable quotas. This prototype does not require Play Store publishing.
Public OAuth consent may require additional branding/policy/verification work;
that is separate from installing a debug APK for an added test user.

## Device acceptance checklist

Use a physical Android device with Google Play services and internet:

1. Fresh install: Google button must launch real account/permission authorization.
   Cancelling, denying scopes, unavailable network and missing registration must
   not open Home. Failed setup uses the Figma notice layout. Try Again clears
   the selected account/workbook and starts setup again.
2. Create a workbook: read the blanket notice, Proceed, then inspect Drive.
   A **separate spreadsheet file** named Paninda Workbench contains Meta,
   Vendors, Products, Sales, SaleItems and StockMovements. Each has headers at
   A1 and no sample records. Local records retained from a previous connection
   are uploaded only after the notice explicitly explains this.
3. Create a product offline, restart the app, then Sync Now online. Check SKU,
   integer minor-unit price, vendor and opening-stock movement in Sheets.
4. After a completed sync, edit a valid product value in Sheets while the app
   is not syncing. Sync with no pending local changes; Inventory must update.
5. Edit both copies after a sync: Sync must stop for review and preserve both.
   Check Settings guidance. There is no automatic merge or force-overwrite.
6. Break a required header: sync must fail without importing. Restore it and
   retry. This mandatory data-contract check is separate from the **deferred**
   general-purpose compatibility classifier. The onboarding notice is blanket,
   never a claim that incompatibility was detected.
7. Disconnect: confirm revocation, return to onboarding, verify Google's third-
   party access and that local data/Drive workbook remain. Offline revocation
   must report failure rather than falsely claim disconnection.
8. Reconnect to the saved app workbook. Different nonempty local data must cause
   a conflict; fresh/empty local storage can import the workbook. Local reset
   discards unsynced device records but never uploads deletion of the workbook.

## Sync and recovery limits

Room is the working database and remains available offline after connection.
Sync is explicit via Sync Now/Settings; there is no background worker or polling.
Tokens are obtained on demand from Play services, used in memory, and never
stored in Room or logged. An expired-token 401 clears the cached token; retry
requests authorization again. Account subject IDs bind sync to the selected
Google identity. Access failures preserve local data and pending changes.

The provider-neutral synchronizer compares canonical snapshot hashes to the
last completed shared baseline. It downloads remote-only changes, uploads
local-only changes, acknowledges identical snapshots, and stops on divergence.
Imports and acknowledgements use a Room transaction with a local-hash guard:
edits made during network work cannot be silently discarded/acknowledged.

Writes use one Sheets batch for the six canonical tables, stringValue cells
(no formula execution), and no changes to unrelated worksheets. Original exact
header order is required by the live adapter to avoid reassigning fields while
writing; the pure mapper remains tolerant of reordered input for future import.
Extra data columns are not supported. Core rows may be rewritten/reordered;
manual formatting or formulas in app-owned cells are not preserved as data.

**Single writer only.** Sheets has no compare-and-swap for these mutations.
A second read detects changes before upload and a read-back verifies the result,
but an edit between that check and the write can still be overwritten. Do not
edit in Sheets or on another device during a sync. A coordinator/versioned
write protocol is required before reliable concurrent-device operation.

A timed-out creation/upload may already have reached Google. No automatic
mutation retry or deletion is performed. Review Drive before creating another
workbook; partially initialized workbooks may remain there. A workbook is marked
reconnectable only after its initial write and verification succeed. A lost
response after a normal upload can converge safely on retry when both copies
match. Recovery does not merge divergent rows or recover deleted local-only data.

Revocation applies to the app's Google grant and can affect its other sessions.
Disconnect preserves the last workbook identity/base privately on device so a
later reconnection can compare changes; it does not leave an authenticated local
session open. Backup is disabled to avoid restoring a stale binding on another
device. Clearing app storage/uninstalling is independent of revoking Google access.

## References

- https://developer.android.com/identity/authorization
- https://developers.google.com/workspace/drive/api/guides/api-specific-auth
- https://developers.google.com/workspace/sheets/api/reference/rest/v4/spreadsheets/batchUpdate
- https://developers.google.com/workspace/sheets/api/limits
- https://developers.google.com/workspace/drive/api/guides/limits
