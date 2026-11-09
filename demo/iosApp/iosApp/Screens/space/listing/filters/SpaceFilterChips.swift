import SwiftUI
import SharedLogic

/// The `chips` of what is set, as glass chips under the navigation bar, with no strip behind them. A tap
/// on a chip opens what it came from (the search field, the filters); its cross removes it.
struct SpaceFilterChips: View {
    let chips: [SpaceChip]
    let onOpenSearch: () -> Void
    let onOpenFilters: () -> Void
    let onIntent: (SpaceIntent) -> Void

    /// A long text (a photographer, an album) is cut, so one chip does not take the whole row.
    private let maxTextWidth: CGFloat = 160

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: AppDimenKeyPadding.small.value) {
                ForEach(Array(chips.enumerated()), id: \.offset) { _, chip in
                    self.chip(chip)
                }
            }
            .padding(.horizontal, AppDimenKeyPadding.large.value)
            .padding(.vertical, AppDimenKeyPadding.small.value)
        }
    }

    /// The text and the cross are two buttons in one glass capsule.
    private func chip(_ chip: SpaceChip) -> some View {
        let text = chip.text.string
        return HStack(spacing: 0) {
            Button(action: chip.opens == .search ? onOpenSearch : onOpenFilters) {
                HStack(spacing: AppDimenKeyPadding.small.value) {
                    if let icon = chip.icon {
                        Image(systemName: icon.sfSymbol).foregroundStyle(AppColorKeyIcon.secondary.color)
                    }
                    Text(text).lineLimit(1).frame(maxWidth: maxTextWidth)
                }
                .padding(.leading, AppDimenKeyPadding.large.value)
                .padding(.trailing, AppDimenKeyPadding.extraSmall.value)
                .padding(.vertical, AppDimenKeyPadding.small.value)
                .contentShape(Rectangle())
            }
            .accessibilityLabel(text)
            Button { onIntent(chip.remove) } label: {
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
}
