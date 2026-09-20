plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.core.preferences"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:analytics"))
    implementation(project(":core:utils"))

    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.bundles.unit.test)
}
