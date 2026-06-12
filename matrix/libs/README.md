# GlyphMatrix SDK (local AAR)

This module depends on the official **GlyphMatrix SDK**, shipped by Nothing as a local `.aar`
(not published to Maven). Place the AAR here; the `fileTree(... "*.aar")` dependency in
`matrix/build.gradle.kts` picks it up automatically.

## ⚠️ Phone (4a) Pro needs a recent AAR

The currently present `glyph-matrix-sdk-1.0.aar` (from release **V1.1**, Aug 2025) **predates
Phone (4a) Pro support** — its `com.nothing.ketchum.Glyph` class has no `DEVICE_25111p` constant
(only `DEVICE_23112` = Phone 3 and older). The app still compiles and runs, but the matrix will
report "unavailable" on a 4a Pro.

To drive the matrix on Phone (4a) Pro, replace it with the **latest** AAR that includes
`DEVICE_25111p` (and `Common.getDeviceMatrixLength()`), from the repository's main branch / newest
release:

https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit

Any `*.aar` placed in this folder is used; keep only one.

## Notes

- Required package: `com.nothing.ketchum.*`.
- The renderer resolves the device code reflectively from `Glyph.<sdkTarget>`
  (`DeviceProfile.sdkTarget`), so the app compiles against any SDK version and degrades gracefully
  when a device constant is missing.
