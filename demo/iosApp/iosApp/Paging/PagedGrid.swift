import SwiftUI
import SharedLogic

/// The lazy grid of a paged list: one column is a list, more make a grid. It reports what is on
/// screen to the paginator through `PagingListTracker`; the paginator alone decides what to load.
///
/// The rows (`PagingRows`) are as long as the total count and stay in Kotlin: they cross by
/// reference, the rows on screen are read one by one, and the tracker gets the same rows with the
/// visible indexes. Rows are identified by their position, since SwiftUI reads the identity of
/// every row on each update and a position costs nothing.
///
/// The list is shown to the paginator while it is on screen and the app is active; the system tells
/// both (`onAppear` / `onDisappear` and `scenePhase`), the same as the lifecycle owner does for
/// `PagingEffect` on Android.
///
/// Decorations and a reversed list (chats) are not supported yet.
struct PagedGrid<Q: AnyObject, T: AnyObject, D: AnyObject, ItemView: View, PlaceholderView: View>: View {
    let list: PaginatorUiState<Q, T, D>
    var columns: Int = 1
    var showPosition: Bool = true
    /// The gap between the cells, at the sides and above the first row.
    var spacing: CGFloat = AppDimenKeyPadding.small.value
    @ViewBuilder let placeholder: (_ serverIndex: Int) -> PlaceholderView
    @ViewBuilder let item: (_ item: T, _ serverIndex: Int) -> ItemView

    @Environment(\.scenePhase) private var scenePhase
    @State private var isOnScreen = false
    @State private var tracker: PagingListTracker?
    @State private var trackedEvents: ObjectIdentifier?
    @State private var visible: Set<Int> = []

    private var isShown: Bool { isOnScreen && scenePhase == .active }

    var body: some View {
        ScrollView {
            LazyVGrid(
                columns: Array(repeating: GridItem(.flexible(), spacing: spacing), count: max(columns, 1)),
                spacing: spacing
            ) {
                ForEach(0..<Int(list.rows.rowCount), id: \.self) { index in
                    cell(list.rows.get(index: Int32(index)))
                }
            }
            .scrollTargetLayout()
            .padding(.horizontal, spacing)
            .padding(.top, spacing)
        }
        // The user's scroll hides the keyboard, as in system apps; the typed text stays.
        .scrollDismissesKeyboard(.immediately)
        .onScrollTargetVisibilityChange(idType: Int.self, threshold: 0.01) { ids in
            visible = Set(ids)
            report()
        }
        .overlay(alignment: .bottomLeading) {
            if showPosition { positionBadge }
        }
        .onAppear {
            isOnScreen = true
            followEvents()
        }
        .onDisappear { isOnScreen = false }
        .onChange(of: isShown) { _, shown in
            if shown { tracker?.onStart() } else { tracker?.onStop() }
        }
        .onChange(of: list) { _, _ in followEvents() }
    }

    @ViewBuilder private func cell(_ row: PagingRow) -> some View {
        // Kotlin generics are erased at run time: the row is cast without its type, the item with it.
        if let itemRow = row as? PagingRowItem<AnyObject> {
            let serverIndex = Int(itemRow.serverIndex.int32Value)
            if let value = itemRow.item as? T {
                item(value, serverIndex)
            } else {
                placeholder(serverIndex)
            }
        }
    }

    /// A new paginator (a new query) gets a new tracker and its own `onShown`.
    private func followEvents() {
        let events = list.events
        let id = events.map { ObjectIdentifier($0 as AnyObject) }
        if id != trackedEvents {
            tracker?.onStop()
            tracker = events.map { PagingListTracker(events: $0) }
            trackedEvents = id
            if isShown { tracker?.onStart() }
        }
        report()
    }

    /// Tells the tracker what is on screen; repeated reports are cheap.
    private func report() {
        guard let tracker else { return }
        tracker.onLayout(rows: list.rows, visibleRows: visible.map { KotlinInt(int: Int32($0)) })
    }

    /// "Last visible / total" by server position; hidden while the list is empty.
    @ViewBuilder private var positionBadge: some View {
        let total = Int(list.totalCount)
        let last = visible.compactMap { list.rows.serverIndexAt(row: Int32($0))?.intValue }.max()
        if let last, total > 0 {
            Text("\(min(last + 1, total)) / \(total)")
                .font(AppTypographyKey.caption.font)
                .foregroundStyle(AppColorKeyText.secondary.color)
                .padding(.horizontal, AppDimenKeyPadding.medium.value)
                .padding(.vertical, AppDimenKeyPadding.extraSmall.value)
                .background(.regularMaterial, in: Capsule())
                .padding(AppDimenKeyPadding.small.value)
        }
    }
}
