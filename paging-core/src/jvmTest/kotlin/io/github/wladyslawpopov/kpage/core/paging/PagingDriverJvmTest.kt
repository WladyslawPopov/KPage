package io.github.wladyslawpopov.kpage.core.paging

import app.cash.sqldelight.db.SqlDriver
import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.data.SCHEMA_VERSION
import io.github.wladyslawpopov.kpage.core.paging.data.cache.databaseFileName
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PagingDriverJvmTest {

    private lateinit var dir: File

    @BeforeTest
    fun setup() {
        dir = Files.createTempDirectory("kpage-driver-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun write(driver: SqlDriver) {
        PagingDatabase(driver).itemRawQueries.upsertItem("item", "id_1", """{"id":"id_1"}""")
    }

    private fun read(driver: SqlDriver) =
        PagingDatabase(driver).itemRawQueries.selectById("item", "id_1").executeAsOneOrNull()

    @Test
    fun test_data_survives_reopening_the_driver() {
        createPagingDriver(dir).also { write(it); it.close() }

        val reopened = createPagingDriver(dir)
        try {
            assertNotNull(read(reopened))
        } finally {
            reopened.close()
        }
    }

    @Test
    fun test_files_of_other_schema_versions_are_deleted() {
        createPagingDriver(dir).also { write(it); it.close() }
        val stale = listOf("kpage_v0.db", "kpage_v0.db-wal", "kpage_v${SCHEMA_VERSION + 1}.db")
        val unrelated = listOf("other.db", "kpage.db", "other_v0.db")
        (stale + unrelated).forEach { File(dir, it).writeText("x") }

        val reopened = createPagingDriver(dir)
        try {
            assertNotNull(read(reopened), "The current file must be left untouched")
        } finally {
            reopened.close()
        }

        val remaining = dir.list().orEmpty().filterNot { it.startsWith(databaseFileName("kpage")) }.toSet()
        assertEquals(unrelated.toSet(), remaining)
    }
}
