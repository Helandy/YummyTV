plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.source.mikai.data"
}

dependencies {
    implementation(project(":source:ashdi:domain"))
    implementation(project(":source:fenix:domain"))
    implementation(project(":source:availability:domain"))
    implementation(project(":core:model"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:player:domain"))

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.unit.test)
}
