plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.source.kodik.data"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:common"))
    implementation(project(":core:utils"))
    implementation(project(":feature:player:domain"))
    implementation(project(":feature:player:extractor"))

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":core:testing"))
    testImplementation(testFixtures(project(":feature:player:extractor")))
    testImplementation(libs.org.json)
}
