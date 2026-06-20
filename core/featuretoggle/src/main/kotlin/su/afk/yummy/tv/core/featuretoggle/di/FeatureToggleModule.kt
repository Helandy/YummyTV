package su.afk.yummy.tv.core.featuretoggle.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.featuretoggle.FeatureToggleInitializer
import su.afk.yummy.tv.core.featuretoggle.FeatureToggleProvider
import su.afk.yummy.tv.core.featuretoggle.FeatureToggleUpdateObserver
import su.afk.yummy.tv.core.featuretoggle.FeatureToggleVersionSupportChecker
import su.afk.yummy.tv.core.featuretoggle.VarioqubFeatureToggleInitializer
import su.afk.yummy.tv.core.featuretoggle.VarioqubFeatureToggleProvider
import su.afk.yummy.tv.core.featuretoggle.VarioqubFeatureToggleState
import su.afk.yummy.tv.core.featuretoggle.VersionSupportChecker
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FeatureToggleModule {

    @Binds
    @Singleton
    fun bindFeatureToggleInitializer(
        implementation: VarioqubFeatureToggleInitializer,
    ): FeatureToggleInitializer

    @Binds
    @Singleton
    fun bindFeatureToggleProvider(
        implementation: VarioqubFeatureToggleProvider,
    ): FeatureToggleProvider

    @Binds
    @Singleton
    fun bindVersionSupportChecker(
        implementation: FeatureToggleVersionSupportChecker,
    ): VersionSupportChecker

    @Binds
    @Singleton
    fun bindFeatureToggleUpdateObserver(
        implementation: VarioqubFeatureToggleState,
    ): FeatureToggleUpdateObserver
}
