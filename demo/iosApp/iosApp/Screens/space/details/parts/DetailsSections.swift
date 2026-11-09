import SwiftUI
import SharedLogic

/// A titled card.
struct DetailsCard<Content: View>: View {
    let title: AppStringKey
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: AppDimenKeyPadding.small.value) {
            Text(title.text)
                .font(AppTypographyKey.subtitle.font)
                .foregroundStyle(AppColorKeyText.primary.color)
            VStack(alignment: .leading, spacing: 0) { content() }
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    AppColorKeyContainer.elevated.color,
                    in: RoundedRectangle(cornerRadius: AppDimenKeyCornerRadius.large.value)
                )
        }
    }
}

/// The fields of a card with dividers between them; a row with an action is tappable and sends it.
struct DetailsFieldRows: View {
    let rows: [DetailsRow]
    let onIntent: (SpaceDetailsIntent) -> Void

    var body: some View {
        ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
            if index > 0 { Divider().padding(.leading, AppDimenKeyPadding.large.value) }
            if let action = row.action {
                Button { onIntent(action) } label: { content(row, isTappable: true) }.buttonStyle(.plain)
            } else {
                content(row, isTappable: false)
            }
        }
    }

    private func content(_ row: DetailsRow, isTappable: Bool) -> some View {
        HStack(spacing: AppDimenKeyPadding.medium.value) {
            if let icon = row.icon {
                Image(systemName: icon.sfSymbol)
                    .foregroundStyle(AppColorKeyIcon.secondary.color)
                    .frame(width: AppDimenKeyIconSize.medium.value)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(row.label.text)
                    .font(AppTypographyKey.caption.font)
                    .foregroundStyle(AppColorKeyText.secondary.color)
                Text(row.value.string)
                    .font(AppTypographyKey.body.font)
                    .foregroundStyle(isTappable ? AppColorKeyText.accent.color : AppColorKeyText.primary.color)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if isTappable {
                Image(systemName: AppIconKey.forward.sfSymbol)
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(AppColorKeyIcon.secondary.color)
            }
        }
        .padding(.horizontal, AppDimenKeyPadding.large.value)
        .padding(.vertical, AppDimenKeyPadding.medium.value)
        .contentShape(Rectangle())
    }
}

/// The chips of a card; each sends its action.
struct DetailsChips: View {
    let chips: [DetailsChip]
    let onIntent: (SpaceDetailsIntent) -> Void

    var body: some View {
        ChipsFlow(spacing: AppDimenKeyPadding.small.value) {
            ForEach(Array(chips.enumerated()), id: \.offset) { _, chip in
                Button { onIntent(chip.action) } label: {
                    HStack(spacing: AppDimenKeyPadding.extraSmall.value) {
                        Image(systemName: chip.icon.sfSymbol)
                            .foregroundStyle(AppColorKeyIcon.accent.color)
                        Text(chip.text).lineLimit(1)
                    }
                    .font(AppTypographyKey.label.font)
                    .foregroundStyle(AppColorKeyText.primary.color)
                    .padding(.horizontal, AppDimenKeyPadding.medium.value)
                    .padding(.vertical, AppDimenKeyPadding.small.value)
                    .background(AppColorKeyContainer.accent.color, in: Capsule())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(AppDimenKeyPadding.medium.value)
    }
}
