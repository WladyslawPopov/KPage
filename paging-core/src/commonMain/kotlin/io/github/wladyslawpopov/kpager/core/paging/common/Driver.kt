package io.github.wladyslawpopov.kpager.core.paging.common

import app.cash.sqldelight.db.SqlDriver

internal expect fun getDriver(): SqlDriver
