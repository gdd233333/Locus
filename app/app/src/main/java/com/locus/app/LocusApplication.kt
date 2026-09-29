package com.locus.app

import android.app.Application

class LocusApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
