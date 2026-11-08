import SwiftUI
import SharedLogic

/// What is set, as glass chips under the navigation bar, with no strip behind them: the search text
/// while the search field is out of sight, the center, the years (one chip). A tap on a chip opens
/// what it came from (the search field, the filters); its cross removes it.
struct SpaceFilterChips: View {
    let query: SpaceQuery
    /// The text of the search; nil hides the search chip.
    let searchText: String?
    let onOpenSearch: () -> Void
    let onClearSearch: () -> Void
    let onOpenFilters: () -> Void
    let onIntent: (SpaceIntent) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: AppDimenKeyPadding.small.value) {
                if let searchText {
                    chip(searchText, icon: .search, onOpen: onOpenSearch, onRemove: onClearSearch)
                }
                if let center = query.center {
                    chip(SpaceFilterOptions.shared.title(center: center).text, onOpen: onOpenFilters) {
                        send(nil, query.yearStart, query.yearEnd)
                    }
                }
                if query.yearStart != nil || query.yearEnd != nil {
                    chip(yearsText, onOpen: onOpenFilters) { send(query.center, nil, nil) }
                }
            }
            .padding(.horizontal, AppDimenKeyPadding.large.value)
            .padding(.vertical, AppDimenKeyPadding.small.value)
        }
    }

    /// The text and the cross are two buttons in one glass capsule.
    private func chip(
        _ text: String,
        icon: AppIconKey? = nil,
        onOpen: @escaping () -> Void,
        onRemove: @escaping () -> Void
    ) -> some View {
        HStack(spacing: 0) {
            Button(action: onOpen) {
                HStack(spacing: AppDimenKeyPadding.small.value) {
                    if let icon {
                        Image(systemName: icon.sfSymbol).foregroundStyle(AppColorKeyIcon.secondary.color)
                    }
                    Text(text).lineLimit(1)
                }
                .padding(.leading, AppDimenKeyPadding.large.value)
                .padding(.trailing, AppDimenKeyPadding.extraSmall.value)
                .padding(.vertical, AppDimenKeyPadding.small.value)
                .contentShape(Rectangle())
            }
            .accessibilityLabel(text)
            Button(action: onRemove) {
                Image(systemName: AppIconKey.close.sfSymbol)
                    .font(.caption2)
                    .padding(.horizontal, AppDimenKeyPadding.medium.value)
                    .padding(.vertical, AppDimenKeyPadding.small.value)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel(AppStringKeyCore.clear.text)
        }
        .buttonStyle(.plain)
        .font(AppTypographyKey.label.font)
        .glassBackground(in: Capsule(), interactive: true)
    }

    /// "1960–1969", or "From 1960" / "To 1969" when only one end is set.
    private var yearsText: String {
        switch (query.yearStart, query.yearEnd) {
        case let (from?, to?): return "\(from)–\(to)"
        case let (from?, nil): return "\(AppStringKeySpace.from.text) \(from)"
        case let (nil, to?): return "\(AppStringKeySpace.to.text) \(to)"
        default: return ""
        }
    }

    private func send(_ center: SpaceCenter?, _ from: KotlinInt?, _ to: KotlinInt?) {
        onIntent(SpaceIntentSetFilters(center: center, yearStart: from, yearEnd: to))
    }
}
