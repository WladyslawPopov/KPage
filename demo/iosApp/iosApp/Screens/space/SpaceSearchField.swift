import SwiftUI
import SharedLogic

/// The search field in the list, under the title. It only looks like a field (the same details: the
/// magnifier, the hint or the text, a small clear button) but it is a button: a tap opens the search
/// mode, where `SpaceSearchBar` takes the place of the navigation bar. It scrolls away with the list.
struct SpaceSearchField: View {
    let text: String
    let onOpen: () -> Void
    let onClear: () -> Void

    var body: some View {
        HStack(spacing: AppDimenKeyPadding.small.value) {
            Button(action: onOpen) {
                HStack(spacing: AppDimenKeyPadding.small.value) {
                    Image(systemName: AppIconKey.search.sfSymbol)
                        .foregroundStyle(AppColorKeyIcon.secondary.color)
                    Text(text.isEmpty ? AppStringKeySpace.searchHint.text : text)
                        .foregroundStyle(text.isEmpty ? AppColorKeyText.secondary.color : AppColorKeyText.primary.color)
                        .lineLimit(1)
                    Spacer(minLength: 0)
                }
                .frame(minHeight: 40)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(AppStringKeyCore.search.text)
            if !text.isEmpty {
                SearchClearButton(action: onClear)
            }
        }
        .padding(.horizontal, AppDimenKeyPadding.medium.value)
        .glassBackground(in: Capsule())
        .padding(.horizontal, AppDimenKeyPadding.large.value)
        .padding(.vertical, AppDimenKeyPadding.small.value)
    }
}

/// The search mode: the real field with a keyboard, and a check mark that ends the mode (the text
/// stays). It is shown instead of the navigation bar, at its place.
///
/// `@FocusState` is a flag that says whether a field has the focus and lets the code take it or give
/// it back, like `FocusRequester` and `clearFocus` in Compose.
struct SpaceSearchBar: View {
    @Binding var text: String
    let onClose: () -> Void

    @FocusState private var isFocused: Bool

    var body: some View {
        HStack(spacing: AppDimenKeyPadding.small.value) {
            HStack(spacing: AppDimenKeyPadding.small.value) {
                Image(systemName: AppIconKey.search.sfSymbol)
                    .foregroundStyle(AppColorKeyIcon.secondary.color)
                TextField(AppStringKeySpace.searchHint.text, text: $text)
                    .focused($isFocused)
                    .submitLabel(.done)
                    .onSubmit { isFocused = false }
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                if !text.isEmpty {
                    SearchClearButton { text = "" }
                }
            }
            .padding(.horizontal, AppDimenKeyPadding.medium.value)
            .frame(minHeight: 40)
            .glassBackground(in: Capsule())
            Button {
                isFocused = false
                onClose()
            } label: {
                Image(systemName: AppIconKey.done.sfSymbol)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(AppColorKeyIcon.primary.color)
                    .frame(width: 40, height: 40)
                    .glassBackground(in: Circle(), interactive: true)
                    // The whole circle is the target; without it only the thin check mark reacts to a tap.
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(AppStringKeyCore.done.text)
        }
        .padding(.horizontal, AppDimenKeyPadding.large.value)
        .padding(.vertical, AppDimenKeyPadding.small.value)
        // The focus is taken after the view is on screen; set at once it is often ignored.
        .onAppear { DispatchQueue.main.async { isFocused = true } }
    }
}

/// A small cross on a round backing, smaller than the buttons of the bar.
private struct SearchClearButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: AppIconKey.close.sfSymbol)
                .font(.system(size: 9, weight: .bold))
                .foregroundStyle(AppColorKeyIcon.primary.color)
                .frame(width: 20, height: 20)
                .background(Circle().fill(AppColorKeyIcon.secondary.color.opacity(0.3)))
                .frame(width: 32, height: 32)
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(AppStringKeyCore.clear.text)
    }
}
