package com.synapse.social.studioasinc.shared.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import org.koin.dsl.module

actual val storageDriverModule = module {
    single<SqlDriver> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        StorageDatabase.Schema.create(driver)
        driver
    }
}
