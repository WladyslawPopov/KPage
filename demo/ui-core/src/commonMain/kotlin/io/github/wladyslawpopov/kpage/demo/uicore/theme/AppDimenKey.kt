package io.github.wladyslawpopov.kpage.demo.uicore.theme

sealed interface AppDimenKey {
    enum class Padding : AppDimenKey { NONE, EXTRA_SMALL, SMALL, MEDIUM, LARGE, EXTRA_LARGE }

    enum class CornerRadius : AppDimenKey { NONE, SMALL, MEDIUM, LARGE, FULL }

    enum class IconSize : AppDimenKey { EXTRA_SMALL, SMALL, MEDIUM, LARGE, EXTRA_LARGE }
}
