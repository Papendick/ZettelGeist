package de.zettelgeist.app

import android.app.Application
import de.zettelgeist.app.data.db.AppDatabase

class ZettelGeistApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
    }
}
