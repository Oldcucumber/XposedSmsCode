# legacy-APIupdate implementation ledger

The accepted scope is modern libxposed API 102 only, Android 8 minimum,
Android 14–16 primary compatibility targets, unchanged package identity and UI.
No legacy APK, UI rewrite, parser algorithm change or database replacement.

## Required delivery

- [ ] Build baseline and behavior regression tests
- [ ] Entity / resource / notification fixes
- [ ] Pure parser, bounded scheduling, fail-open SMS handling
- [ ] API 102 lifecycle, reflection, configuration service
- [ ] Authenticated IPC, no world-writable cross-process storage
- [ ] Android permission / telephony capability adaptation
- [ ] Retained-data upgrade without resetting settings or history
- [ ] Other > Backup dialog, JSON import/export of settings/theme/rules/blocked apps
- [ ] Transactional full restore excluding history and logs
- [ ] System backup of configuration only
- [ ] Sensitive clipboard marker; original logging policy
- [ ] Existing status UI, fork links and version branding
- [ ] Debug + minified build, tests and APK inspection
- [ ] Copy only final APK to Windows Desktop

## Fixed policies

Uncertain SMS parsing/interception must preserve original delivery. Auto-input
still attempts input when foreground/blacklist lookup is uncertain. Unsupported
features degrade independently; first-unlock operation is the supported baseline.
No new clipboard expiry, history retention change, or automatic hot reload.
JSON fully restores user settings, rules and blocked apps, not history or logs.
Preserved original app data is migrated automatically and idempotently.
Repository URL and release signing are unconfigured; use debug signing for tests.
Only tested environments may be described as verified. No device is connected.

## Verification notes

Initial tree: upstream 7be0a27. Gradle 7.5 fails under JDK 21 (major version 65).
JDK 17 is installed; tools/build.ps1 selects it. API/service 102.0.0 AAR metadata
requires compileSdk 37; targetSdk remains 34 to minimize behavior changes.
