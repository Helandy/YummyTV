package su.afk.yummy.tv.feature.reviews.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.reviews.IMobileReviewsEntry
import su.afk.yummy.tv.feature.reviews.mobile.navigator.ReviewsNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface ReviewsMobileNavigationModule {

    @Binds
    fun bindReviewsNavRegistrar(impl: ReviewsNavRegistrar): IMobileReviewsEntry
}
