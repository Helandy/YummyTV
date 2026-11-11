package su.afk.yummy.tv.source.torrent.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.core.model.settings.TorrServerChecker
import su.afk.yummy.tv.source.torrent.data.repository.TorrServerCheckerImpl
import su.afk.yummy.tv.source.torrent.data.repository.TorrServerRepositoryImpl
import su.afk.yummy.tv.source.torrent.data.repository.TorrentIndexerRepositoryImpl
import su.afk.yummy.tv.source.torrent.data.source.AndroidTorrentMessages
import su.afk.yummy.tv.source.torrent.data.source.AndroidVideoDecoderSupport
import su.afk.yummy.tv.source.torrent.data.source.VideoDecoderSupport
import su.afk.yummy.tv.source.torrent.data.source.TorrentExtraStreamRepository
import su.afk.yummy.tv.source.torrent.data.source.TorrentExtraVideoRepository
import su.afk.yummy.tv.source.torrent.data.source.TorrentMessages
import su.afk.yummy.tv.source.torrent.domain.repository.TorrServerRepository
import su.afk.yummy.tv.source.torrent.domain.repository.TorrentIndexerRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TorrentModule {

    @Binds
    @Singleton
    fun bindIndexerRepository(impl: TorrentIndexerRepositoryImpl): TorrentIndexerRepository

    @Binds
    @Singleton
    fun bindTorrServerRepository(impl: TorrServerRepositoryImpl): TorrServerRepository

    @Binds
    @Singleton
    fun bindTorrServerChecker(impl: TorrServerCheckerImpl): TorrServerChecker

    @Binds
    fun bindVideoDecoderSupport(impl: AndroidVideoDecoderSupport): VideoDecoderSupport

    @Binds
    fun bindMessages(impl: AndroidTorrentMessages): TorrentMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: TorrentExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: TorrentExtraStreamRepository): ExtraStreamRepository
}
