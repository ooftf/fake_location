package com.ooftf.fake.location

import android.app.Application
import com.baidu.mapapi.SDKInitializer

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        SDKInitializer.setApiKey("jORgd4QkxdcjvhWBFcGtHVjok9FILv6g")
        SDKInitializer.initialize(this)
    }
}