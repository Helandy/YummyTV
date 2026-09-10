plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(project(":core:model"))
    implementation(libs.javax.inject)
    // чистый JVM-модуль без Compose convention-плагина: BOM нужен явно,
    // версии compose-артефактов в каталоге не указаны
    compileOnly(platform(libs.androidx.compose.bom))
    compileOnly(libs.compose.runtime)
}
