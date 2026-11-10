# Releasing ManagerApp (Android + iOS)

**One app for every client** (Syngenta now; AKG, Coke, Arla later): one Play Store listing, one App Store
listing. Builds differ only by **environment**, never by client. A flavor per client would make a separate
app.

| | Android variants | iOS config / scheme | App ID | Name | Base URL | Goes to |
|---|---|---|---|---|---|---|
| **Dev** | `devDebug` (default), `devRelease` | `Debug-Test` / `Release-Test` | `org.celimited.manager.dev` (Android), `org.celimited.manager.Manager.dev` (iOS) | ManageEsae Dev | `http://210.4.65.222/SupervisorApp/api/v1/` | Testers (APK), TestFlight |
| **Prod** | `prodDebug`, `prodRelease` | `Debug-Live` / `Release-Live` | `org.celimited.manager` (Android), `org.celimited.manager.Manager` (iOS) | ManageEsae | `https://syngentawebapp.azurewebsites.net/SupervisorApp/api/v1/` | Play Store, App Store |

Both URLs live in one place for both platforms: `EnvironmentBaseUrlProvider` in
`composeApp/src/commonMain/kotlin/org/celimited/manager/core/network/BaseUrlProvider.kt`. The build only
says which environment it is: `BuildConfig.ENVIRONMENT` on Android, `APP_ENVIRONMENT` on iOS.

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
   - Android: `./gradlew :composeApp:assembleDevRelease`. Send
     `composeApp/build/outputs/apk/dev/release/*.apk` to testers.
   - iOS: in Xcode, run Product ▸ Archive with the **ManagerApp Test** scheme, then upload to TestFlight
     (the "ManageEsae Dev" App Store Connect record).
4. Get sign-off from testers on the test server.
5. **Live builds** from the same commit:
   - Android: `./gradlew :composeApp:bundleProdRelease`. Upload
     `composeApp/build/outputs/bundle/prodRelease/*.aab` to the Play Console.
   - iOS: Archive with the **ManagerApp Live** scheme, upload, and submit for review. In the review notes,
     give Apple a working demo account on the live server (and, once one exists, the company code).
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
   `.dev` bundle ID and create the TestFlight-only "ManageEsae Dev" record.
6. Optional: the insecure-HTTP exception for `210.4.65.222` (App Transport Security, ATS) in `Info.plist` is
   only needed by Test. To keep it out of Live, give the `*-Test` configurations their own Info plist file.

Until step 1–2 is done, `Config.xcconfig` includes `Live.xcconfig`, so the existing Debug/Release
configurations build Live.

## Adding a client / future server options
Adding a client never needs a new flavor or a new store listing. The app needs a way to know **which**
client a user belongs to, because login only sends `loginId` + `password`. Every option below only replaces
the `BaseUrlProvider` bound in `core/di/NetworkModule.kt`, plus a small screen for A and B. Ktor reads the
provider on every request, so switching clients at runtime works without rebuilding the HTTP client.

The two directions under consideration:

| Option | Mobile work | Server work |
|---|---|---|
| **Company code** | One-time "Company code" screen; the app saves the code. A new `BaseUrlProvider` returns that company's URL, either from a lookup or from a list | A lookup (static JSON or tiny endpoint), or nothing if the list ships in the app |
| **API gateway** | `EnvironmentBaseUrlProvider` stays (dev/prod gateway URLs) + a client header on every request | Gateway (e.g. Azure API Management / Front Door) routing each client to its server |

Until one is chosen, the app talks to the single dev/prod server above.
