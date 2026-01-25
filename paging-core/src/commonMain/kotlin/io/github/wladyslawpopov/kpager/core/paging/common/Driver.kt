package io.github.wladyslawpopov.kpager.core.paging.common

import app.cash.sqldelight.db.SqlDriver

expect fun getDriver(): SqlDriver
