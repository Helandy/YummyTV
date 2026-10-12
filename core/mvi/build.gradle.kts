plugins {
    id("yummytv.android.library")
}

android {
    namespace = "su.afk.yummy.tv.core.mvi"
}

dependencies {
    api(project(":core:error:api"))

    api(libs.androidx.lifecycle.viewmodel)
    api(libs.kotlinx.coroutines.android)

    // Типизированная проверка ResponseException в userMessage(): без api, наружу не торчит.
    implementation(libs.ktor.client.core)
}
