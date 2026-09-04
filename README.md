# Walzi — Android App

Jetpack Compose + MVI wallpaper app. Reads categories/wallpapers from Firebase Firestore
(written by the **Walzi Admin Portal**), loads images from Cloudflare R2 URLs, and deep-links
from push notifications straight to a wallpaper, category, or other screen.

- **UI**: Jetpack Compose, Material 3, single-activity
- **Architecture**: MVI (`Intent -> ViewModel -> State`, one-shot `Effect`s for snackbars),
  Hilt for DI
- **Data**: Firestore (`categories`, `wallpapers` collections — read-only, public rules),
  Coil for image loading
- **Local**: DataStore Preferences for on-device favorites (no auth in this build)
- **Push**: Firebase Cloud Messaging, with deep-link navigation parsed from the notification's
  data payload
- **Package name**: `com.yunok.walzi`
- **App name**: Walzi

## 1. Firebase setup

1. Use the **same Firebase project** as the Walzi Admin Portal (they share `categories` /
   `wallpapers` collections).
2. In Firebase Console → Project settings → Your apps, add an **Android app** with package
   name `com.yunok.walzi`.
3. Download `google-services.json` and place it at `app/google-services.json` (replacing
   `app/google-services.json.example`, which is just a placeholder/instructions file).
4. Firestore security rules only need to allow public reads on `categories` and `wallpapers`
   (see `firestore.rules` in the admin portal project) — this app never writes to Firestore.

## 2. Open & build

1. Open the `walzi-android` folder in **Android Studio (Koala or newer)**.
2. Let Gradle sync — it will pick up `gradle/libs.versions.toml` automatically.
3. If the Gradle wrapper jar is missing (this repo ships `gradle-wrapper.properties` but not
   the binary jar), run once from the project root:
   ```bash
   gradle wrapper --gradle-version 8.7
   ```
   or just open the project in Android Studio, which regenerates it automatically.
4. Run on a device/emulator running **API 24+**.

## 3. How data flows

```
Admin Portal (Next.js)  --writes-->  Firestore (categories, wallpapers)
                                            |
                                            v
                                   Walzi Android app (this project)
                                   FirestoreService -> WallpaperRepositoryImpl
                                            |
                                            v
                                  HomeViewModel / CategoryDetailViewModel / ...
                                            |
                                            v
                                       Compose UI
```

Wallpaper/category images themselves are **not** fetched through Firestore — Firestore only
stores the public Cloudflare R2 URL (`imageUrl`), which Coil loads directly.

## 4. Screens

| Screen | File | Notes |
|---|---|---|
| Home | `presentation/home/HomeScreen.kt` | Top tabs: Your Feed / Recent / Collections / Popular. Feed/Recent/Popular render a Pinterest-style masonry grid (`LazyVerticalStaggeredGrid`); Collections renders category tiles. |
| Category detail | `presentation/category/CategoryDetailScreen.kt` | Wallpapers filtered by `categoryId`, sorted by admin-set priority. |
| Wallpaper detail | `presentation/wallpaperdetail/WallpaperDetailScreen.kt` | Full-screen preview. **Set Wallpaper** opens a sheet (Home / Lock / Both) and calls `WallpaperManager` directly. **Download** saves to `Pictures/Walzi` via `MediaStore`. |
| Favorites | `presentation/favorites/FavoritesScreen.kt` | Wallpapers where `isFavorite == true` (from local DataStore). |
| Settings | `presentation/settings/SettingsScreen.kt` | UI toggles (auto-rotate, downloads) — wire these up to actual behaviour as needed. |

Navigation drawer (hamburger icon on Home) → Your Feed / Favorites / Settings, mirroring the
original design mock.

## 5. Push notifications & deep links

`WalziFirebaseMessagingService` (in `service/`) receives every push sent from the admin
portal's **Notifications** dashboard. Each message carries a data payload:

```
screen        "home" | "collections" | "favorites" | "category" | "wallpaper"
wallpaperId   present when screen == "wallpaper"
categoryId    present when screen == "category"
```

Tapping the notification launches `MainActivity` with those values as intent extras.
`MainActivity` turns them into a `DeepLinkTarget` and passes it to `AppRoot`, which navigates
to the right destination once the nav graph is ready (`AppRoot.kt`).

**To test:**
1. Run the app once so `WalziApp.onCreate()` subscribes it to the `all_users` FCM topic.
2. To test a *specific device* instead of the topic, log the FCM token — add a temporary
   `Log.d("FCM_TOKEN", token)` inside `WalziFirebaseMessagingService.onNewToken`, run the app,
   copy the token from Logcat, and paste it into the admin portal's Notifications page under
   "Device token (testing)".
3. Send a notification targeting **Wallpaper Detail** with a specific wallpaper from the admin
   portal — tapping it on-device should open `WallpaperDetailScreen` directly.

## 6. Favorites (local-only by design)

This build stores favorites in `DataStore` on-device (`data/local/FavoritesDataStore.kt`) to
match the original design mock, which had no sign-in. If you add Firebase Auth later, swap
`FavoritesDataStore` for a `users/{uid}/favorites` Firestore subcollection and update
`WallpaperRepositoryImpl` accordingly — the rest of the app (ViewModels, screens) doesn't need
to change since it only depends on the `WallpaperRepository` interface.

## 7. Folder structure

```
app/src/main/java/com/yunok/walzi/
  WalziApp.kt                 Hilt Application, notification channel, FCM topic subscribe
  MainActivity.kt              single Activity, deep-link extraction, notification permission
  di/                          Hilt modules (Firebase, Repository binding)
  data/
    model/                     Firestore DTOs (CategoryDto, WallpaperDto)
    remote/                    FirestoreService (snapshot listeners -> Flow)
    repository/                WallpaperRepositoryImpl
    local/                     FavoritesDataStore (DataStore Preferences)
  domain/
    model/                     Category, Wallpaper (UI-facing models)
    repository/                WallpaperRepository interface
  presentation/
    common/                    MVI base (BaseViewModel, Intent/State/Effect)
    navigation/                Screen routes, AppRoot (drawer + deep link), NavGraph
    components/                WallpaperCard, CategoryTile
    theme/                     Color, Type, Theme (dark, gradient-accent design system)
    home/                      Contract, ViewModel, Screen
    category/                  Contract, ViewModel, Screen
    favorites/                 Contract, ViewModel, Screen
    settings/                  Screen
    wallpaperdetail/           Contract, ViewModel, Screen
  service/
    WalziFirebaseMessagingService.kt
  util/
    WallpaperSetter.kt          WallpaperManager wrapper (Home/Lock/Both)
    ImageDownloader.kt          MediaStore save
```

## 8. Known limitations / next steps

- No authentication — favorites are per-device, not per-account.
- Search icon in the Home top bar is currently a no-op stub; wire it to a Firestore
  `whereGreaterThanOrEqualTo`/`whereLessThan` prefix query or an external search index
  (Algolia/Typesense) for real text search.
- Settings toggles are UI-only placeholders — connect "Auto-rotate wallpaper" to `WorkManager`
  for a real daily-rotation feature.
- Legacy launcher icons are simple generated placeholders — swap in real brand assets via
  Android Studio's Image Asset Studio before shipping.
