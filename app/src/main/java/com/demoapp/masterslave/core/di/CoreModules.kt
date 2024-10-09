package com.demoapp.masterslave.core.di

import android.net.nsd.NsdManager
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity.NSD_SERVICE
import com.demoapp.masterslave.core.common.SharedState
import com.demoapp.masterslave.core.domain.repository.ClientRepository
import com.demoapp.masterslave.core.domain.repository.VideoRepository
import com.demoapp.masterslave.core.repository.ClientRepositoryImpl
import com.demoapp.masterslave.core.repository.VideoRepositoryImpl
import com.demoapp.masterslave.utils.directoryName
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File
import java.net.ServerSocket

val commonModule = module {
    single { SharedState() }
}

val nsdModule = module {
    single { androidContext().getSystemService(NSD_SERVICE) as NsdManager }
}

val serverSocketModule = module {
    single { ServerSocket(8989) }
}

val fileModule = module {
    single {
        val fileDir = File(Environment.getExternalStorageDirectory(), androidContext().directoryName())
        if (!fileDir.exists()) fileDir.mkdirs()
        fileDir
    }
}

val repositoriesModule = module {
    single<ClientRepository> { ClientRepositoryImpl(get(), get(), get()) }
    single<VideoRepository> { VideoRepositoryImpl(get()) }
}