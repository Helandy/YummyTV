# Плеер Fenix

Общий разбор плеера `fenixplay.xyz` для [AnimeON](animeon-source.md) и [Mikai](mikai-source.md): модули `source/fenix/{domain,data}`
(как у `source/ashdi`). Нет каталога `source/fenix` — нет и зависимости в `app/build.gradle.kts`; Mikai подключается только вместе с ним.

- `ResolveFenixStreamsUseCase(pageUrl, track)` → `FenixRepository.getStreams`. Принимаются только
  `https://fenixplay.xyz/vod/{токен}` (серия и озвучка заданы токеном, фрагмент `#embed=…` отбрасывается) и
  `https://fenixplay.xyz/embed/{токен}?ep={N}&edition={tv|…}`.
- Страница отдаёт два объекта: `window.FENIX_PLAYLIST` (серии, у каждой `tracks` с озвучками и своим `hls_url`) и
  `window.FENIX_MEDIA_ACCESS` (`token` доступа к `media.fenixplay.xyz`, ограничен путём тайтла и сроком). Объекты вырезаются со счётом
  скобок с учётом строк.
- Новый вид страницы (с октября 2026): `FENIX_PLAYLIST` — только индекс серий (`slug`, `episodes[]` без `hls_master_url` и `tracks`) и
  `playback.token`. Потоки и озвучки серии берутся запросом `GET https://fenixplay.xyz/api/playback/{slug}/episodes/{edition}/{номер}` с
  заголовком `X-Fenix-Playback: {playback.token}` (ответ `{episode:{hls_master_url, tracks[]…}, token}`). Приложение идёт в него,
  если в серии страницы потоков нет; если есть (старый вид) — читает их со страницы. Мастер теперь может содержать HEVC 1080p и AVC 480p,
  плеер выбирает поддерживаемый.
- Серия: на `/vod/` первая, на `/embed/` по `ep` и `edition` (по умолчанию `default_edition`, затем `tv`). Озвучка: на `/vod/` та, чей
  `vod_url` равен пути страницы; на `/embed/` по названию `track` без учёта регистра; иначе дорожка по умолчанию (оригинал).
- Результат — `hls_url` озвучки + `?token=…` (без токена 403). Токен дописывается только к адресам `*.fenixplay.xyz`. Плейлисты и
  сегменты подписаны сами (`token`/`edge` в query), заголовков не нужно. Звук у Fenix отдельной дорожкой мастера (`EXT-X-MEDIA`), поэтому
  карты качеств нет: плеер играет мастер адаптивно.
- Гео-блока нет: проверено с российского IP (страница, мастер, сегменты видео и звука отдают 200/206).
- Не сделано: субтитры (`subtitles[].url`, `.vtt` отвечает 307 с токеном), отметки опенинга/эндинга (`intro_*`/`ending_*`), реклама VAST
  страницы не загружается.
