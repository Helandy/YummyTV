plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.data.home"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:error:api"))
    implementation(project(":core:network"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:tv:api"))
    implementation(project(":core:utils"))
    implementation(project(":feature:home:domain"))

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.bundles.unit.test.network)
}
