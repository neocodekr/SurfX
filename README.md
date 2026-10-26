# SurfX Protocol

Android application for the supplied SurfX 14-screen mobile concept. Package: `com.neocode.surfx`.

## What works

- Compose navigation for welcome, home (idle and surfing), check-in, missions, lesson, quiz, result, crew, invite, wallet, board, ranking, and profile.
- Local demo state for points, daily check-in, lesson, quiz, board upgrades, and 24-hour surfing. Reward actions cannot be repeated on the same day.
- Test AdMob banner, optional rewarded ad and exit-dialog native ad, UMP consent flow, remote version check, privacy and terms links.
- Signed, minified AAB for Google Play internal testing. Dependency native libraries are limited to 64-bit ARM and x86.

## Scope

The design materials are a static concept. This build labels its balances, rewards, crew, and ranking as demo data. It does not authenticate users, synchronize rewards, connect a token wallet, or validate Play Integrity tokens on a backend. Configure these services before a production release. The Google AdMob IDs are official test IDs.

## Build

The existing Play upload key is `D:/ai/keystore/surfx-upload.jks`, alias `surfx`. Set `SURFX_STORE_PASSWORD` and `SURFX_KEY_PASSWORD` as environment variables, then run `gradle :app:bundleRelease`. Never commit passwords or the service-account JSON.

The 512 x 512 launcher artwork is [surfx_icon_512.png](surfx_icon_512.png). The original supplied screens remain under `design/SurfX_UI/` for visual comparison.
