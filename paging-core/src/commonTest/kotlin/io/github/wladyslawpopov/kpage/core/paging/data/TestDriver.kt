package io.github.wladyslawpopov.kpage.core.paging.data

import app.cash.sqldelight.db.SqlDriver

/** Fresh in-memory database for every test, independent of the production driver. */
expect fun createTestDriver(): SqlDriver
