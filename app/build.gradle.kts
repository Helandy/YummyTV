plugins {
    id("yummytv.android.application")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

fun String.toBuildConfigString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val baseApplicationId = providers.gradleProperty("yummytv.applicationId").get()
val appVersionName = providers.gradleProperty("yummytv.versionName").get()
val appVersionCode = providers.gradleProperty("yummytv.versionCode").get().toInt()
val appmetricaApiKey = providers.gradleProperty("yummytv.appmetricaApiKey").get()
val varioqubClientId = providers.gradleProperty("yummytv.varioqubClientId").get()

android {
    namespace = "su.afk.yummy.tv"

    defaultConfig {
        applicationId = baseApplicationId
        targetSdk = libs.versions.android.compileSdk.get().toInt()

        versionName = appVersionName
        versionCode = appVersionCode

        buildConfigField("String", "APPMETRICA_API_KEY", appmetricaApiKey.toBuildConfigString())
        buildConfigField("String", "VARIOQUB_CLIENT_ID", varioqubClientId.toBuildConfigString())
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "search_suggest_authority", "$baseApplicationId.debug.search")
            isMinifyEnabled = false
            isShrinkResources = false
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
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:deeplink"))
    implementation(project(":core:navigation"))
    implementation(project(":core:network"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:featuretoggle"))
    implementation(project(":core:update"))
    implementation(project(":core:tv:tv-impl"))
    implementation(project(":core:tv:tv-api"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:account:ui-mobile"))
    implementation(project(":feature:account:ui-tv"))
    implementation(project(":feature:account:data"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:collection:ui-mobile"))
    implementation(project(":feature:collection:ui-tv"))
    implementation(project(":feature:collection:data"))
    implementation(project(":feature:comments:api"))
    implementation(project(":feature:comments:ui-mobile"))
    implementation(project(":feature:comments:data"))
    implementation(project(":feature:commonScreen:presenter"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:details:ui-mobile"))
    implementation(project(":feature:details:ui-tv"))
    implementation(project(":feature:details:data"))
    implementation(project(":feature:home:api"))
    implementation(project(":feature:home:domain"))
    implementation(project(":feature:home:ui-mobile"))
    implementation(project(":feature:home:ui-tv"))
    implementation(project(":feature:home:data"))
    implementation(project(":feature:library:api"))
    implementation(project(":feature:library:data"))
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
    implementation(project(":feature:search:api"))
    implementation(project(":feature:search:domain"))
    implementation(project(":feature:search:ui-mobile"))
    implementation(project(":feature:search:ui-tv"))
    implementation(project(":feature:search:data"))
    implementation(project(":feature:settings:api"))
    implementation(project(":feature:settings:ui-mobile"))
    implementation(project(":feature:settings:ui-tv"))
    implementation(project(":feature:top:api"))
    implementation(project(":feature:top:ui-mobile"))
    implementation(project(":feature:top:ui-tv"))
    implementation(project(":feature:top:data"))

    implementation(libs.bundles.coil.full)

    implementation(libs.androidx.tvprovider)
    implementation(libs.hilt.work)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.work.runtime.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    add("ksp", libs.hilt.work.compiler)
}
