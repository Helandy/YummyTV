import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val desugarJdkLibs = libsCatalog.findLibrary("desugar-jdk-libs").get()

subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension>("android") {
            compileOptions {
                isCoreLibraryDesugaringEnabled = true
            }
        }
        dependencies.add("coreLibraryDesugaring", desugarJdkLibs)
    }

    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension>("android") {
            compileOptions {
                isCoreLibraryDesugaringEnabled = true
            }
        }
        dependencies.add("coreLibraryDesugaring", desugarJdkLibs)
    }
}
