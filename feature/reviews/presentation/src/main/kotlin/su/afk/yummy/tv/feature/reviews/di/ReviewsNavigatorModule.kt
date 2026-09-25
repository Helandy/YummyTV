package su.afk.yummy.tv.feature.reviews.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator
import su.afk.yummy.tv.feature.reviews.navigator.ReviewsNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ReviewsNavigatorModule {

    @Binds
    @Singleton
    fun bindReviewsNavigator(impl: ReviewsNavigator): IReviewsNavigator
}
