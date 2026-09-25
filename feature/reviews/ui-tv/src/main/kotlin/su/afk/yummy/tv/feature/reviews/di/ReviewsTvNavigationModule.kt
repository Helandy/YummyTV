package su.afk.yummy.tv.feature.reviews.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.reviews.ITvReviewsEntry
import su.afk.yummy.tv.feature.reviews.tv.navigator.ReviewsNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface ReviewsTvNavigationModule {

    @Binds
    fun bindReviewsNavRegistrar(impl: ReviewsNavRegistrar): ITvReviewsEntry
}
