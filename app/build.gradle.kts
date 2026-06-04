plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "su.afk.yummy.tv"

        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.compileSdk.get().toInt()

        versionName = libs.versions.appVersionName.get()
        versionCode = libs.versions.appVersionCode.get().toInt()

    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isShrinkResources = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
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
    implementation(project(":feature:main:api"))

    implementation(project(":core:network"))
    implementation(project(":core:navigation"))
    implementation(project(":core:storage"))
    implementation(project(":core:preferences"))
    implementation(project(":core:deeplink"))
    implementation(project(":core:tv-api"))
    implementation(project(":feature:details:data"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:home:data"))
    implementation(project(":feature:home:api"))
    implementation(project(":feature:top100:data"))
    implementation(project(":feature:top100:api"))
    implementation(project(":feature:search:data"))
    implementation(project(":feature:search:api"))
    implementation(project(":feature:collection:data"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:account:data"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:schedule:data"))
    implementation(project(":feature:schedule:api"))
    implementation(project(":feature:library:api"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:settings:api"))

    implementation(project(":feature:commonScreen:presenter"))
    implementation(project(":feature:home:domain"))

    implementation(project(":feature:main:ui-tv"))
    implementation(project(":core:tv"))
    implementation(libs.androidx.tvprovider)
    implementation(project(":feature:account:ui-tv"))
    implementation(project(":feature:details:ui-tv"))
    implementation(project(":feature:player:ui-tv"))
    implementation(project(":feature:settings:ui-tv"))
    implementation(project(":feature:collection:ui-tv"))
    implementation(project(":feature:schedule:ui-tv"))
    implementation(project(":feature:home:ui-tv"))
    implementation(project(":feature:search:ui-tv"))
    implementation(project(":feature:top100:ui-tv"))
    implementation(project(":feature:library:ui-tv"))

    implementation(project(":feature:main:ui-mobile"))
    implementation(project(":feature:account:ui-mobile"))
    implementation(project(":feature:details:ui-mobile"))
    implementation(project(":feature:player:ui-mobile"))
    implementation(project(":feature:settings:ui-mobile"))
    implementation(project(":feature:collection:ui-mobile"))
    implementation(project(":feature:schedule:ui-mobile"))
    implementation(project(":feature:home:ui-mobile"))
    implementation(project(":feature:search:ui-mobile"))
    implementation(project(":feature:top100:ui-mobile"))
    implementation(project(":feature:library:ui-mobile"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.work.runtime.ktx)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
}
