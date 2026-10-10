plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.data.player"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:storage"))
    implementation(project(":core:utils"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:player:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)

    testImplementation(project(":core:testing"))
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.encoding)
    testImplementation(libs.org.json)
}
