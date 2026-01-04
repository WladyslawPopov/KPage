package io.github.wladyslawpopov.kpager

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform