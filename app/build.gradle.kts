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
    flavorDimensions += "target"
    productFlavors {
        create("tv") {
            dimension = "target"
        }
        create("mobile") {
            dimension = "target"
            applicationIdSuffix = ".mobile"
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
            val fileName = "YummyTV-${variant.flavorName?.takeIf { it.isNotEmpty() }?.let { "$it-" } ?: ""}" +
                "${output.versionName.orNull ?: "1.0"}-${variant.buildType}.apk"
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)?.outputFileName?.set(fileName)
        }
    }
}

dependencies {
    implementation(project(":feature:main:api"))

    implementation(project(":core:network"))
    implementation(project(":core:storage"))
    implementation(project(":core:preferences"))
    implementation(project(":core:deeplink"))
    implementation(project(":core:tv-api"))
    implementation(project(":feature:details:data"))
    implementation(project(":feature:home:data"))
    implementation(project(":feature:top100:data"))
    implementation(project(":feature:search:data"))
    implementation(project(":feature:collection:data"))
    implementation(project(":feature:account:data"))
    implementation(project(":feature:schedule:data"))

    implementation(project(":feature:commonScreen:presenter"))
    implementation(project(":feature:home:domain"))

    add("tvImplementation", project(":feature:main:ui-tv"))
    add("tvImplementation", project(":core:tv"))
    add("tvImplementation", libs.androidx.tvprovider)
    add("tvImplementation", project(":feature:account:ui-tv"))
    add("tvImplementation", project(":feature:details:ui-tv"))
    add("tvImplementation", project(":feature:player:ui-tv"))
    add("tvImplementation", project(":feature:settings:ui-tv"))
    add("tvImplementation", project(":feature:collection:ui-tv"))
    add("tvImplementation", project(":feature:schedule:ui-tv"))

    add("mobileImplementation", project(":feature:main:ui-mobile"))
    add("mobileImplementation", project(":feature:account:ui-mobile"))
    add("mobileImplementation", project(":feature:details:ui-mobile"))
    add("mobileImplementation", project(":feature:player:ui-mobile"))
    add("mobileImplementation", project(":feature:settings:ui-mobile"))
    add("mobileImplementation", project(":feature:collection:ui-mobile"))
    add("mobileImplementation", project(":feature:schedule:ui-mobile"))

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
