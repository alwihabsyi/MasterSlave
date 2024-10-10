package com.demoapp.masterslave.di

import com.demoapp.masterslave.core.domain.usecase.client.ClientInteractor
import com.demoapp.masterslave.core.domain.usecase.client.ClientUseCase
import com.demoapp.masterslave.core.domain.usecase.slave.SlaveInteractor
import com.demoapp.masterslave.core.domain.usecase.slave.SlaveUseCase
import com.demoapp.masterslave.core.domain.usecase.video.VideoInteractor
import com.demoapp.masterslave.core.domain.usecase.video.VideoUseCase
import com.demoapp.masterslave.presentation.master.MasterViewModel
import com.demoapp.masterslave.presentation.player.PlayerViewModel
import com.demoapp.masterslave.presentation.slave.SlaveViewModel
import org.koin.dsl.module

val useCasesModule = module {
    single<ClientUseCase> { ClientInteractor(get()) }
    single<VideoUseCase> { VideoInteractor(get()) }
    single<SlaveUseCase> { SlaveInteractor(get()) }
}

val viewModelModules = module {
    single { MasterViewModel(get(), get()) }
    single { SlaveViewModel(get()) }
    single { PlayerViewModel() }
}