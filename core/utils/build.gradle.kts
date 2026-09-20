plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.core.utils"
}

dependencies {
    implementation(project(":core:model"))

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.coil.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.encoding)
    // Только ради GoogleApiAvailability в CastSupport: у basement уже объявлены <queries>
    // для com.google.android.gms, поэтому проверка версии GMS работает и на API 30+.
    implementation(libs.play.services.base)

    testImplementation(libs.junit)
    testImplementation(libs.bundles.unit.test)
}
