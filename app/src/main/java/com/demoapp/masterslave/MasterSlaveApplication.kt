package com.demoapp.masterslave

import android.app.Application
import com.demoapp.masterslave.core.di.commonModule
import com.demoapp.masterslave.core.di.fileModule
import com.demoapp.masterslave.core.di.nsdModule
import com.demoapp.masterslave.core.di.repositoriesModule
import com.demoapp.masterslave.core.di.serverSocketModule
import com.demoapp.masterslave.di.useCasesModule
import com.demoapp.masterslave.di.viewModelModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MasterSlaveApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.NONE)
            androidContext(this@MasterSlaveApplication)
            modules(listOf(
                commonModule,
                nsdModule,
                serverSocketModule,
                fileModule,
                repositoriesModule,
                useCasesModule,
                viewModelModules
            ))
        }
    }
}