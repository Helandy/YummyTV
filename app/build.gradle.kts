import su.afk.yummy.tv.buildlogic.buildConfigSecret

plugins {
    id("yummytv.android.application")
    id("yummytv.android.hilt")
    id("yummytv.android.application.baselineprofile")
}

val baseApplicationId = providers.gradleProperty("yummytv.applicationId").get()
val appVersionName = providers.gradleProperty("yummytv.versionName").get()
val appVersionCode = providers.gradleProperty("yummytv.versionCode").get().toInt()

android {
    namespace = "su.afk.yummy.tv"

    defaultConfig {
        applicationId = baseApplicationId
        targetSdk = libs.versions.android.compileSdk.get().toInt()

        versionName = appVersionName
        versionCode = appVersionCode

        // Только языки приложения: строки библиотек (play-services, media3, appcompat) на остальных не нужны
        androidResources {
            localeFilters += listOf("en", "ru", "uk")
        }

        buildConfigField("String", "APPMETRICA_API_KEY", buildConfigSecret("yummytv.appmetricaApiKey"))
        buildConfigField("String", "VARIOQUB_CLIENT_ID", buildConfigSecret("yummytv.varioqubClientId"))
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "search_suggest_authority", "$baseApplicationId.debug.search")
            isMinifyEnabled = false
        }

        release {
            resValue("string", "search_suggest_authority", "$baseApplicationId.search")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            isDebuggable = false
        }

        // Релизная сборка (R8 + shrinkResources), но подписанная debug-ключом —
        // чтобы можно было ставить и проверять результат минификации без релизного keystore.
        create("releaseDebug") {
            initWith(getByName("release"))
            applicationIdSuffix = ".releasedebug"
            resValue("string", "search_suggest_authority", "$baseApplicationId.releasedebug.search")
            signingConfig = signingConfigs.getByName("debug")
            // библиотечные модули этого типа не объявляют — берём их release-вариант
            matchingFallbacks += listOf("release")
        }
    }
    buildFeatures {
        buildConfig = true
        resValues = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val fileName = "YummyTV-${output.versionName.orNull ?: "1.0"}-${variant.buildType}.apk"
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)?.outputFileName?.set(fileName)
        }
    }

    // mapping.txt R8 лежит в build/outputs/mapping/<variant> и затирается следующей сборкой.
    // Копируем его в app/mappings/<версия>-<код>/<тип>/, чтобы mapping каждой версии сохранялся
    // отдельно и им можно было расшифровать крэши AppMetrica этой версии.
    onVariants { variant ->
        if (variant.buildType == "debug") return@onVariants
        val capitalized = variant.name.replaceFirstChar { it.uppercase() }
        val saveMapping = tasks.register<Copy>("save${capitalized}Mapping") {
            from(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.OBFUSCATION_MAPPING_FILE))
            into(layout.projectDirectory.dir("mappings/$appVersionName-$appVersionCode/${variant.name}"))
        }
        tasks.matching { it.name == "assemble$capitalized" || it.name == "bundle$capitalized" }
            .configureEach { finalizedBy(saveMapping) }
    }
}

dependencies {
    // Необязательный источник «4K»: нет каталога source/anime4k, нет и зависимости.
    findProject(":source:anime4k:data")?.let { implementation(it) }
    // Необязательный источник «AiLiberty» (нужен source/availability): нет каталога, нет и зависимости.
    findProject(":source:availability:data")?.let { implementation(it) }
    findProject(":source:ailiberty:data")?.let { implementation(it) }
    // Необязательный источник «AniDub» (нужен source/availability): нет каталога, нет и зависимости.
    findProject(":source:anidub:data")?.let { implementation(it) }
    // Общий разбор плеера Ashdi для AnimeON и Mikai: нет каталога source/ashdi, нет и зависимости.
    findProject(":source:ashdi:data")?.let { implementation(it) }
    // Общий разбор плеера Fenix для AnimeON и Mikai: нет каталога source/fenix, нет и зависимости.
    findProject(":source:fenix:data")?.let { implementation(it) }
    // Необязательный источник «AnimeON» (нужен source/availability): нет каталога, нет и зависимости.
    findProject(":source:animeon:data")?.let { implementation(it) }
    // Необязательный источник «AnimeVost» (нужен source/availability): нет каталога, нет и зависимости.
    findProject(":source:animevost:data")?.let { implementation(it) }
    // Необязательный источник «Mikai» (нужны source/availability, source/ashdi и source/fenix): нет каталога, нет и зависимости.
    findProject(":source:mikai:data")?.let { implementation(it) }
    // Необязательный источник «Animebesst» (нужен source/availability): нет каталога, нет и зависимости.
    findProject(":source:animebesst:data")?.let { implementation(it) }
    // Необязательный источник «Dream Cast» (нужен source/availability): нет каталога, нет и зависимости.
    findProject(":source:dreamerscast:data")?.let { implementation(it) }
    // Необязательный источник «AniBoom»: нет каталога source/aniboom, нет и зависимости.
    findProject(":source:aniboom:data")?.let { implementation(it) }
    // Необязательный источник «AniStar»: нет каталога source/anistar, нет и зависимости.
    findProject(":source:anistar:data")?.let { implementation(it) }
    // Необязательный источник «Amedia»: нет каталога source/amedia, нет и зависимости.
    findProject(":source:amedia:data")?.let { implementation(it) }
    // Необязательный источник «AniLibria»: нет каталога source/anilibria, нет и зависимости.
    findProject(":source:anilibria:data")?.let { implementation(it) }
    implementation(project(":core:analytics"))
    implementation(project(":core:deeplink"))
    implementation(project(":core:deeplink:api"))
    implementation(project(":core:designsystem"))
    // реализация обработки ошибок: биндинги нужны в графе, хотя фичи знают только core:error:api
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:network"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:utils"))
    implementation(project(":core:featuretoggle"))
    implementation(project(":core:tv"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:account:ui-mobile"))
    implementation(project(":feature:account:ui-tv"))
    implementation(project(":feature:account:data"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:collection:ui-mobile"))
    implementation(project(":feature:collection:ui-tv"))
    implementation(project(":feature:collection:data"))
    implementation(project(":feature:comments:api"))
    implementation(project(":feature:comments:ui-mobile"))
    implementation(project(":feature:comments:ui-tv"))
    implementation(project(":feature:comments:data"))
    implementation(project(":feature:reviews:api"))
    implementation(project(":feature:reviews:data"))
    implementation(project(":feature:reviews:domain"))
    implementation(project(":feature:reviews:ui-mobile"))
    implementation(project(":feature:reviews:ui-tv"))
    implementation(project(":feature:posts:api"))
    implementation(project(":feature:posts:data"))
    implementation(project(":feature:posts:ui-mobile"))
    implementation(project(":feature:posts:ui-tv"))
    implementation(project(":feature:messages:api"))
    implementation(project(":feature:messages:data"))
    implementation(project(":feature:messages:ui-mobile"))
    implementation(project(":feature:bloggers:api"))
    implementation(project(":feature:bloggers:data"))
    implementation(project(":feature:bloggers:ui-mobile"))
    implementation(project(":feature:bloggers:ui-tv"))
    implementation(project(":feature:commonScreen:presenter"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:details:ui-mobile"))
    implementation(project(":feature:details:ui-tv"))
    implementation(project(":feature:details:data"))
    implementation(project(":feature:faq:api"))
    implementation(project(":feature:faq:ui-mobile"))
    implementation(project(":feature:pages:api"))
    implementation(project(":feature:pages:data"))
    implementation(project(":feature:pages:ui-mobile"))
    implementation(project(":feature:home:api"))
    implementation(project(":feature:home:domain"))
    implementation(project(":feature:home:ui-mobile"))
    implementation(project(":feature:home:ui-tv"))
    implementation(project(":feature:home:data"))
    implementation(project(":feature:library:api"))
    implementation(project(":feature:library:data"))
    implementation(project(":feature:library:presentation"))
    implementation(project(":feature:library:ui-mobile"))
    implementation(project(":feature:library:ui-tv"))
    implementation(project(":feature:main:api"))
    implementation(project(":feature:main:ui-mobile"))
    implementation(project(":feature:main:ui-tv"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:player:ui-mobile"))
    implementation(project(":feature:player:ui-tv"))
    implementation(project(":feature:player:data"))
    implementation(project(":feature:schedule:api"))
    implementation(project(":feature:schedule:ui-mobile"))
    implementation(project(":feature:schedule:ui-tv"))
    implementation(project(":feature:schedule:data"))
    implementation(project(":feature:catalog:api"))
    implementation(project(":feature:catalog:android"))
    implementation(project(":feature:catalog:domain"))
    implementation(project(":feature:catalog:ui-mobile"))
    implementation(project(":feature:catalog:ui-tv"))
    implementation(project(":feature:catalog:data"))
    implementation(project(":feature:playersetup:api"))
    implementation(project(":feature:playersetup:ui-mobile"))
    implementation(project(":feature:playersetup:ui-tv"))
    implementation(project(":feature:settings:api"))
    implementation(project(":feature:settings:ui-mobile"))
    implementation(project(":feature:settings:ui-tv"))
    implementation(project(":feature:video-download:api"))
    implementation(project(":feature:video-download:data"))
    implementation(project(":feature:video-download:domain"))
    implementation(project(":feature:video-download:ui-mobile"))
    implementation(project(":feature:watch-later:api"))
    implementation(project(":feature:watch-later:data"))
    implementation(project(":feature:watch-later:domain"))
    implementation(project(":feature:watch-later:ui-mobile"))
    implementation(project(":feature:update:api"))
    implementation(project(":feature:update:data"))
    implementation(project(":feature:update:presentation"))
    implementation(project(":feature:update:ui"))

    implementation(libs.bundles.coil.full)
    implementation(libs.libavif.android)
    implementation(libs.ktor.client.okhttp)
    // manifest ссылается на классы Cast/mediarouter напрямую, implementation в feature:player их не пробрасывает
    implementation(libs.media3.cast)

    implementation(libs.androidx.tvprovider)
    implementation(libs.hilt.work)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.bundles.compose.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.work.runtime.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.glance.appwidget)
    implementation(libs.androidx.appcompat)
    // APK ставится мимо Play (самообновление) — без profileinstaller профиль не применится
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.tracing.ktx)
    baselineProfile(project(":baselineprofile"))
    add("ksp", libs.hilt.work.compiler)
}
