package su.afk.yummy.tv.data.reviews.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.data.reviews.repository.YaniReviewsRepository
import su.afk.yummy.tv.domain.reviews.repository.ReviewsRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class ReviewsDataModule {
    @Binds
    abstract fun bindReviewsRepository(impl: YaniReviewsRepository): ReviewsRepository
}
