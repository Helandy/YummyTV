plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.player.extractor"

    // Общие помощники тестов балансеров (FakePlayerHttpClient и др.) для модулей source/<балансер>/data.
    testFixtures {
        enable = true
    }
}

dependencies {
    api(project(":feature:player:domain"))

    implementation(project(":core:analytics"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:utils"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.ktor.client.core)

    testImplementation(project(":core:testing"))
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.encoding)
    testImplementation(libs.org.json)

    testFixturesApi(project(":feature:player:domain"))
    testFixturesApi(libs.mockk)
}
