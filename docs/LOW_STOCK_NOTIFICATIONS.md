# Local stock notifications — litmus_4

No Firebase or server is required. Notifications use the inventory already saved
on this device. The app observes committed Room changes after product edits,
recorded sales, confirmed counts and imported manual-sync changes. It also checks
on process startup. There is no remote polling, new sync schedule, background
worker or notification foreground service.

## Behavior

- Active products with positive stock at or below their threshold are low.
  Zero/negative balances are out of stock. Healthy/inactive products do not alert.
- One notification summarizes current products needing attention, with up to five
  item lines. One batch import produces one summary rather than many sounds.
- A product warns once while low; reaching out of stock can warn again. A positive
  balance rearms its out-of-stock warning; getting above the threshold rearms
  its low-stock warning. Decreasing severity updates quietly.
- Existing notifications update quietly as quantities/names change and remove
  recovered/archived/deleted products. If no affected products remain, the
  notification is cancelled. Disconnect and switching alerts off cancel it too.
- Dismissing the notification does not cause it to reappear for unchanged low
  stock. Device-only SharedPreferences markers survive app/activity restarts and
  never enter Sheets or the sync queue. Undelivered alerts are not acknowledged.
- Tapping opens Stock Alerts, also available under Tools, with All/Low/Out filters
  and product search. Stock Alerts works even if Android notifications are off.
  Navigation stays behind the normal saved-session gate and does not bypass setup.
- The small icon is `ic_notification_monochrome.xml`, the existing launcher bag
  in white on transparency; the launcher color tile is not used as a small icon.

## Permission and Settings

Android 13+ asks contextually when an active product first needs attention. The
app explains local alerts, then opens the Android permission dialog only after
Enable notifications. Not now turns alerts off without repeatedly asking.
A denied request does not clear local data or block work. Settings provides an
app toggle and links to Android's notification/channel controls; permission and
channel availability are rechecked when returning to the app. Older Android
versions use notification/channel controls without a runtime permission request.

Notifications use the Stock alerts channel at default importance and private
lock-screen visibility. Android/OEM notification settings and Do Not Disturb
control actual delivery/sound. Markers record successful API submission; an
abrupt process/storage failure between posting and saving markers can duplicate
an alert. The app cannot guarantee that an OS-accepted notification was seen.

## Limits

Remote spreadsheet edits become visible only after manual sync. A stopped app
process cannot fetch remote changes or observe inventory elsewhere. New alerts
are evaluated from the current local snapshot on reopening; state changes that
happen entirely between evaluations are not a historical alert feed. No server
push, scheduled reminders or automatic sync is added in this branch.
Room/workbook schemas and the eight-worksheet contract are unchanged.

## Device acceptance

1. Android 13+: create or count a product to its low threshold. Check the context
   explanation, grant notification permission and inspect the summary/bag icon.
2. Record another sale while still low: update the balance quietly. Sell the
   remaining units: an out-of-stock alert may sound again.
3. Confirm a count above threshold: remove the notification. Lower it again:
   allow a fresh low warning. Restock an empty product to positive-but-low stock:
   no new low warning, but reaching zero again allows a fresh out warning.
4. Dismiss a notification, recreate/reopen the app and rename the same low
   product: no new audible warning for unchanged stock. A genuine new threshold
   event can show the summary again.
5. Deny permission or choose Not now; local work/Stock Alerts must stay available.
   Enable through Settings and Android controls; current undelivered stock risks
   should notify. Test channel disabled separately from global permission.
6. Tap while app is open and from a cold start: open Stock Alerts. Navigate Home
   and recreate: the notification request must not reopen the alert page.
7. Turn off internet and repeat a sale/count. Alerts must still work. Sync a valid
   workbook with several low/out products: create one summary and update the list.
8. Reset local data or disconnect: remove stale notifications. Archived/inactive
   products must not continue to appear.
