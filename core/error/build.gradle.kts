plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.core.error"
}

dependencies {
    implementation(project(":core:analytics"))
    api(project(":core:model"))

    implementation(project(":core:navigation"))
    implementation(project(":feature:commonScreen:api"))
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)

    debugImplementation(libs.compose.uiTooling)
}
