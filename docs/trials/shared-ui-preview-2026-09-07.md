# Shared layout device trial — Feed controls follow-up

This branch contains the verified web bundle from `poplur-clay-map` commit
`2a92cb24ec33ee656bd22bfdac1f1389df9a23aa`. Native Kotlin application code is
unchanged. The user chose shared layouts, native media capabilities and WebGL
only for Map as the migration target, then approved trials on iPhone and Android.
Expo builds and installations remain excluded.

Manual Feed D-pad and dial choices now stay selected, including empty channels
and across feed/location updates. The sprite navigator responds to tilt without
a prior touch and beyond three seconds. It renews the native motion lease while
visible, stops on background/hidden edges, and switches images only on meaningful
pose changes. No navigator WebGL context or animation clock was restored.

The production thermal gate passed without a skip or budget increase. All 1,439
web files exactly match the frozen build in native iOS. Home, Feed and Profile
have zero WebGL contexts; Map has one. Map retained heap remained 17.3 MB across
the measured 60-second dwell with zero idle renders. There are 58 passing focused
web tests, including manual empty-channel tuning and sensor lifecycle coverage.

The first iPhone trial was installed, but its heat recording was stopped after
the controls failed. Its logs reached a fair thermal state. These desktop checks
do not prove physical-device overheating is resolved; the corrected build needs
a separate active-use endurance result. Package and device verification receipts
are recorded in `.codex/install-reports/shared-ui-controls-20260907/` in the parent
workspace. iPad, Watch, tablet and Wear installations are outside this request.

Custom unbaked GLBs and specialized 3D games/avatar tools retain their renderers.
Existing camera integration and platform media decoding remain in use.
Rollback baseline: `checkpoint/pre-native-shell-2026-09-07`.
