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
    // The state lists what is set; the search chip is shown only while the field is out of sight.
    private var visibleChips: [SpaceChip] { state.chips.filter { $0.opens != .search || isSearchAway } }
    private var showsChips: Bool { !visibleChips.isEmpty }

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
                        chips: visibleChips,
                        onOpenSearch: openSearch,
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
        Group {
            if isGrid {
                ImageTile(image: image, onFavorite: onFavorite)
            } else {
                ImageRow(image: image, onFavorite: onFavorite)
            }
        }
        // A tap on the card opens it; the heart is a button of its own and keeps its taps.
        .contentShape(Rectangle())
        .onTapGesture {
            if let image { component.onIntent(intent: SpaceIntentOpenImage(id: image.id)) }
        }
    }
}
