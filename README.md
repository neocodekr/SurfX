# SurfX Protocol

## Website (Vue / GitHub Pages)

The bilingual SurfX introduction is in [`website/`](website/). The site uses Vue 3 and Vite, with a `/` base path for the custom domain `www.surfx-protocol.com`. The whitepaper is deployed separately from [SurfX-whitepaper](https://github.com/neocodekr/SurfX-whitepaper) at `whitepaper.surfx-protocol.com`.

```powershell
cd website
npm ci
npm run dev
npm run build
```

The production files are generated in `website/dist/`. The workflow in `.github/workflows/pages.yml` builds and deploys them automatically when `main` changes. In the GitHub repository settings, set **Pages → Build and deployment → Source** to **GitHub Actions**. The published address is `https://www.surfx-protocol.com/`.

The website whitepaper describes the current product concept and implementation. App points are local demo data; it does not claim a live token or on-chain reward system.

Android application for the supplied SurfX 14-screen mobile concept. Package: `com.neocode.surfx`.

## What works

- Compose navigation for welcome, home (idle and surfing), check-in, missions, lesson, quiz, result, crew, invite, wallet, board, ranking, and profile.
- Local demo state for points, daily check-in, lesson, quiz, board upgrades, and 24-hour surfing. Reward actions cannot be repeated on the same day.
- The Korean and English blockchain question banks are bundled under `app/src/main/assets/quiz/` (1,200 questions per language). The daily quiz selects a question from the bank; Missions offers practice across all questions with difficulty and question-type filters. Answers are checked by stable option ID, and explanations and learning objectives are shown after submission.
- Profile → Language / 언어 switches between Korean and English, persists on this device, and keeps the current question ID when switched mid-quiz.
- Test AdMob banner, optional rewarded ad and exit-dialog native ad, UMP consent flow, remote version check, privacy and terms links.
- Signed, minified AAB for Google Play internal testing. Dependency native libraries are limited to 64-bit ARM and x86.

## Scope

The design materials are a static concept. This build labels its balances, rewards, crew, and ranking as demo data. It does not authenticate users, synchronize rewards, connect a token wallet, or validate Play Integrity tokens on a backend. Configure these services before a production release. The Google AdMob IDs are official test IDs.

## Build

The existing Play upload key is `D:/ai/keystore/surfx-upload.jks`, alias `surfx`. Set `SURFX_STORE_PASSWORD` and `SURFX_KEY_PASSWORD` as environment variables, then run `gradle :app:bundleRelease`. Never commit passwords or the service-account JSON.

The 512 x 512 launcher artwork is [surfx_icon_512.png](surfx_icon_512.png). The original supplied screens remain under `design/SurfX_UI/` for visual comparison.
