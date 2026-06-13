plugins {
    id("yummytv.android.library")
}

android {
    namespace = "su.afk.yummy.tv.feature.player.common"
}

dependencies {
    implementation(libs.media3.exoplayer)
}
