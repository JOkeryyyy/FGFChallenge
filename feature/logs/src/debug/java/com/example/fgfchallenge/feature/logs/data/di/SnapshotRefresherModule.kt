package com.example.fgfchallenge.feature.logs.data.di

import com.example.fgfchallenge.feature.logs.data.repository.RemoteSnapshotRefresher
import com.example.fgfchallenge.feature.logs.data.repository.SnapshotRefresher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Selects the remote snapshot refresher for the shipping-style `debug` build variant.
 *
 * The `debug`, `release`, and `benchmark` source sets each declare a module with this name, and
 * exactly one of them is compiled into any given variant. That is what keeps the benchmark fixture
 * out of every shipping build without a runtime flag.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SnapshotRefresherModule {
    @Binds
    @Singleton
    abstract fun bindSnapshotRefresher(refresher: RemoteSnapshotRefresher): SnapshotRefresher
}
