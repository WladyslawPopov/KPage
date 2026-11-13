import SwiftUI
import SharedLogic

/// The lazy grid of a paged list: one column is a list, more make a grid. It reports what is on
/// screen to the paginator through `PagingListTracker`; the paginator alone decides what to load. Whether
/// the list is on screen is not reported: the app decides when to renew the data, not the list.
///
/// The rows (`PagingRows`) are as long as the total count and stay in Kotlin: they cross by
/// reference, the rows on screen are read one by one, and the tracker gets the same rows with the
/// visible indexes. Rows are identified by the keys of `PagingRows.keys` (the item's own key, as on
/// Android), made once per snapshot: SwiftUI reads the identity of every row on each update, and
/// builds only the views on screen. The scroll position is held by the key of the top row
/// (`scrollPosition(id:)`), so a removed or added row moves only the rows after it.
///
/// A `header` scrolls with the rows (a search field). When `empty` is set it takes the place of the
/// rows under the header, so the header, and the keyboard of a field in it, stay while the list is
/// empty.
///
/// `reversed` starts the list from the bottom, as a chat does: the scroll view is flipped upside down
/// and every row is flipped back, so row 0 is at the bottom and older rows load above it. A decoration
/// takes a cell; in a one-column list that is the whole width. When `startTrigger` changes (not on the
/// first value), the list scrolls back to row 0.
struct PagedGrid<Q: AnyObject, T: AnyObject, D: AnyObject, HeaderView: View, DecorationView: View, ItemView: View, PlaceholderView: View>: View {
    let list: PaginatorUiState<Q, T, D>
    let columns: Int
    let reversed: Bool
    let showPosition: Bool
    /// The gap between the cells, at the sides and above the first row.
    let spacing: CGFloat
    let empty: PageEmptyState?
    let onEmptyAction: (PageIntent) -> Void
    let startTrigger: String?
    /// Identity of an item.
    let itemKey: (T) -> String
    /// Identity of a decoration; by default its position and order. Must differ from the item keys.
    let decorationKey: ((D) -> String)?
    let header: () -> HeaderView
    let decoration: (_ value: D) -> DecorationView
    let placeholder: (_ serverIndex: Int) -> PlaceholderView
    let item: (_ item: T, _ serverIndex: Int) -> ItemView

    init(
        list: PaginatorUiState<Q, T, D>,
        columns: Int = 1,
        reversed: Bool = false,
        showPosition: Bool = true,
        spacing: CGFloat = AppDimenKeyPadding.small.value,
        empty: PageEmptyState? = nil,
        onEmptyAction: @escaping (PageIntent) -> Void = { _ in },
        startTrigger: String? = nil,
        itemKey: @escaping (T) -> String,
        decorationKey: ((D) -> String)? = nil,
        @ViewBuilder header: @escaping () -> HeaderView,
        @ViewBuilder decoration: @escaping (_ value: D) -> DecorationView,
        @ViewBuilder placeholder: @escaping (_ serverIndex: Int) -> PlaceholderView,
        @ViewBuilder item: @escaping (_ item: T, _ serverIndex: Int) -> ItemView
    ) {
        self.list = list
        self.columns = columns
        self.reversed = reversed
        self.showPosition = showPosition
        self.spacing = spacing
        self.empty = empty
        self.onEmptyAction = onEmptyAction
        self.startTrigger = startTrigger
        self.itemKey = itemKey
        self.decorationKey = decorationKey
        self.header = header
        self.decoration = decoration
        self.placeholder = placeholder
        self.item = item
    }

    @State private var tracker: PagingListTracker?
    @State private var trackedEvents: ObjectIdentifier?
    // `@State` keeps one object for the life of the list, as `remember { }` does.
    @State private var keys = RowKeys()
    @State private var visibleKeys: [String] = []
    // The key of the top row on screen; the scroll view keeps that row in place when the rows change.
    @State private var topKey: String?
    // The number of columns `topKey` was taken in: another layout starts from the top.
    @State private var topKeyColumns = 0

    var body: some View {
        let rows = keys.rows(of: list.rows, itemKey: itemKey, decorationKey: decorationKey)
        ScrollViewReader { proxy in
            ScrollView {
                VStack(spacing: 0) {
                    header().flipped(reversed)
                    if let empty {
                        PageEmptyBody(empty: empty, onAction: onEmptyAction)
                            .containerRelativeFrame(.vertical) { height, _ in height * 0.7 }
                            .flipped(reversed)
                    } else {
                        LazyVGrid(
                            columns: Array(repeating: GridItem(.flexible(), spacing: spacing), count: max(columns, 1)),
                            spacing: spacing
                        ) {
                            ForEach(rows) { row in
                                cell(list.rows.get(index: Int32(row.index))).flipped(reversed)
                            }
                        }
                        .scrollTargetLayout()
                        .padding(.horizontal, spacing)
                        .padding(.top, spacing)
                    }
                }
            }
            // A reversed list (a chat) needs no help: it is held from its start, where the newest row is.
            .modifier(TopRowPosition(id: reversed ? nil : topKeyInLayout))
            // A list becomes a grid and back: the list starts from its first row. A lazy grid finds a far row only
            // by laying out every row before it, which is slow; a new identity makes a new scroll view at the top.
            .id(columns)
            // The user's scroll hides the keyboard, as in system apps; the typed text stays.
            .scrollDismissesKeyboard(.immediately)
            .flipped(reversed)
            .onScrollTargetVisibilityChange(idType: String.self, threshold: 0.01) { ids in
                visibleKeys = ids
                report()
            }
            .onChange(of: startTrigger) { old, new in
                guard old != nil, new != nil, let first = rows.first?.id else { return }
                Task {
                    // The new row is laid out first, as on Android.
                    try? await Task.sleep(for: .milliseconds(100))
                    // At once: an animated scroll through a long history lays rows out on the way and they fly.
                    proxy.scrollTo(first, anchor: .top)
                }
            }
        }
        .overlay(alignment: .bottomLeading) {
            if showPosition { positionBadge }
        }
        .onAppear { followEvents() }
        .onChange(of: list) { _, _ in followEvents() }
    }

    @ViewBuilder private func cell(_ row: PagingRow) -> some View {
        // Kotlin generics are erased at run time: the row is cast without its type, the value with it.
        if let itemRow = row as? PagingRowItem<AnyObject> {
            let serverIndex = Int(itemRow.serverIndex.int32Value)
            if let value = itemRow.item as? T {
                item(value, serverIndex)
            } else {
                placeholder(serverIndex)
            }
        } else if let decorationRow = row as? PagingRowDecoration<AnyObject>, let value = decorationRow.value as? D {
            decoration(value)
        }
    }

    private var topKeyInLayout: Binding<String?> {
        Binding(
            get: { topKeyColumns == columns ? topKey : nil },
            set: {
                topKey = $0
                topKeyColumns = columns
            }
        )
    }

    /// The rows on screen, by their index in the current rows.
    private var visibleRows: [Int] { visibleKeys.compactMap { keys.index(of: $0) } }

    /// A new paginator (a new query) gets a new tracker, which reports the visible rows to it from the start.
    private func followEvents() {
        let events = list.events
        let id = events.map { ObjectIdentifier($0 as AnyObject) }
        if id != trackedEvents {
            tracker = events.map { PagingListTracker(events: $0) }
            trackedEvents = id
        }
        report()
    }

    /// Tells the tracker what is on screen; repeated reports are cheap.
    private func report() {
        guard let tracker else { return }
        tracker.onLayout(rows: list.rows, visibleRows: visibleRows.map { KotlinInt(int: Int32($0)) })
    }

    /// "Last visible / total" by server position; hidden while the list is empty.
    @ViewBuilder private var positionBadge: some View {
        let total = Int(list.totalCount)
        let last = visibleRows.compactMap { list.rows.serverIndexAt(row: Int32($0))?.intValue }.max()
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

/// Holds the scroll position by the key of the top row, when there is one to hold.
private struct TopRowPosition: ViewModifier {
    let id: Binding<String?>?

    func body(content: Content) -> some View {
        if let id {
            content.scrollPosition(id: id, anchor: .top)
        } else {
            content
        }
    }
}

/// One row for `ForEach`: its key and its index in the rows.
private struct RowKey: Identifiable {
    let id: String
    let index: Int
}

/// The keys of the rows of one snapshot, made in Kotlin by the same rules as on Android, once per
/// snapshot (`remember(rows)` on Android). A plain class kept in `@State`: filling it is not a change
/// SwiftUI watches.
private final class RowKeys {
    private var source: AnyObject?
    private var rows: [RowKey] = []
    private var indexes: [String: Int] = [:]

    func rows<T: AnyObject, D: AnyObject>(
        of rows: PagingRows<T, D>,
        itemKey: @escaping (T) -> String,
        decorationKey: ((D) -> String)?
    ) -> [RowKey] {
        if rows === source { return self.rows }
        let keyAt = rows.keys(
            decorationKey: decorationKey.map { key in { value, _ in key(value) } },
            itemKey: { itemKey($0) }
        )
        let made = (0..<Int(rows.rowCount)).map { RowKey(id: String(describing: keyAt(KotlinInt(int: Int32($0)))), index: $0) }
        source = rows
        self.rows = made
        indexes = Dictionary(made.map { ($0.id, $0.index) }, uniquingKeysWith: { first, _ in first })
        return made
    }

    func index(of key: String) -> Int? { indexes[key] }
}

extension PagedGrid where DecorationView == EmptyView {
    /// A list without decorations, with a header.
    init(
        list: PaginatorUiState<Q, T, D>,
        columns: Int = 1,
        showPosition: Bool = true,
        spacing: CGFloat = AppDimenKeyPadding.small.value,
        empty: PageEmptyState? = nil,
        onEmptyAction: @escaping (PageIntent) -> Void = { _ in },
        itemKey: @escaping (T) -> String,
        @ViewBuilder header: @escaping () -> HeaderView,
        @ViewBuilder placeholder: @escaping (_ serverIndex: Int) -> PlaceholderView,
        @ViewBuilder item: @escaping (_ item: T, _ serverIndex: Int) -> ItemView
    ) {
        self.init(
            list: list,
            columns: columns,
            showPosition: showPosition,
            spacing: spacing,
            empty: empty,
            onEmptyAction: onEmptyAction,
            itemKey: itemKey,
            header: header,
            decoration: { _ in EmptyView() },
            placeholder: placeholder,
            item: item
        )
    }
}

extension PagedGrid where HeaderView == EmptyView, DecorationView == EmptyView {
    /// A list without a header and decorations.
    init(
        list: PaginatorUiState<Q, T, D>,
        columns: Int = 1,
        showPosition: Bool = true,
        spacing: CGFloat = AppDimenKeyPadding.small.value,
        itemKey: @escaping (T) -> String,
        @ViewBuilder placeholder: @escaping (_ serverIndex: Int) -> PlaceholderView,
        @ViewBuilder item: @escaping (_ item: T, _ serverIndex: Int) -> ItemView
    ) {
        self.init(
            list: list,
            columns: columns,
            showPosition: showPosition,
            spacing: spacing,
            itemKey: itemKey,
            header: { EmptyView() },
            decoration: { _ in EmptyView() },
            placeholder: placeholder,
            item: item
        )
    }
}

extension PagedGrid where HeaderView == EmptyView {
    /// A list with decorations and without a header, for example a chat.
    init(
        list: PaginatorUiState<Q, T, D>,
        reversed: Bool,
        showPosition: Bool = true,
        spacing: CGFloat = AppDimenKeyPadding.small.value,
        startTrigger: String? = nil,
        itemKey: @escaping (T) -> String,
        decorationKey: ((D) -> String)? = nil,
        @ViewBuilder decoration: @escaping (_ value: D) -> DecorationView,
        @ViewBuilder placeholder: @escaping (_ serverIndex: Int) -> PlaceholderView,
        @ViewBuilder item: @escaping (_ item: T, _ serverIndex: Int) -> ItemView
    ) {
        self.init(
            list: list,
            reversed: reversed,
            showPosition: showPosition,
            spacing: spacing,
            startTrigger: startTrigger,
            itemKey: itemKey,
            decorationKey: decorationKey,
            header: { EmptyView() },
            decoration: decoration,
            placeholder: placeholder,
            item: item
        )
    }
}

private extension View {
    /// Upside down when `isFlipped`: a reversed list flips its scroll view and every row back.
    func flipped(_ isFlipped: Bool) -> some View {
        scaleEffect(x: 1, y: isFlipped ? -1 : 1)
    }
}
