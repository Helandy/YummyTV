plugins {
    id("yummytv.android.library")
}

android {
    namespace = "su.afk.yummy.tv.domain.top"
}

dependencies {
    implementation(libs.javax.inject)
}

dependencies {
    testImplementation(libs.kotlin.test)
}
