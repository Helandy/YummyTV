package su.afk.yummy.tv.core.update.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.update.nav.UpdateNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface UpdateNavModule {

    @Binds
    @IntoSet
    fun bindUpdateNavRegistrar(impl: UpdateNavRegistrar): NavRegistrar
}
