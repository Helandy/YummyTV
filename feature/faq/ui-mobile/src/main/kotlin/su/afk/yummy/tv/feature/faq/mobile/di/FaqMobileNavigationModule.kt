package su.afk.yummy.tv.feature.faq.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.faq.IFaqNavigator
import su.afk.yummy.tv.feature.faq.IMobileFaqEntry
import su.afk.yummy.tv.feature.faq.mobile.navigator.FaqNavRegistrar
import su.afk.yummy.tv.feature.faq.navigator.FaqNavigator
import javax.inject.Singleton

// У FAQ нет presentation-модуля, поэтому навигатор биндится здесь же, рядом с регистратором.
@Module
@InstallIn(SingletonComponent::class)
interface FaqMobileNavigationModule {

    @Binds
    fun bindFaqNavRegistrar(impl: FaqNavRegistrar): IMobileFaqEntry

    @Binds
    @Singleton
    fun bindFaqNavigator(impl: FaqNavigator): IFaqNavigator
}
