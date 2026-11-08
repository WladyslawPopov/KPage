import SwiftUI
import SharedLogic

/// The filters that are set, as glass chips under the navigation bar, with no strip behind them. A tap
/// on a chip removes its filter; the years are one chip.
struct SpaceFilterChips: View {
    let query: SpaceQuery
    let onIntent: (SpaceIntent) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: AppDimenKeyPadding.small.value) {
                if let center = query.center {
                    chip(SpaceFilterOptions.shared.title(center: center).text) {
                        send(nil, query.yearStart, query.yearEnd)
                    }
                }
                if query.yearStart != nil || query.yearEnd != nil {
                    chip(yearsText) { send(query.center, nil, nil) }
                }
            }
            .padding(.horizontal, AppDimenKeyPadding.large.value)
            .padding(.vertical, AppDimenKeyPadding.small.value)
        }
    }

    private func chip(_ text: String, onRemove: @escaping () -> Void) -> some View {
        Button(action: onRemove) {
            HStack(spacing: AppDimenKeyPadding.extraSmall.value) {
                Text(text)
                Image(systemName: AppIconKey.close.sfSymbol).font(.caption2)
            }
            .font(AppTypographyKey.label.font)
            .padding(.horizontal, AppDimenKeyPadding.medium.value)
            .padding(.vertical, AppDimenKeyPadding.small.value)
            .glassChip()
        }
        .buttonStyle(.plain)
        .accessibilityLabel(text)
        .accessibilityHint(AppStringKeyCore.clear.text)
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

private extension View {
    /// Liquid Glass from iOS 26; a system material before it.
    @ViewBuilder func glassChip() -> some View {
        if #available(iOS 26, *) {
            glassEffect(.regular.interactive(), in: Capsule())
        } else {
            background(.regularMaterial, in: Capsule())
        }
    }
}
