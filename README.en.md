<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" height="128" alt="YummyTV logo" />
</p>

<h1 align="center">YummyTV</h1>

<p align="center">
  An unofficial <a href="https://yummyani.me/">YummyAnime</a> client for Android TV and Android.<br/>
  Native UI, built-in player, library, subscriptions, and remote-friendly TV navigation.
</p>

<p align="center">
  <a href="README.md">Русский</a> · <a href="README.en.md">English</a>
</p>

<p align="center">
  <a href="https://github.com/Helandy/YummyTV/releases/latest">
    <img alt="Download APK" src="https://img.shields.io/badge/Download-APK-2ea44f?style=for-the-badge" />
  </a>
  <a href="https://github.com/Helandy/YummyTV/releases/latest">
    <img alt="Latest release" src="https://img.shields.io/github/v/release/Helandy/YummyTV?style=for-the-badge" />
  </a>
  <a href="https://github.com/Helandy/YummyTV/releases">
    <img alt="Total downloads" src="https://img.shields.io/github/downloads/Helandy/YummyTV/total?style=for-the-badge" />
  </a>
  <a href="https://github.com/Helandy/YummyTV/stargazers">
    <img alt="GitHub stars" src="https://img.shields.io/github/stars/Helandy/YummyTV?style=for-the-badge" />
  </a>
</p>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-7.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" />
  <img alt="Android TV" src="https://img.shields.io/badge/Android%20TV-supported-4285F4?style=flat-square&logo=androidtv&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white" />
  <img alt="Media3" src="https://img.shields.io/badge/Player-Media3%20%2F%20ExoPlayer-orange?style=flat-square" />
</p>

---

## What is YummyTV?

**YummyTV** is a native Android app for watching anime from YummyAnime. The project includes two
separate experiences: a TV-first interface for Android TV / set-top boxes and a mobile interface for
Android phones.

Beyond streaming, the built-in player, and the library, the app includes community features
(comments, reviews, news, bloggers), social features (profiles, user search, direct messages), and
episode downloads for offline viewing.

On TV, the app is designed for remote control, D-pad navigation, and large UI elements. On mobile,
it supports bottom navigation, player gestures, and Picture-in-Picture.

<p align="center">
  <a href="https://github.com/Helandy/YummyTV/releases/latest"><b>Download APK</b></a>
  ·
  <a href="#screenshots"><b>Screenshots</b></a>
  ·
  <a href="#features"><b>Features</b></a>
  ·
  <a href="#for-developers"><b>For developers</b></a>
</p>

---

## Screenshots

### Android TV

<table>
  <tr>
    <td align="center"><b>Home</b></td>
    <td align="center"><b>Title details</b></td>
    <td align="center"><b>Top</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/tv/home.webp" width="320" alt="TV home screen" /></td>
    <td><img src="docs/screenshots/tv/details.webp" width="320" alt="TV details screen" /></td>
    <td><img src="docs/screenshots/tv/top.webp" width="320" alt="TV top screen" /></td>
  </tr>
  <tr>
    <td align="center"><b>Episodes</b></td>
    <td align="center"><b>Balancers</b></td>
    <td align="center"><b>Viewing order</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/tv/episodes.webp" width="320" alt="TV episodes screen" /></td>
    <td><img src="docs/screenshots/tv/balancers.webp" width="320" alt="TV balancers screen" /></td>
    <td><img src="docs/screenshots/tv/viewing-order.webp" width="320" alt="TV viewing order screen" /></td>
  </tr>
</table>

<p align="center">
  <b>Player</b><br/>
  <img src="docs/screenshots/tv/player.webp" width="640" alt="TV player screen" />
</p>

### Android Mobile

<table>
  <tr>
    <td align="center"><b>Home</b></td>
    <td align="center"><b>Title details</b></td>
    <td align="center"><b>Top</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/mobile/home.webp" width="210" alt="Mobile home screen" /></td>
    <td><img src="docs/screenshots/mobile/details.webp" width="210" alt="Mobile details screen" /></td>
    <td><img src="docs/screenshots/mobile/top.webp" width="210" alt="Mobile top screen" /></td>
  </tr>
  <tr>
    <td align="center"><b>Library</b></td>
    <td align="center"><b>Profile</b></td>
    <td align="center"><b>Balancers</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/mobile/library.webp" width="210" alt="Mobile library screen" /></td>
    <td><img src="docs/screenshots/mobile/profile.webp" width="210" alt="Mobile profile screen" /></td>
    <td><img src="docs/screenshots/mobile/balancers.webp" width="210" alt="Mobile balancers screen" /></td>
  </tr>
</table>

<p align="center">
  <b>Player</b><br/>
  <img src="docs/screenshots/mobile/player.webp" width="360" alt="Mobile player screen" />
</p>

---

## Features

### Interface

- 📺 Separate interfaces for Android TV and Android Mobile.
- 🎮 Remote, D-pad, and touch navigation.
- 🏠 Home screen with collections and quick continue watching.
- 🔎 Catalog search with filters (genre, type, status, year, season, rating, sorting), plus search by
  studio and director.
- 🏆 Top anime series and movies, and the episode release schedule.
- 🖼️ Poster and screenshot viewer inside the app.

### Library and account

- 🔐 YummyAnime account login.
- 📚 Library sections: continue watching, favorites, watching, planned, completed, postponed, and
  dropped.
- 🔔 Subscriptions for new episodes from selected voice-over teams, with a prompt to subscribe to
  ongoing titles.
- ⭐ Favorites, user lists, and ratings.
- 📄 Title page: description, ratings, episodes with titles, trailers, similar titles, viewing order,
  screenshots, collections, and a countdown to the next episode.

### Community and social

- 💬 Comments with replies, sorting, voting, and spoiler hiding.
- 📝 Title reviews with ratings, reactions, and moderation status.
- 📰 Site news and publications, plus blogger videos with subscriptions.
- 👤 User profiles, search, friends, and direct messages (on mobile).

### Downloads and offline

- ⬇️ Download episodes for offline viewing, a download manager, and MP4 export (on mobile).

### Player

The built-in player is based on **Media3 / ExoPlayer** and supports:

- quality, voice-over, and balancer selection (Kodik, Aksor, Alloha, CVH, VK, Rutube, Sibnet,
  Zedfilm);
- playback speed, scale and zoom, and automatic opening/ending skip;
- previous/next episode navigation and auto-play of the next episode;
- mobile gestures (brightness, volume, seeking, zoom) and mouse control on TV;
- Picture-in-Picture, local file playback, and a rating suggestion after watching.

### Settings and integrations

- 🎨 App themes and background mode (light / dark / system).
- 🧩 Details button order, card sizes, poster quality, and content language.
- 🏡 TV Home (Preview Channel, Watch Next), FAQ, and GitHub Releases updates.

---

## Download

The latest version is available on **GitHub Releases**:

<p align="center">
  <a href="https://github.com/Helandy/YummyTV/releases/latest">
    <img alt="Download latest APK" src="https://img.shields.io/badge/Download%20latest%20APK-2ea44f?style=for-the-badge&logo=android&logoColor=white" />
  </a>
</p>

Download the APK, transfer it to your Android TV, set-top box, or Android device, and open the file
to install it.

### How to install on Android TV

The easiest way to transfer the APK to a TV is [LocalSend](https://localsend.org/):

1. Install LocalSend on your phone or computer.
2. Install LocalSend on Android TV.
3. Connect both devices to the same Wi-Fi network.
4. Send the APK to your TV.
5. Open the APK on TV and allow installation from this source if Android asks for confirmation.

---

## For developers

The project is built with Kotlin and a modular architecture.

### Structure

- `core/*` — shared systems: navigation, design system, network, storage, settings, updates, deep
  links, analytics, and TV integrations.
- `feature/*` — user scenarios: main, home, search, top, library, details, player, settings,
  account, collection, schedule, plus comments, reviews, posts, bloggers, messages, video-download,
  faq, pages, watching, and commonScreen.
- `ui-tv` and `ui-mobile` — separate UI modules for TV and mobile devices.

## Limitations

YummyTV depends on YummyAnime and third-party video balancers. If the website, API, player, or
source is temporarily unavailable, some features or video playback may not work.

For users outside CIS countries, some video balancers may be unavailable due to regional
restrictions or IP blocks.

---

## ☕ Support the project

YummyTV is developed and maintained independently.

If you enjoy using the app and want to support its future development:

[![Support on Boosty](https://img.shields.io/badge/Support%20on-Boosty-orange?style=for-the-badge)](https://boosty.to/etozhesandy)

You can also support the project for free:

* ⭐ **Star the repository** — it helps others discover the project
* 🐞 **Report bugs** via [Issues](https://github.com/Helandy/YummyTV/issues)
* 💡 **Suggest improvements** via [Discussions](https://github.com/Helandy/YummyTV/discussions) or [Issues](https://github.com/Helandy/YummyTV/issues)

---

## Disclaimer

- YummyTV is an unofficial YummyAnime client.
- The app does not host, store, or distribute content.
- All rights belong to their respective owners.
