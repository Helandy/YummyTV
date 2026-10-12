plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.core.storage"
}

// Схемы Room коммитятся в репозиторий: без них нельзя проверить миграцию относительно прошлой версии.
// `ksp {}` как типобезопасный аксессор тут недоступен (плагин применяет convention plugin), поэтому
// аргумент задаётся через расширение по имени.
extensions.getByName("ksp").withGroovyBuilder {
    "arg"("room.schemaLocation", "$projectDir/schemas")
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
