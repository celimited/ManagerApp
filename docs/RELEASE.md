# Releasing ManagerApp (Android + iOS)

One shared app for every client. It has two installable builds:

| | Android flavor | iOS config / scheme | App ID | Name | Server | Goes to |
|---|---|---|---|---|---|---|
| **Test** | `stage` | `Debug-Test` / `Release-Test` | `org.celimited.manager.test` (Android), `org.celimited.manager.Manager.test` (iOS) | ManageEsae Test | `http://210.4.65.222/…` | Testers (APK), TestFlight |
| **Live** | `live` | `Debug-Live` / `Release-Live` | `org.celimited.manager` (Android), `org.celimited.manager.Manager` (iOS) | ManageEsae | `https://syngentawebapp.azurewebsites.net/…` | Play Store, App Store |

Base URLs live in two places that must match:
- Android: `productFlavors` in `composeApp/build.gradle.kts`.
- iOS: `iosApp/Configuration/{Test,Live}.xcconfig`.

Shared code reads the URL only through `AppConfig`
(`composeApp/src/commonMain/kotlin/org/celimited/manager/core/common/AppConfig.kt`).

## Version
- Single source: `version.properties`, using MAJOR.MINOR.PATCH plus a build number that only ever goes up.
  Android and iOS always ship the same pair.
- Never edit it by hand. Run:
  ```
  ./gradlew bumpVersion -Pbump=patch   # 1.2.0 -> 1.2.1, bug fixes
  ./gradlew bumpVersion -Pbump=minor   # 1.2.1 -> 1.3.0, new features
  ./gradlew bumpVersion -Pbump=major   # 1.3.0 -> 2.0.0, big / breaking changes
  ./gradlew bumpVersion -Pbump=build   # same version, new build number (re-upload after a rejected build)
  ```
- The command also rewrites `iosApp/Configuration/Version.xcconfig`. Commit both files.

## Release steps
1. On `main`, with everything merged: `./gradlew bumpVersion -Pbump=<type>`.
2. `git commit -am "release X.Y.Z"` and `git tag vX.Y.Z`.
3. **Test builds**:
   - Android: `./gradlew :composeApp:assembleStageRelease`. Send
     `composeApp/build/outputs/apk/stage/release/*.apk` to testers.
   - iOS: in Xcode, run Product ▸ Archive with the **ManagerApp Test** scheme, then upload to TestFlight
     (the "ManageEsae Test" App Store Connect record).
4. Get sign-off from testers on the test server.
5. **Live builds** from the same commit:
   - Android: `./gradlew :composeApp:bundleLiveRelease`. Upload
     `composeApp/build/outputs/bundle/liveRelease/*.aab` to the Play Console.
   - iOS: Archive with the **ManagerApp Live** scheme, upload, and submit for review.
6. `git push && git push --tags`.

**Hotfix:**
1. `git checkout -b hotfix/X.Y.Z+1 vX.Y.Z`
2. Fix the bug.
3. `bumpVersion -Pbump=patch`
4. Release as above.
5. Merge back to `main`.

## Android signing
- The release keystore is **not** in git. Keep it, and its passwords, in the team password vault.
- To sign locally, create `keystore.properties` in the repo root. This file is gitignored. Its contents:
  ```
  storeFile=keystore/syngenta
  storePassword=...
  keyAlias=...
  keyPassword=...
  ```
- On CI, set these environment variables instead:
  - `MANAGER_KEYSTORE_FILE`
  - `MANAGER_KEYSTORE_PASSWORD`
  - `MANAGER_KEY_ALIAS`
  - `MANAGER_KEY_PASSWORD`
- Without either, release builds come out unsigned.
- The keystore was committed to git in the past, so it is still in the history. Use Play App Signing. If the
  repo has ever been shared outside the core team, ask Play to reset the upload key.

## iOS one-time setup (on a Mac, in Xcode)
The xcconfigs are in place. The Xcode project still needs these steps, which can't be done from Windows:
1. In the Project ▸ Info ▸ Configurations panel, duplicate `Debug` → `Debug-Test` and `Debug-Live`, and
   `Release` → `Release-Test` and `Release-Live`. Then delete the original `Debug` and `Release`, or keep them
   as Live.
2. For each configuration, set the iosApp target's configuration file: the `*-Test` ones to `Test.xcconfig`,
   the `*-Live` ones to `Live.xcconfig`.
3. Under Product ▸ Scheme ▸ Manage Schemes, create the **ManagerApp Test** scheme (Run = `Debug-Test`,
   Archive = `Release-Test`) and the **ManagerApp Live** scheme (Run = `Debug-Live`,
   Archive = `Release-Live`). Tick **Shared** on both.
4. Set `TEAM_ID` in `Common.xcconfig`.
5. Check that `PRODUCT_BUNDLE_IDENTIFIER` in `Live.xcconfig` matches the App Store Connect app. Register the
   `.test` bundle ID and create the TestFlight-only "ManageEsae Test" record.
6. Optional: the insecure-HTTP exception for `210.4.65.222` (App Transport Security, ATS) in `Info.plist` is
   only needed by Test. To keep it out of Live, give the `*-Test` configurations their own Info plist file.

Until step 1–2 is done, `Config.xcconfig` includes `Live.xcconfig`, so the existing Debug/Release
configurations build Live.

## Adding another client later
This is one app for every client, so no new flavor or store listing is needed. When the way to pick a client
is decided (company code, lookup API, etc.), change only how `AppConfig.baseUrl` is produced. Feature code
never sees the URL.
