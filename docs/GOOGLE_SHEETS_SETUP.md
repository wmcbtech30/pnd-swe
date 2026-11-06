# Google Sheets connection — litmus_3

## Google Cloud setup

Use an existing Google Cloud project. Enable **Google Drive API**, **Google Sheets API**, and
**Google Picker API**. Configure Google Auth Platform branding, audience and Data Access.
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

`drive.file` confines access to files explicitly created or selected for Paninda.
Existing-file selection uses **Google's own Android Picker**, via AuthorizationClient
with PICKER_OAUTH_TRIGGER, CONSENT, drive.file only and
setOptOutIncludingGrantedScopes(true). Multi-selection/folder selection are off;
the MIME filter allows Google Sheets files. Account identity is obtained in a
separate normal authorization request, because the Picker request cannot combine
identity scopes. The selected file is verified as editable using Drive metadata.

No custom file list, WebView, browser API key or broad Drive metadata/Sheets scopes
are used. Enable Google Picker API in this same Cloud project. An Android storage
picker URI is not used as a substitute for a Google Drive file grant.

Google documents standard Drive/Sheets API use as having no additional cost
within applicable quotas. This prototype does not require Play Store publishing.
Public OAuth consent may require additional branding/policy/verification work;
that is separate from installing a debug APK for an added test user.

## Device acceptance checklist

Use a physical Android device with up-to-date Google Play services, a browser and internet:

1. Fresh install: Google button opens a gradient-banner choice: **Choose an
   existing file** or **Create a new file**. Existing selection launches Google's
   Picker; cancellation, denied permissions or network failure never open Home.
2. New file: enter a name, confirm creation and authorize Google. Inspect Drive:
   the file contains `Meta (Paninda)`, `Vendors (Paninda)`, `Products (Paninda)`,
   `Sales (Paninda)`, `SaleItems (Paninda)`, `StockMovements (Paninda)`,
   `StockCounts (Paninda)`, `StockCountItems (Paninda)`, `StockDeliveries (Paninda)`
   and `StockDeliveryItems (Paninda)`.
   Headers start at A1. No sample records. Retained device records are uploaded.
3. After connection, turn off Wi-Fi/mobile data. The current page and local
   records must remain available. Sync Status shows an offline X. Tap Sync Now:
   no Google/onboarding page should open. Create/edit a product locally, restart
   offline and confirm the saved session/data survive. Restore internet and retry.
   Deleting the file or removing access should show Sync failed while keeping
   local work and navigation available.
4. Select an empty existing file: initialize app worksheets inside it without a
   warning. Select a nonempty existing file missing required app worksheets:
   show the Figma warning **before any mutation**. Proceed adds only missing/
   empty app worksheets; other worksheets and their data remain unchanged.
5. Select a compliant file: connect directly without a compatibility warning.
   Invalid existing (Paninda) headers/records fail validation rather than being
   overwritten. This branch uses a fresh schema and does not upgrade older app
   worksheets or rename unsuffixed worksheets. Uninstall for a clean database
   and create a new file or fresh app worksheets when testing.
6. Create a product offline, restart, then Sync Now online. Check SKU, minor-unit
   price, vendor and opening movement. Remote-only valid edits should import;
   divergent local/remote edits must stop with Settings conflict suggestions.
7. Break a required header or remove a required app worksheet: sync must fail
   without importing/clearing local changes. Repair the file and retry.
8. Disconnect: verify actual Google revocation, return to onboarding and confirm
   local data/Drive file remain. Offline revocation must report failure.
9. Reconnect via Google Picker. One selected file ID/account binding is stored
   per device. A local reset discards unsynced device records, never the remote
   file. Empty valid app worksheets can receive retained local records safely;
   different nonempty copies without a common baseline require conflict review.

10. Tools → Vendors: create/edit a vendor offline with optional contacts/notes,
    link a product, rename the vendor and confirm stock/history stay intact.
    Archive it: existing links remain, new product assignments are blocked.
    Restore it and confirm assignments work again. Sync Now and inspect all ten
    `Vendors (Paninda)` columns; on a separate clean install connect the same
    current-schema file to verify contacts, links and archive state import.

11. Tools → Reports: confirm stock totals, selling-price estimate, stock warnings
    and labeled vendor bars match local products, including unsynced edits. Filter
    by stock/vendor/category. Open Stock movements and check recorded quantities,
    date ranges (Manila time) and current-vendor filtering. Restart offline and
    confirm reports still work. Sync valid remote movements and verify reports
    refresh without reopening. Current stock cards are not historical balances.

12. Sell → Record sale: select one or more products, enter quantities and actual
    unit prices, review and save. Check History and on-hand stock; payment must
    display Not recorded. Insufficient stock must block the save. Restart offline
    and confirm persistence. Sync should upload Sales/SaleItems/SALE movements
    without collecting or verifying a payment.
13. Tools → Stock Counts: count selected products, leaving others blank. Enter 0
    explicitly when empty. Add a reason for variances, review and confirm. Check
    expected/counted quantities and history, including a count with no variance.
    A sale or product edit during an open count must block that stale count.
    Sync and inspect the two count worksheets and matching COUNT_ADJUSTMENT
    movements; a clean installation should import the same history and quantities.

14. Tools → Management → Stock Receiving: choose an active vendor, received date,
    recorder and one or more of that vendor's products (including empty stock).
    Enter quantities and optional reference/notes, then review/confirm. Verify
    stock increases, prices remain unchanged, and history/details survive restart.
    The same saved form must not add stock twice. Archived vendors, mismatched
    products, zero/invalid quantities and future dates must not partially save.
15. Sync and inspect StockDeliveries, StockDeliveryItems and matching RECEIVING
    movements. Import on a clean installation and check vendor/name/SKU snapshots.
    A delivery that restores healthy stock should clear/rearm local alerts. An
    open physical count must reject the older baseline after a delivery is saved.
    Test this revised schema with fresh local data and ten app-owned worksheets.

## Sync and recovery limits

Room is the working database and remains available offline after connection.
Sync is explicit via Sync Now/Settings; there is no background worker or polling.
Tokens are obtained on demand from Play services, used in memory, and never
stored in Room or logged. An expired-token 401 clears the cached token; retry
requests authorization again. Account subject IDs bind sync to the selected
Google identity. Live connectivity is observed through Android's default-network callbacks and
validated-internet capabilities, independently of the saved file/session binding.
Offline Sync Now fails before Google authorization, stays inside Sync Status,
and retains local work. Online 403/404/timeouts show an unavailable-file failure;
401 shows authorization recovery. Access failures preserve local data, pending
changes, the file binding and last successful sync time. Reconnecting internet
updates the displayed status but does not start a background upload; tap Sync Now.

The provider-neutral synchronizer compares canonical snapshot hashes to the
last completed shared baseline. It downloads remote-only changes, uploads
local-only changes, acknowledges identical snapshots, and stops on divergence.
Imports and acknowledgements use a Room transaction with a local-hash guard:
edits made during network work cannot be silently discarded/acknowledged.

Writes use one Sheets batch for the ten suffixed app worksheets, stringValue cells
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
workbook; partially initialized workbooks may remain there. A lost
response after a normal upload can converge safely on retry when both copies
match. Recovery does not merge divergent rows or recover deleted local-only data.

Revocation applies to the app's Google grant and can affect its other sessions.
Disconnect preserves the last workbook identity/base privately on device so a
later reconnection can compare changes; it does not leave an authenticated local
session open. Backup is disabled to avoid restoring a stale binding on another
device. Clearing app storage/uninstalling is independent of revoking Google access.

## References

- https://developers.google.com/workspace/drive/picker/guides/desktop-mobile-picker
- https://developer.android.com/identity/authorization
- https://developers.google.com/workspace/drive/api/guides/api-specific-auth
- https://developers.google.com/workspace/sheets/api/reference/rest/v4/spreadsheets/batchUpdate
- https://developers.google.com/workspace/sheets/api/limits
- https://developers.google.com/workspace/drive/api/guides/limits
