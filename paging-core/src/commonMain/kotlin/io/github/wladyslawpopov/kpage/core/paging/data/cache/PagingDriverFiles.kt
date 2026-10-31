package io.github.wladyslawpopov.kpage.core.paging.data.cache

import io.github.wladyslawpopov.kpage.core.paging.data.SCHEMA_VERSION

internal fun databaseFileName(name: String): String = "${name}_v$SCHEMA_VERSION.db"

/**
 * True for files of this cache that belong to another schema version, including SQLite sidecar
 * files (`-wal`, `-shm`, `-journal`). Unrelated files never match.
 */
internal fun isStaleDatabaseFile(fileName: String, name: String): Boolean {
    val match = Regex("^${Regex.escape(name)}_v(\\d+)\\.db(-wal|-shm|-journal)?$").matchEntire(fileName)
        ?: return false
    return match.groupValues[1] != SCHEMA_VERSION.toString()
}
