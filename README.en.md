<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" height="128" alt="YummyTV logo" />
</p>

<h1 align="center">YummyTV</h1>

<p align="center">
  Unofficial Android client for <a href="https://yummyani.me/">yummyani.me</a><br/>
  Watch anime on Android TV, TV boxes, or phones with a native UI and built-in player.
</p>

<p align="center">
  <a href="README.md">Русский README</a>
</p>

<p align="center">
  <a href="https://github.com/Helandy/yummytv/releases">
    <img alt="Download YummyTV APK" src="https://img.shields.io/badge/Download-APK-2ea44f?style=for-the-badge">
  </a>
  <a href="https://github.com/Helandy/yummytv">
    <img alt="GitHub stars" src="https://img.shields.io/github/stars/Helandy/yummytv?style=for-the-badge">
  </a>
  <a href="https://github.com/Helandy/yummytv/releases">
    <img alt="Latest release" src="https://img.shields.io/github/v/release/Helandy/yummytv?style=for-the-badge">
  </a>
</p>

## What It Is

YummyTV is a native Android app for watching anime from yummyani.me. It is no longer limited to TVs
only: the project includes dedicated interfaces for Android TV and mobile Android devices.

On TV, the app is built for remote and D-pad navigation: no browser, cursor, or tiny web controls.
On phones, it provides a mobile UI with bottom navigation, player gestures, and Picture-in-Picture.

## Features

- 📺 Dedicated TV and mobile interfaces.
- 🎮 Remote, D-pad, and touch-friendly navigation.
- 🔎 Catalog search with filters for genres, type, status, year, season, age rating, and sorting.
- 🏠 Home screen with collections and quick continue watching.
- 📅 Episode release schedule.
- 🏆 Top anime series and movies.
- 📚 Library: continue watching, favorites, watching, planned, completed, postponed, and dropped.
- 📄 Title pages with descriptions, ratings, episodes, trailers, similar titles, viewing order,
  screenshots, and collections.
- 🔔 Subscriptions for new episodes from selected voiceovers.
- ⭐ Favorites, user lists, and ratings.
- 🖼️ Poster and screenshot viewer inside the app.

## Account and Sync

YummyTV can work with account:

- sign in with email or nickname and password;
- solve captcha inside the app when the service requests it;
- profile, statistics, and notifications;
- read and update user lists;
- sync watched episodes, ratings, favorites, and subscriptions;
- remove items from lists and watch progress.

Local library data and watch progress remain on the device even after signing out.

## Player

The built-in player is based on Media3/ExoPlayer and supports:

- quality, voiceover, and video balancer selection;
- default player preference: Kodik, Aksor, Alloha, CVH, VK, or Rutube;
- playback speed;
- video resize and zoom;
- automatic opening and ending skip when timestamps are available;
- previous and next episode navigation;
- prompt to rate the title after watching;
- Picture-in-Picture on mobile devices.

## Settings and Integrations

- App themes: Warm Amber, Sakura, Mint, Ocean, and Graphite.
- Poster card size and poster quality.
- Preview cache size.
- Content language: default, Russian, English, or Ukrainian.
- Custom action order on title pages.
- TV Home: Preview Channel with new releases and Watch Next for continue watching.
- GitHub Releases update checks and in-app APK installation.

## Screenshots

|                              Home                               |                                Details                                |                              Top                              |
|:---------------------------------------------------------------:|:---------------------------------------------------------------------:|:-------------------------------------------------------------:|
| <img src="docs/screenshots/Home.webp" width="320" alt="Home" /> | <img src="docs/screenshots/Details.webp" width="320" alt="Details" /> | <img src="docs/screenshots/Top.webp" width="320" alt="Top" /> |

|                               Series                                |                                    Continue Watching                                     |                                Similar                                |
|:-------------------------------------------------------------------:|:----------------------------------------------------------------------------------------:|:---------------------------------------------------------------------:|
| <img src="docs/screenshots/Series.webp" width="320" alt="Series" /> | <img src="docs/screenshots/ContinueWatching.webp" width="320" alt="Continue Watching" /> | <img src="docs/screenshots/Similar.webp" width="320" alt="Similar" /> |

|                               Player                                |                                      Quality                                       |                                     Voice                                      |
|:-------------------------------------------------------------------:|:----------------------------------------------------------------------------------:|:------------------------------------------------------------------------------:|
| <img src="docs/screenshots/Player.webp" width="320" alt="Player" /> | <img src="docs/screenshots/PlayerQuality.webp" width="320" alt="Player Quality" /> | <img src="docs/screenshots/PlayerVoice.webp" width="320" alt="Player Voice" /> |

|                                   Select Balancer                                    |                                   Player Balancer                                    |
|:------------------------------------------------------------------------------------:|:------------------------------------------------------------------------------------:|
| <img src="docs/screenshots/SelectBalancer.webp" width="420" alt="Select Balancer" /> | <img src="docs/screenshots/PlayerBalancer.webp" width="420" alt="Player Balancer" /> |

## Download

Install the latest APK from [GitHub Releases](https://github.com/Helandy/yummytv/releases).

Download the APK, transfer it to your TV, Android TV box, or Android device, and open the file to
install it.

## How to Install on Android TV

The easiest way to transfer the APK to your TV is with [LocalSend](https://localsend.org/):

1. Install LocalSend on your phone or computer.
2. Install LocalSend on your Android TV.
3. Connect both devices to the same Wi-Fi network.
4. Send the APK to your TV.
5. Open the APK on your TV and allow installation from this source if Android asks for confirmation.

## For Developers

The project is built with Kotlin and clean modular architecture:

- `core/*` contains shared subsystems: navigation, design system, networking, storage, settings,
  updates, deep links, analytics, and TV integrations.
- `feature/*` is split by user workflows: main, home, search, top, library, details, player,
  settings, account, collection, and schedule.
- UI is written with Jetpack Compose and separate `ui-tv` and `ui-mobile`
  modules.
- The stack includes Navigation 3, Hilt, Ktor, Room, DataStore, Coil, WorkManager, and Media3.

## Limitations

YummyTV depends on the availability of yummyani.me and third-party video balancers. If the
website, API, player, or source is temporarily unavailable, some features or video playback may not
work.

For users outside the CIS region, some video balancers may be unavailable due to regional
restrictions or IP blocks.

## Support

- ⭐ Star the repository.
- 🐞 Open an issue if you find a bug.
- 💡 Suggest improvements via Issues.

## Disclaimer

- YummyTV is an unofficial client.
- The app does not host or distribute any content.
- All rights belong to their respective owners.
