plugins {
    id("yummytv.android.library.compose")
}

android {
    namespace = "su.afk.yummy.tv.feature.account.common"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:account:presentation"))

    implementation(libs.bundles.compose.core)
    implementation(libs.kotlinx.collections.immutable)
}
