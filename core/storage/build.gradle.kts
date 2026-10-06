plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.core.storage"
}

dependencies {
    implementation(project(":core:model"))
    // UserScopedCache скоупит ключи по текущему пользователю и языку контента
    implementation(project(":core:preferences"))
    implementation(project(":core:utils"))

    implementation(libs.bundles.room)

    add("ksp", libs.room.compiler)

    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
}
