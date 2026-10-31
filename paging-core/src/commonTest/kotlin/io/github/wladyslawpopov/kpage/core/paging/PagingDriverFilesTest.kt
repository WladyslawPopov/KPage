package io.github.wladyslawpopov.kpage.core.paging

import io.github.wladyslawpopov.kpage.core.paging.data.SCHEMA_VERSION
import io.github.wladyslawpopov.kpage.core.paging.data.cache.databaseFileName
import io.github.wladyslawpopov.kpage.core.paging.data.cache.isStaleDatabaseFile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PagingDriverFilesTest {

    @Test
    fun test_other_versions_and_their_sidecar_files_are_stale() {
        val other = SCHEMA_VERSION + 1
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            assertTrue(isStaleDatabaseFile("kpage_v$other.db$suffix", "kpage"))
            assertTrue(isStaleDatabaseFile("kpage_v0.db$suffix", "kpage"))
            assertFalse(isStaleDatabaseFile(databaseFileName("kpage") + suffix, "kpage"), "The current version is kept")
        }
        assertTrue(isStaleDatabaseFile("kpage_v1000.db", "kpage"))
    }

    @Test
    fun test_unrelated_files_are_never_stale() {
        listOf("kpage.db", "kpage_vX.db", "kpage_v0.db.bak", "other_v0.db", "my_kpage_v0.db", "kpage_v.db")
            .forEach { assertFalse(isStaleDatabaseFile(it, "kpage"), it) }
    }
}
