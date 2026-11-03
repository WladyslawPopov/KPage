import SwiftUI
import SharedLogic

/// Layout of the empty screens of the skeleton; the title goes to the navigation bar.
struct PlaceholderScreen: View {
    let topBar: TopBarState
    let text: AppStringKey

    var body: some View {
        Text(text.text)
            .font(AppTypographyKey.headline.font)
            .foregroundStyle(AppColorKeyText.primary.color)
            .padding(AppDimenKeyPadding.large.value)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(AppColorKeyContainer.background.color)
            .navigationTitle(topBar.title.text)
    }
}
