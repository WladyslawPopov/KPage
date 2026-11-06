import SwiftUI
import SharedLogic

struct SpaceScreen: View {
    let component: SpaceComponent
    let state: SpaceState

    var body: some View {
        PageLayout(page: state.page, onPageIntent: { component.onPageIntent(intent: $0) }) {
            PagedGrid(list: state.list, placeholder: { _ in TitleRow(title: nil) }) { image, _ in
                TitleRow(title: image.title)
            }
        }
        .navigationTitle(state.topBar.title.text)
    }
}

// Temporary row to check the page and the paginator; replaced by the cards.
private struct TitleRow: View {
    let title: String?

    var body: some View {
        Text(title ?? "…")
            .font(AppTypographyKey.body.font)
            .foregroundStyle(title == nil ? AppColorKeyText.muted.color : AppColorKeyText.primary.color)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(AppDimenKeyPadding.large.value)
    }
}
