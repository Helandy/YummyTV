# Замер старта в поле

Время холодного старта у реальных пользователей — событие `app_startup` в AppMetrica. Лабораторный
замер (`StartupBenchmark`, см. [baseline-profile.md](baseline-profile.md)) показывает старт на
эмуляторе, а этот — на настоящих телефонах и приставках, включая самые слабые.

## Что меряется

Все длительности отсчитываются от старта процесса (`Process.getStartUptimeMillis()`).

| Метрика            | Что значит                                                                              | Где фиксируется                                     |
|--------------------|-----------------------------------------------------------------------------------------|-----------------------------------------------------|
| TTID               | первый кадр главной Activity (`MobileActivity`/`TvActivity`), обычно лоадер             | первый `onDraw` корневой view                       |
| TTFD               | главная отрисована целиком: лента и «продолжить просмотр» загружены или показана ошибка | `ReportDrawnWhen` на главной → `reportFullyDrawn()` |
| `app_on_create_ms` | длительность `Application.onCreate`, включая инжект Hilt                                | `YummyTvApplication.onCreate()`                     |

Цепочка старта: `InterfaceRouterActivity` (трамплин) → `MobileActivity`/`TvActivity` → главная.
Время трамплина входит в TTID.

## Событие `app_startup`

| Параметр                     | Значения                                                                                 |
|------------------------------|------------------------------------------------------------------------------------------|
| `ui`                         | `mobile`, `tv`                                                                           |
| `entry`                      | `launcher`, `deeplink`, `other` (поиск, уведомление) — по Intent первой Activity         |
| `ttid_ms`, `ttfd_ms`         | миллисекунды                                                                             |
| `ttid_bucket`, `ttfd_bucket` | `<500`, `500-1000`, `1000-2000`, `2000-4000`, `4000+`                                    |
| `fully_drawn`                | `false`, если главная не дорисовалась (ушли с экрана, таймаут 30 с) — тогда `ttfd_*` нет |
| `ram_gb`                     | объём RAM, округлённый вверх                                                             |
| `api`                        | `Build.VERSION.SDK_INT`                                                                  |
| `start_type`                 | `cold`/`warm`/`hot` от системы — **только API 35+**                                      |
| `start_reason`               | `launcher`, `start_activity`, `push`, `job`, … от системы — **только API 35+**           |

Параметры AppMetrica — строки. Консоль группирует по значениям, поэтому для графиков удобнее
корзины `*_bucket`, а сырые `*_ms` — для выгрузки (Logs API) и перцентилей.

## Какие старты не попадают

Событие шлётся не больше одного раза на процесс и только для старта, который видит
пользователь. Замер отбрасывается, если:

- процесс поднят в фоне — WorkManager, пуш, поисковый провайдер
  (`importance != IMPORTANCE_FOREGROUND` в `Application.onCreate`);
- интерфейс ещё не выбран — первый запуск с диалогом выбора мерил бы сам диалог;
- главная Activity восстанавливается из `savedInstanceState` или создаётся позже чем через 5 с
  после `Application.onCreate`;
- первой открылась другая Activity;
- первый кадр не успел отрисоваться.

Тёплые и горячие старты (процесс уже жив) не меряются: `Application.onCreate` не вызывается.

До API 35 холодный старт определяется только этими эвристиками. На API 35+ `start_type` и
`start_reason` берутся из `ApplicationStartInfo` — по ним можно проверить, что эвристика не
пропускает фоновые подъёмы. На API 24–34 на реальных устройствах это не проверялось.

## Код

| Файл                                                    | Что делает                                                               |
|---------------------------------------------------------|--------------------------------------------------------------------------|
| `app/.../android/startup/StartupMetricsTracker.kt`      | собирает замер через `ActivityLifecycleCallbacks`, фильтрует, отправляет |
| `app/.../android/startup/StartupAnalytics.kt`           | формирует событие `app_startup`                                          |
| `app/.../android/startup/model/`                        | `StartupMetrics`, `StartupUi`, `StartupEntry`                            |
| `app/.../android/startup/utils/StartupMetricsUtils.kt`  | корзины, RAM, `ApplicationStartInfo` → строки                            |
| `feature/home/presentation/.../utils/HomeStateUtils.kt` | `isFirstScreenSettled()` — когда главная «готова»                        |
| `HomeMobileScreen.kt`, `HomeTvScreen.kt`                | `ReportDrawnWhen { state.isFirstScreenSettled() }`                       |

Трекер ставится в `YummyTvApplication.onCreate()` сразу после `super.onCreate()` (там Hilt
инжектит поля), а время начала берётся до него.

Если меняется то, что считается загруженной главной, правится только `isFirstScreenSettled()`.
Если главным экраном станет другой экран, `ReportDrawnWhen` нужно перенести туда. Иначе
`fully_drawn` всегда будет `false`.

## Trace-секции

Шаги `Application.onCreate` обёрнуты в `androidx.tracing.trace("App.*")`: `App.hiltInject`,
`App.analytics`, `App.featureToggles`, `App.cast`, `App.coil`, `App.watchedEpisodeRuleSync`,
`App.onlineStatus`, `App.featureToggleRefresh`, `App.schedulers`, `App.maintenance`.

- В Perfetto / System Trace видно, какой шаг занимает время.
- `StartupBenchmark` пишет `TraceSectionMetric("App.%", Mode.Sum)` — суммарное время секций —
  и, благодаря `reportFullyDrawn`, `timeToFullDisplayMs` рядом с `timeToInitialDisplayMs`.

Новый шаг старта оборачивать так же, с префиксом `App.`.

## Проверка локально

В debug события не уходят в AppMetrica, а пишутся в logcat (тег `Analytics`):

```bash
adb shell am force-stop su.afk.yummy.tv.debug
```

```bash
adb logcat -c && adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n su.afk.yummy.tv.debug/su.afk.yummy.tv.android.InterfaceRouterActivity
```

```bash
adb logcat -d | grep -E "app_startup|Fully drawn|Displayed"
```

Для сверки системные строки `Displayed` (TTID) и `Fully drawn` (TTFD) от `ActivityTaskManager`
считаются от Intent, а наши — от старта процесса. Поэтому наши значения на ~40–90 мс меньше.

## Замеры (25.09.2026)

Эмулятор Pixel 10 Pro XL, API 37, debug-сборка:

|                              |    TTID |    TTFD | Система: Displayed / Fully drawn |
|------------------------------|--------:|--------:|---------------------------------:|
| ТВ-интерфейс, лаунчер        | 1059 мс | 1236 мс |                   1140 / 1312 мс |
| Мобильный интерфейс, лаунчер | 1439 мс | 1817 мс |                   1483 / 1853 мс |

Горячий старт, поворот экрана, фоновый подъём процесса с последующим открытием и первый запуск
с диалогом события не дают. Проверено только на API 37.

## Известные ограничения

- Диплинк в детали: TTFD всё равно приходит, если главная отрисовалась под деталями. Для
  чистых цифр фильтровать `entry=launcher`.
- Цифры выше сняты на debug-сборке, где нет R8 и baseline profile. Ориентироваться на данные
  из release.
