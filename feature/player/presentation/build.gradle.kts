plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.player.presentation"

    // Поведения источников пишут в android.util.Log — в JVM-тестах это no-op.
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    api(project(":core:preferences"))

    implementation(project(":core:analytics"))
    implementation(project(":core:deeplink:api"))
    implementation(project(":core:error:api"))
    implementation(project(":core:model"))
    api(project(":core:mvi"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:player:domain"))
    implementation(project(":feature:video-download:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.bundles.unit.test)
}
