package com.demoapp.masterslave.core.di

import android.net.nsd.NsdManager
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity.NSD_SERVICE
import com.demoapp.masterslave.core.common.SharedState
import com.demoapp.masterslave.core.data.nsd.NsdService
import com.demoapp.masterslave.core.data.repository.ClientRepositoryImpl
import com.demoapp.masterslave.core.data.repository.SlaveRepositoryImpl
import com.demoapp.masterslave.core.data.repository.VideoRepositoryImpl
import com.demoapp.masterslave.core.data.socket.BaseSocketService
import com.demoapp.masterslave.core.data.socket.MasterSocketService
import com.demoapp.masterslave.core.data.socket.SlaveSocketService
import com.demoapp.masterslave.core.domain.repository.ClientRepository
import com.demoapp.masterslave.core.domain.repository.SlaveRepository
import com.demoapp.masterslave.core.domain.repository.VideoRepository
import com.demoapp.masterslave.core.utils.directoryName
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File
import java.net.ServerSocket

val commonModule = module {
    single { SharedState() }
}

val serverSocketModule = module {
    single { ServerSocket(8989) }
}

val fileModule = module {
    single {
        val fileDir = if (Build.VERSION.SDK_INT <= M) {
            File(androidContext().externalCacheDir, androidContext().directoryName())
        } else {
            File(Environment.getExternalStorageDirectory(), androidContext().directoryName())
        }

        if (!fileDir.exists()) fileDir.mkdirs()
        fileDir
    }
}

val socketModule = module {
    single { BaseSocketService() }
    single { MasterSocketService(get(), get()) }
    single { SlaveSocketService(get()) }
}

val nsdModule = module {
    single { androidContext().getSystemService(NSD_SERVICE) as NsdManager }
    single { NsdService(get()) }
}

val repositoriesModule = module {
    single<ClientRepository> { ClientRepositoryImpl(get(), get()) }
    single<VideoRepository> { VideoRepositoryImpl(get(), androidContext()) }
    single<SlaveRepository> { SlaveRepositoryImpl(get(), get()) }
}