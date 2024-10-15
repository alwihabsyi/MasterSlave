package com.demoapp.masterslave.di

import com.demoapp.masterslave.core.domain.usecase.client.ClientInteractor
import com.demoapp.masterslave.core.domain.usecase.client.ClientUseCase
import com.demoapp.masterslave.core.domain.usecase.slave.SlaveInteractor
import com.demoapp.masterslave.core.domain.usecase.slave.SlaveUseCase
import com.demoapp.masterslave.core.domain.usecase.video.VideoInteractor
import com.demoapp.masterslave.core.domain.usecase.video.VideoUseCase
import com.demoapp.masterslave.ui.master.MasterActivity
import com.demoapp.masterslave.ui.master.MasterViewModel
import com.demoapp.masterslave.ui.player.PlayerViewModel
import com.demoapp.masterslave.ui.slave.SlaveActivity
import com.demoapp.masterslave.ui.slave.SlaveViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val useCasesModule = module {
    scope<MasterActivity> {
        scoped<ClientUseCase> { ClientInteractor(get()) }
        scoped<VideoUseCase> { VideoInteractor(get()) }
    }
    scope<SlaveActivity> {
        scoped<SlaveUseCase> { SlaveInteractor(get()) }
    }
}

val viewModelModules = module {
    scope<MasterActivity> { viewModel { MasterViewModel(get(), get()) } }
    scope<SlaveActivity> { viewModel { SlaveViewModel(get()) } }
    single { PlayerViewModel() }
}