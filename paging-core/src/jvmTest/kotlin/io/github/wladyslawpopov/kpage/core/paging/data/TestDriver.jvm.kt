package io.github.wladyslawpopov.kpage.core.paging.data

import app.cash.sqldelight.db.SqlDriver
import io.github.wladyslawpopov.kpage.core.paging.createInMemoryPagingDriver

actual fun createTestDriver(): SqlDriver = createInMemoryPagingDriver()
