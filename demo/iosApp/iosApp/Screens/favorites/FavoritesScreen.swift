import SwiftUI
import SharedLogic

struct FavoritesScreen: View {
    let component: FavoritesComponent
    let state: FavoritesState

    var body: some View {
        PageLayout(
            page: state.page,
            onPageIntent: { component.onPageIntent(intent: $0) },
            onRefresh: { try? await component.pullToRefresh() }
        ) {
            PagedGrid(
                list: state.list,
                // Rows keep their own padding and a divider.
                spacing: AppDimenKeyPadding.none.value,
                itemKey: { $0.id },
                placeholder: { _ in ImageRow(image: nil) }
            ) { image, _ in
                row(image)
            }
        }
        .navigationTitle(state.isSelecting ? state.selectionTitle.string : state.topBar.title.text)
        .toolbar {
            // The selection mode: a cross leaves it, the bin asks to remove the chosen images.
            ToolbarItemGroup(placement: .topBarLeading) {
                if state.isSelecting {
                    Button { component.onIntent(intent: FavoritesIntentClearSelection.shared) } label: {
                        Image(systemName: AppIconKey.close.sfSymbol)
                    }
                    .accessibilityLabel(AppStringKeyCore.close.text)
                }
            }
            ToolbarItemGroup(placement: .topBarTrailing) {
                if state.isSelecting {
                    Button { component.onIntent(intent: FavoritesIntentDeleteSelected.shared) } label: {
                        Image(systemName: AppIconKey.trash.sfSymbol)
                    }
                    .accessibilityLabel(AppStringKeyFavorites.remove.text)
                }
            }
        }
        // The state decides when it is shown; the buttons answer through it, so the binding only reads.
        .alert(AppStringKeyFavorites.removeTitle.text, isPresented: .constant(!state.pendingDelete.isEmpty)) {
            Button(AppStringKeyFavorites.remove.text, role: .destructive) {
                component.onIntent(intent: FavoritesIntentConfirmDelete.shared)
            }
            Button(AppStringKeyCore.cancel.text, role: .cancel) {
                component.onIntent(intent: FavoritesIntentCancelDelete.shared)
            }
        } message: {
            Text(AppStringKeyFavorites.removeMessage.text)
        }
    }

    @ViewBuilder private func row(_ image: SpaceImage) -> some View {
        SwipeToRemove(
            isEnabled: !state.isSelecting,
            onRemove: { component.onIntent(intent: FavoritesIntentDeleteOne(id: image.id)) }
        ) {
            ImageRow(image: image) { image in
                if state.isSelecting {
                    Image(systemName: state.selected.contains(image.id) ? "checkmark.circle.fill" : "circle")
                        .font(.title2)
                        .foregroundStyle(
                            state.selected.contains(image.id)
                                ? AppColorKeyIcon.accent.color
                                : AppColorKeyIcon.secondary.color
                        )
                }
            }
            .contentShape(Rectangle())
            .onTapGesture { component.onIntent(intent: FavoritesIntentClick(id: image.id)) }
            .onLongPressGesture {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                component.onIntent(intent: FavoritesIntentLongClick(id: image.id))
            }
        }
    }
}
