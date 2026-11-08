package io.github.wladyslawpopov.kpage.demo.android.ui.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.color
import io.github.wladyslawpopov.kpage.demo.android.theme.dp
import io.github.wladyslawpopov.kpage.demo.android.theme.style
import io.github.wladyslawpopov.kpage.demo.android.theme.vector
import io.github.wladyslawpopov.kpage.demo.android.ui.barItemColor
import io.github.wladyslawpopov.kpage.demo.shared.strings.asString
import io.github.wladyslawpopov.kpage.demo.uicore.model.TopBarState
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppColorKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppDimenKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppIconKey
import io.github.wladyslawpopov.kpage.demo.uicore.theme.AppTypographyKey

/**
 * A [TopBar] of a tab root whose title turns into a search field: the icon opens it, the arrow (or
 * back) closes it and clears the text. The done key only hides the keyboard, because the list
 * follows the text by itself. The text lives in the screen state; whether the field is open is only
 * the state of the bar.
 *
 * @param hint The placeholder of the empty field: what can be typed.
 * @param text The text of the screen state; the bar shows what is typed, so it uses [text] only to
 * learn that it was cleared from outside.
 * @param actions Shown while the search is closed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    state: TopBarState,
    text: String,
    hint: AppStringKey,
    onTextChange: (String) -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    var isOpen by rememberSaveable { mutableStateOf(text.isNotEmpty()) }
    // The bar keeps the typed text itself, so a late echo of the state cannot overwrite a fast typist.
    var typed by rememberSaveable { mutableStateOf(text) }
    LaunchedEffect(text) { if (text.isEmpty()) typed = "" }
    // Only a tap on the icon raises the keyboard; a field that comes back after a rotation does not.
    var focusOnOpen by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val change = { value: String ->
        typed = value
        onTextChange(value)
    }
    val close = {
        focusManager.clearFocus()
        change("")
        isOpen = false
    }
    BackHandler(enabled = isOpen, onBack = close)

    TopAppBar(
        title = {
            AnimatedContent(
                targetState = isOpen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "searchTitle",
            ) { open ->
                if (open) {
                    SearchField(
                        text = typed,
                        hint = hint,
                        onTextChange = change,
                        requestFocus = focusOnOpen,
                        onFocusRequested = { focusOnOpen = false },
                    )
                } else {
                    Text(
                        text = state.title.asString(),
                        style = AppTypographyKey.TITLE.style(),
                        modifier = Modifier
                            .background(barItemColor(), CircleShape)
                            .padding(horizontal = AppDimenKey.Padding.LARGE.dp(), vertical = AppDimenKey.Padding.SMALL.dp()),
                    )
                }
            }
        },
        navigationIcon = {
            AnimatedVisibility(
                visible = isOpen,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                BarIconButton(onClick = close) {
                    Icon(AppIconKey.BACK.vector(), contentDescription = AppStringKey.Core.CLOSE.asString())
                }
            }
        },
        actions = {
            if (isOpen) {
                if (typed.isNotEmpty()) {
                    BarIconButton(onClick = { change("") }) {
                        Icon(AppIconKey.CLOSE.vector(), contentDescription = AppStringKey.Core.CLEAR.asString())
                    }
                }
            } else {
                BarIconButton(onClick = {
                    focusOnOpen = true
                    isOpen = true
                }) {
                    Icon(AppIconKey.SEARCH.vector(), contentDescription = AppStringKey.Core.SEARCH.asString())
                }
                actions()
            }
        },
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SearchField(
    text: String,
    hint: AppStringKey,
    onTextChange: (String) -> Unit,
    requestFocus: Boolean,
    onFocusRequested: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onFocusRequested()
        }
    }
    // The cursor belongs to the keyboard: when the keyboard goes away by its own button or by back,
    // the field loses the focus too.
    val isImeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    var wasImeVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && wasImeVisible) focusManager.clearFocus()
        wasImeVisible = isImeVisible
    }
    val style = AppTypographyKey.SUBTITLE.style()
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        singleLine = true,
        textStyle = style.copy(color = AppColorKey.Text.PRIMARY.color()),
        cursorBrush = SolidColor(AppColorKey.Border.FOCUSED.color()),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        decorationBox = { field ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barItemColor(), CircleShape)
                    .padding(horizontal = AppDimenKey.Padding.LARGE.dp(), vertical = AppDimenKey.Padding.MEDIUM.dp()),
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = hint.asString(),
                        style = style,
                        color = AppColorKey.Text.SECONDARY.color(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                field()
            }
        },
    )
}
