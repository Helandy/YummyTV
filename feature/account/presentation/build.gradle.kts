plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.account.presentation"
}

dependencies {
    implementation(project(":feature:comments:api"))
    implementation(project(":core:analytics"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:logger"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:messages:api"))
    implementation(project(":feature:posts:api"))
    implementation(project(":feature:reviews:api"))
    implementation(project(":feature:home:domain"))
    implementation(project(":feature:collection:domain"))
    implementation(project(":feature:video-download:api"))

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
}
