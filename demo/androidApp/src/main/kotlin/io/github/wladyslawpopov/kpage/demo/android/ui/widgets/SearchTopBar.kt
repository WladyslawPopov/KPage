package io.github.wladyslawpopov.kpage.demo.android.ui.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
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
 * Whether the field of a [SearchTopBar] is open. It is held outside the bar, so what else shows the
 * search (a chip with the text) can open it too. Only the open flag survives a rotation.
 */
@Stable
class SearchBarState(initiallyOpen: Boolean = false) {
    var isOpen by mutableStateOf(initiallyOpen)
        private set

    /** True after [open]: the field raises the keyboard once; a field that comes back after a rotation does not. */
    var requestFocus by mutableStateOf(false)
        private set

    fun open() {
        requestFocus = true
        isOpen = true
    }

    fun close() {
        isOpen = false
    }

    fun focusRequested() {
        requestFocus = false
    }

    companion object {
        val Saver = Saver<SearchBarState, Boolean>(save = { it.isOpen }, restore = { SearchBarState(it) })
    }
}

private val CLEAR_TARGET = 32.dp
private val CLEAR_SIZE = 20.dp
private const val CLEAR_ALPHA = 0.3f

@Composable
fun rememberSearchBarState(): SearchBarState = rememberSaveable(saver = SearchBarState.Saver) { SearchBarState() }

/**
 * A [TopBar] of a tab root whose title turns into a search field: the icon opens it, the check mark
 * in the actions (or back) closes it. Closing keeps the text, so the screen can show it as a chip that
 * opens the field again; the small cross inside the field empties the text. The done key only hides the
 * keyboard, because the list follows the text by itself. The text lives in the screen state;
 * whether the field is open is [searchBar].
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
    searchBar: SearchBarState,
    text: String,
    hint: AppStringKey,
    onTextChange: (String) -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val isOpen = searchBar.isOpen
    // The bar keeps the typed text itself, so a late echo of the state cannot overwrite a fast typist.
    var typed by rememberSaveable { mutableStateOf(text) }
    LaunchedEffect(text) { if (text.isEmpty()) typed = "" }

    val focusManager = LocalFocusManager.current
    val change = { value: String ->
        typed = value
        onTextChange(value)
    }
    val close = {
        focusManager.clearFocus()
        searchBar.close()
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
                        requestFocus = searchBar.requestFocus,
                        onFocusRequested = searchBar::focusRequested,
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
        actions = {
            if (isOpen) {
                BarIconButton(onClick = close) {
                    Icon(AppIconKey.DONE.vector(), contentDescription = AppStringKey.Core.DONE.asString())
                }
            } else {
                BarIconButton(onClick = searchBar::open) {
                    Icon(AppIconKey.SEARCH.vector(), contentDescription = AppStringKey.Core.SEARCH.asString())
                }
                actions()
            }
            // With the 4 dp of the bar and the 4 dp of the button, the edge gap matches the 16 dp at the start.
            Spacer(Modifier.width(AppDimenKey.Padding.SMALL.dp()))
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
    // The cursor starts at the end of the text, so a field opened from the chip continues what was typed.
    var field by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    // The text was emptied from outside (the clear button): the field follows.
    LaunchedEffect(text) {
        if (text != field.text) field = TextFieldValue(text, TextRange(text.length))
    }
    BasicTextField(
        value = field,
        onValueChange = {
            field = it
            if (it.text != text) onTextChange(it.text)
        },
        singleLine = true,
        textStyle = style.copy(color = AppColorKey.Text.PRIMARY.color()),
        cursorBrush = SolidColor(AppColorKey.Border.FOCUSED.color()),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        // The end gap keeps the field off the check mark next to it.
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = AppDimenKey.Padding.SMALL.dp())
            .focusRequester(focusRequester),
        decorationBox = { input ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barItemColor(), CircleShape)
                    .padding(start = AppDimenKey.Padding.LARGE.dp(), end = AppDimenKey.Padding.SMALL.dp())
                    .padding(vertical = AppDimenKey.Padding.SMALL.dp()),
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (field.text.isEmpty()) {
                        Text(
                            text = hint.asString(),
                            style = style,
                            color = AppColorKey.Text.SECONDARY.color(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    input()
                }
                if (field.text.isNotEmpty()) {
                    // A small cross on a round backing, smaller than the buttons of the bar.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(CLEAR_TARGET)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onTextChange("") },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(CLEAR_SIZE)
                                .background(AppColorKey.Icon.SECONDARY.color().copy(alpha = CLEAR_ALPHA), CircleShape),
                        ) {
                            Icon(
                                imageVector = AppIconKey.CLOSE.vector(),
                                contentDescription = AppStringKey.Core.CLEAR.asString(),
                                tint = AppColorKey.Icon.PRIMARY.color(),
                                modifier = Modifier.size(AppDimenKey.IconSize.EXTRA_SMALL.dp()),
                            )
                        }
                    }
                }
            }
        },
    )
}
