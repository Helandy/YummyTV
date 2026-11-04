# Источник Studio MIR

Модули `source/studiomir/{domain,data}` (по образцу `source/anidub`). Наличие тайтла и число серий приходят с сервера YummyTV
из общей ручки `POST /availability` (`sourceId = "studiomir"`, ключ — slug страницы `107-hotaru-no-yomeiri`).

- `StudioMirExtraVideoRepository` строит по серии `AnimeVideo` с `source://studiomir/{slug}/{episode}`, озвучка и плеер — «Studio MIR».
- При воспроизведении `StudioMirExtraStreamRepository` → `ResolveStudioMirStreamsUseCase` → `StudioMirRepositoryImpl`: скачивает
  `https://studiomir.club/anime/{slug}`, из HTML берёт плейлист плеера TSM (`new Playerjs({file:[{title:"Серия N", file:"[720p]url,[1080p]url"}]})`)
  и отдаёт HLS-варианты серии по возрастанию качества (лучшее — по умолчанию). «Трейлер» пропускается.
- Ссылки не подписаны, заголовки не нужны, кэша ссылок нет.
- Логотип: ключ `studiomir` в `EXTERNAL_SOURCE_KEYS` (значок источника по умолчанию).
