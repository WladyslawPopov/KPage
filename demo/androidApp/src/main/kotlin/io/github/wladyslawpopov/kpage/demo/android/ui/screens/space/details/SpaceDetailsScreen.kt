package io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.page.PageLayout
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.details.parts.DetailsContent
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.details.parts.DetailsHero
import io.github.wladyslawpopov.kpage.demo.android.ui.screens.space.details.viewer.SpaceImageViewer
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.BarIconButton
import io.github.wladyslawpopov.kpage.demo.android.ui.widgets.rememberBackAction
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageIntent
import io.github.wladyslawpopov.kpage.demo.presentation.page.ToastType
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.SpaceDetailsComponent
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey

/**
 * The details of an image: the picture from the very top with the heart on it, the title and the
 * description, then the fields in cards. A field the list can be filtered by opens the list with it.
 * A tap on the picture opens it full screen; whether it is open is the state of this screen only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceDetailsScreen(component: SpaceDetailsComponent) {
    val state by component.state.collectAsState()
    val onIntent = component::onIntent
    var isViewerShown by rememberSaveable { mutableStateOf(false) }
    val display = state.display
    val fullPictureUrl = display?.fullPictureUrl
    if (isViewerShown && fullPictureUrl != null) {
        SpaceImageViewer(url = fullPictureUrl, onClose = { isViewerShown = false })
    }
    PageLayout(
        page = state.page,
        onPageIntent = component::onPageIntent,
        header = {
            // Only the back button: the picture shows from the top, under the status bar and this bar.
            TopAppBar(
                title = {},
                navigationIcon = {
                    BarIconButton(onClick = rememberBackAction(), modifier = Modifier.padding(start = AppDimenKey.Padding.SMALL.dp())) {
                        Icon(AppIconKey.BACK.vector(), contentDescription = AppStringKey.Core.BACK.asString())
                    }
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            DetailsHero(display, onIntent, onOpen = { isViewerShown = true })
            display?.let {
                DetailsContent(
                    display = it,
                    onIntent = onIntent,
                    onCopied = { component.onPageIntent(PageIntent.ShowToast(AppStringKey.Core.COPIED, ToastType.SUCCESS)) },
                )
            }
        }
    }
}
