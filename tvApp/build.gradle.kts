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
            signingConfig = signingConfigs.getByName("debug")
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

dependencies {
    implementation(project(":feature:main:api"))
    implementation(project(":feature:main:ui-tv"))

    implementation(project(":core:network"))
    implementation(project(":core:storage"))
    implementation(project(":core:deeplink"))
    implementation(project(":core:tv"))
    implementation(project(":feature:details:data"))
    implementation(project(":feature:home:data"))
    implementation(project(":feature:top100:data"))
    implementation(project(":feature:search:data"))
    implementation(project(":feature:collection:data"))

    implementation(project(":feature:commonScreen:presenter"))
    implementation(project(":feature:details:ui-tv"))
    implementation(project(":feature:player:ui-tv"))
    implementation(project(":feature:settings:ui-tv"))
    implementation(project(":feature:collection:ui-tv"))
    implementation(project(":feature:home:domain"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)

    implementation(libs.androidx.tvprovider)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
}
