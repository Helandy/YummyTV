plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.data.library"
}

dependencies {
    implementation(project(":core:storage"))
    implementation(project(":feature:library:domain"))
}
