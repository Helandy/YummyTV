plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.source.amedia.data"
}

dependencies {
    implementation(project(":source:amedia:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:player:domain"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.ktor.client.core)

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.unit.test.network)
}
