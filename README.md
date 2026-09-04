<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" height="128" alt="YummyTV logo" />
</p>

<h1 align="center">YummyTV</h1>

<p align="center">
  Неофициальный клиент <a href="https://yummyani.me/">YummyAnime</a> для Android TV и Android.<br/>
  Нативный интерфейс, встроенный плеер, библиотека, подписки и удобное управление с пульта.
</p>

<p align="center">
  <a href="README.en.md">English</a> · <a href="README.md">Русский</a>
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


> [!IMPORTANT]
> В поиске работы [@Etozhesandy](https://t.me/Etozhesandy) (Android, compose, kotlin). Не пишите туда с вопросами по программе - для этого есть Issues

## Что это

**YummyTV** — нативное Android-приложение для просмотра аниме с YummyAnime. Проект поддерживает два
отдельных интерфейса: удобный TV-режим для Android TV / приставок и мобильный режим для смартфонов.

Помимо просмотра, встроенного плеера и библиотеки, приложение включает сообщество (комментарии,
отзывы, новости, блогеры), социальные функции (профили, поиск пользователей, личные сообщения) и
скачивание серий для офлайн-просмотра.

На телевизоре приложение рассчитано на пульт, D-pad и крупные элементы интерфейса. На телефоне
доступны нижняя навигация, жесты в плеере и Picture-in-Picture.

<p align="center">
  <a href="https://github.com/Helandy/YummyTV/releases/latest"><b>Скачать APK</b></a>
  ·
  <a href="#скриншоты"><b>Скриншоты</b></a>
  ·
  <a href="#возможности"><b>Возможности</b></a>
  ·
  <a href="#для-разработчиков"><b>Для разработчиков</b></a>
</p>

---

## Скриншоты

### Android TV

<table>
  <tr>
    <td align="center"><b>Главная</b></td>
    <td align="center"><b>Страница тайтла</b></td>
    <td align="center"><b>Топ</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/tv/home.webp" width="320" alt="TV home screen" /></td>
    <td><img src="docs/screenshots/tv/details.webp" width="320" alt="TV details screen" /></td>
    <td><img src="docs/screenshots/tv/top.webp" width="320" alt="TV top screen" /></td>
  </tr>
  <tr>
    <td align="center"><b>Серии</b></td>
    <td align="center"><b>Балансеры</b></td>
    <td align="center"><b>Порядок просмотра</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/tv/episodes.webp" width="320" alt="TV episodes screen" /></td>
    <td><img src="docs/screenshots/tv/balancers.webp" width="320" alt="TV balancers screen" /></td>
    <td><img src="docs/screenshots/tv/viewing-order.webp" width="320" alt="TV viewing order screen" /></td>
  </tr>
</table>

<p align="center">
  <b>Плеер</b><br/>
  <img src="docs/screenshots/tv/player.webp" width="640" alt="TV player screen" />
</p>

### Android Mobile

<table>
  <tr>
    <td align="center"><b>Главная</b></td>
    <td align="center"><b>Страница тайтла</b></td>
    <td align="center"><b>Топ</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/mobile/home.webp" width="210" alt="Mobile home screen" /></td>
    <td><img src="docs/screenshots/mobile/details.webp" width="210" alt="Mobile details screen" /></td>
    <td><img src="docs/screenshots/mobile/top.webp" width="210" alt="Mobile top screen" /></td>
  </tr>
  <tr>
    <td align="center"><b>Библиотека</b></td>
    <td align="center"><b>Профиль</b></td>
    <td align="center"><b>Балансеры</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/mobile/library.webp" width="210" alt="Mobile library screen" /></td>
    <td><img src="docs/screenshots/mobile/profile.webp" width="210" alt="Mobile profile screen" /></td>
    <td><img src="docs/screenshots/mobile/balancers.webp" width="210" alt="Mobile balancers screen" /></td>
  </tr>
</table>

<p align="center">
  <b>Плеер</b><br/>
  <img src="docs/screenshots/mobile/player.webp" width="360" alt="Mobile player screen" />
</p>

---

## Возможности

### Интерфейс

- 📺 Отдельные интерфейсы для Android TV и Android Mobile.
- 🎮 Навигация с пульта, D-pad и сенсорного экрана.
- 🏠 Главная страница с подборками и быстрым продолжением просмотра.
- 🔎 Поиск по каталогу с фильтрами (жанр, тип, статус, год, сезон, рейтинг, сортировка), а также по
  студии и режиссёру.
- 🏆 Топ сериалов и фильмов, расписание выхода серий.
- 🖼️ Просмотр постеров и скриншотов внутри приложения.

### Библиотека и аккаунт

- 🔐 Авторизация в аккаунте YummyAnime.
- 📚 Библиотека: продолжить просмотр, любимые, смотрю, в планах, просмотрено, отложено и брошено.
- 🔔 Подписки на новые серии выбранных озвучек с предложением подписаться на выходящие тайтлы.
- ⭐ Избранное, пользовательские списки и оценки.
- 📄 Страница тайтла: описание, рейтинги, серии с названиями, трейлеры, похожие, порядок просмотра,
  скриншоты, коллекции и таймер до выхода следующей серии.

### Сообщество и социальное

- 💬 Комментарии с ответами, сортировкой, голосованием и скрытием спойлеров.
- 📝 Отзывы на тайтлы с оценками, реакциями и статусом модерации.
- 📰 Новости и публикации сайта, а также видео блогеров с подписками.
- 👤 Профили пользователей, поиск, друзья и личные сообщения (на мобильных устройствах).

### Скачивание и офлайн

- ⬇️ Скачивание серий для офлайна, менеджер загрузок и экспорт в MP4 (на мобильных).

### Плеер

Встроенный плеер на **Media3 / ExoPlayer** поддерживает:

- выбор качества, озвучки и балансера (Kodik, Aksor, Alloha, CVH, VK, Rutube, Sibnet, Zedfilm);
- скорость, масштаб и зум, автопропуск опенинга/эндинга;
- переход между сериями и автовоспроизведение следующей;
- жесты на мобильных (яркость, громкость, перемотка, зум) и управление мышью на TV;
- Picture-in-Picture, воспроизведение локальных файлов и предложение оценить тайтл.

### Настройки и интеграции

- 🎨 Темы оформления и режим фона (светлый / тёмный / системный).
- 🧩 Порядок кнопок на странице тайтла, размеры карточек, качество постеров, язык контента.
- 🏡 TV Home (Preview Channel, Watch Next), FAQ и обновления через GitHub Releases.

---

## Скачать

Последняя версия доступна в разделе **GitHub Releases**:

<p align="center">
  <a href="https://github.com/Helandy/YummyTV/releases/latest">
    <img alt="Download latest APK" src="https://img.shields.io/badge/Download%20latest%20APK-2ea44f?style=for-the-badge&logo=android&logoColor=white" />
  </a>
</p>

Скачай APK, передай его на Android TV, приставку или Android-устройство и открой файл для установки.

### Как установить на Android TV

Самый простой способ передать APK на телевизор — через [LocalSend](https://localsend.org/):

1. Установи LocalSend на телефон или компьютер.
2. Установи LocalSend на Android TV.
3. Подключи оба устройства к одной Wi-Fi сети.
4. Отправь APK на телевизор.
5. Открой APK на TV и разреши установку из этого источника, если Android попросит подтверждение.

---

## Для разработчиков

Проект построен на Kotlin и модульной архитектуре.

### Структура

- `core/*` — общие подсистемы: навигация, дизайн-система, сеть, хранилище, настройки, обновления,
  deep links, аналитика и TV-интеграции.
- `feature/*` — пользовательские сценарии: main, home, search, top, library, details, player,
  settings, account, collection, schedule, а также comments, reviews, posts, bloggers, messages,
  video-download, faq, pages, watching и commonScreen.
- `ui-tv` и `ui-mobile` — отдельные UI-модули для телевизора и мобильных устройств.

## Ограничения

YummyTV зависит от доступности YummyAnime и сторонних видеобалансеров. Если сайт, API, плеер или
источник временно недоступны, часть функций или воспроизведение видео могут не работать.

Для пользователей за пределами СНГ часть видеобалансеров может быть недоступна из-за региональных
ограничений или блокировок IP-адресов.

---

## Поддержка

- ⭐ Поставь звезду репозиторию.
- 🐞 Открой issue, если нашёл баг.
- 💡 Предложи улучшение через Issues.
- https://boosty.to/etozhesandy

---

## Disclaimer

- YummyTV — неофициальный клиент YummyAnime.
- Приложение не хранит и не распространяет контент.
- Все права принадлежат их владельцам.
