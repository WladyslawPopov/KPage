import SwiftUI
import SharedLogic

struct SpaceScreen: View {
    let component: SpaceComponent
    let state: SpaceState

    // The field keeps the typed text itself: the state comes back from Kotlin a moment later, and a
    // late echo must not overwrite a fast typist.
    @State private var query: String
    @State private var isFiltersShown = false
    @State private var isSearchFieldVisible = true
    // The search mode: a real field and its buttons replace the navigation bar.
    @State private var isSearchMode = false

    init(component: SpaceComponent, state: SpaceState) {
        self.component = component
        self.state = state
        _query = State(initialValue: state.searchText)
    }

    private var isGrid: Bool { state.layout == .grid }

    private var searchText: String { query.trimmingCharacters(in: .whitespacesAndNewlines) }

    // The search field scrolls away with the list. Then a button in the toolbar, or a chip with the
    // text when something is typed, takes its place.
    private var isSearchAway: Bool { !isSearchFieldVisible && !isSearchMode }
    private var showsSearchButton: Bool { isSearchAway && searchText.isEmpty }
    private var showsSearchChip: Bool { isSearchAway && !searchText.isEmpty }
    private var showsChips: Bool { state.isFiltered || showsSearchChip }

    var body: some View {
        // The empty state is drawn by the list, under the search field, so the field stays.
        PageLayout(page: state.page, onPageIntent: { component.onPageIntent(intent: $0) }, showsEmpty: false) {
            PagedGrid(
                list: state.list,
                columns: isGrid ? 2 : 1,
                // List rows keep their own padding and a divider.
                spacing: isGrid ? AppDimenKeyPadding.medium.value : AppDimenKeyPadding.none.value,
                empty: state.page.empty,
                onEmptyAction: { component.onPageIntent(intent: $0) },
                header: {
                    // Gone while the search mode is open; back when it ends.
                    if !isSearchMode {
                        SpaceSearchField(text: query, onOpen: openSearch, onClear: { query = "" })
                            .onScrollVisibilityChange(threshold: 0.1) { visible in
                                withAnimation(.easeInOut(duration: 0.25)) { isSearchFieldVisible = visible }
                            }
                    }
                },
                placeholder: { _ in card(nil) }
            ) { image, _ in
                card(image)
            }
        }
        // The search bar takes the place of the navigation bar, which is hidden meanwhile; the chips are under it.
        .safeAreaInset(edge: .top, spacing: 0) {
            VStack(spacing: 0) {
                if isSearchMode {
                    SpaceSearchBar(text: $query, onClose: closeSearch)
                        .transition(.move(edge: .top).combined(with: .opacity))
                }
                if showsChips {
                    SpaceFilterChips(
                        query: state.list.query,
                        searchText: showsSearchChip ? searchText : nil,
                        onOpenSearch: openSearch,
                        onClearSearch: { query = "" },
                        onOpenFilters: { isFiltersShown = true },
                        onIntent: { component.onIntent(intent: $0) }
                    )
                    .transition(.move(edge: .top).combined(with: .opacity))
                }
            }
        }
        .toolbar(isSearchMode ? .hidden : .visible, for: .navigationBar)
        .animation(.easeInOut(duration: 0.25), value: showsChips)
        .animation(.easeInOut(duration: 0.25), value: isSearchMode)
        .navigationTitle(state.topBar.title.text)
        .onChange(of: query) { _, text in component.onIntent(intent: SpaceIntentSearchChanged(text: text)) }
        // The text cleared from outside (the Reset button of the empty state).
        .onChange(of: state.searchText) { _, text in if text.isEmpty { query = "" } }
        .sheet(isPresented: $isFiltersShown) {
            SpaceFiltersSheet(state: state) { component.onIntent(intent: $0) }
        }
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                if showsSearchButton {
                    Button(action: openSearch) {
                        Image(systemName: AppIconKey.search.sfSymbol)
                    }
                    .accessibilityLabel(AppStringKeyCore.search.text)
                    .transition(.opacity.combined(with: .scale))
                }
                Button { isFiltersShown = true } label: {
                    Image(systemName: AppIconKey.filter.sfSymbol)
                }
                .accessibilityLabel(AppStringKeyCore.filters.text)
                // Shows the layout a tap switches to.
                Button { component.onIntent(intent: SpaceIntentToggleLayout.shared) } label: {
                    Image(systemName: (isGrid ? AppIconKey.layoutList : AppIconKey.layoutGrid).sfSymbol)
                }
            }
        }
    }

    private func openSearch() { isSearchMode = true }

    private func closeSearch() { isSearchMode = false }

    @ViewBuilder private func card(_ image: SpaceImage?) -> some View {
        let onFavorite = {
            if let image { component.onIntent(intent: SpaceIntentSetFavorite(id: image.id, isFavorite: !image.isFavorite)) }
        }
        if isGrid {
            ImageTile(image: image, onFavorite: onFavorite)
        } else {
            ImageRow(image: image, onFavorite: onFavorite)
        }
    }
}

/// A list card with a divider under the texts; a nil image is the placeholder of the same size.
private struct ImageRow: View {
    let image: SpaceImage?
    let onFavorite: () -> Void

    private let thumbnail: CGFloat = 72
    private let gap = AppDimenKeyPadding.medium.value
    private let start = AppDimenKeyPadding.large.value

    var body: some View {
        VStack(spacing: 0) {
            row
            // From the start of the texts to the end of the screen.
            Divider().padding(.leading, start + thumbnail + gap)
        }
    }

    private var row: some View {
        HStack(spacing: gap) {
            Group {
                if let image {
                    RemoteImage(url: image.previewUrl)
                } else {
                    AppColorKeyContainer.secondary.color
                }
            }
            .frame(width: thumbnail, height: thumbnail)
            .clipShape(RoundedRectangle(cornerRadius: AppDimenKeyCornerRadius.medium.value))

            VStack(alignment: .leading, spacing: AppDimenKeyPadding.extraSmall.value) {
                ImageTexts(image: image)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // Keeps the text width of a placeholder equal to a card.
            Heart(isFavorite: image?.isFavorite ?? false, onClick: onFavorite)
                .opacity(image == nil ? 0 : 1)
                .disabled(image == nil)
        }
        .padding(.leading, start)
        .padding(.trailing, AppDimenKeyPadding.extraSmall.value)
        .padding(.vertical, AppDimenKeyPadding.small.value)
    }
}

/// A grid card; a nil image is the placeholder of the same size.
private struct ImageTile: View {
    let image: SpaceImage?
    let onFavorite: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: AppDimenKeyPadding.extraSmall.value) {
            // A square that takes the column width; the picture fills it.
            AppColorKeyContainer.secondary.color
                .aspectRatio(1, contentMode: .fit)
                .overlay {
                    if let image { RemoteImage(url: image.previewUrl) }
                }
                .overlay(alignment: .topTrailing) {
                    if let image {
                        Heart(isFavorite: image.isFavorite, onClick: onFavorite, onImage: true)
                            .padding(AppDimenKeyPadding.extraSmall.value)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: AppDimenKeyCornerRadius.medium.value))

            ImageTexts(image: image)
        }
    }
}

/// The title in two lines and "date · center" under it; grey bars for a placeholder.
private struct ImageTexts: View {
    let image: SpaceImage?

    var body: some View {
        if let image {
            Text(image.title)
                .font(AppTypographyKey.body.font)
                .fontWeight(.semibold)
                .foregroundStyle(AppColorKeyText.primary.color)
                .lineLimit(2, reservesSpace: true)
            Text([image.date, image.center].compactMap { $0 }.joined(separator: " · "))
                .font(AppTypographyKey.caption.font)
                .foregroundStyle(AppColorKeyText.secondary.color)
                .lineLimit(1)
        } else {
            // `redacted` draws grey bars in place of the text, so a placeholder is as tall as a card.
            Group {
                Text(String(repeating: "x", count: 24))
                    .font(AppTypographyKey.body.font)
                    .lineLimit(2, reservesSpace: true)
                Text(String(repeating: "x", count: 10))
                    .font(AppTypographyKey.caption.font)
            }
            .redacted(reason: .placeholder)
        }
    }
}

/// The favorite mark; over an image it gets a background to stay visible on any picture.
private struct Heart: View {
    let isFavorite: Bool
    let onClick: () -> Void
    var onImage: Bool = false

    var body: some View {
        Button(action: onClick) {
            Image(systemName: (isFavorite ? AppIconKey.favorite : AppIconKey.favoriteOff).sfSymbol)
                .foregroundStyle(isFavorite ? AppColorKeyIcon.accent.color : AppColorKeyIcon.secondary.color)
                .frame(width: 32, height: 32)
                .background {
                    if onImage { Circle().fill(.regularMaterial) }
                }
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
