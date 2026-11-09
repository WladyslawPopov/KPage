import SwiftUI
import SharedLogic

/// The title: a long press copies it, with a short press-in of the text, a haptic tick and an
/// `onCopied` message (iOS shows nothing of its own after a copy).
struct CopyableTitle: View {
    let title: String
    let onCopied: () -> Void

    @State private var isPulsing = false
    // Changes on every copy: the haptic fires when the value it watches changes.
    @State private var copies = 0

    var body: some View {
        Text(title)
            .font(AppTypographyKey.headline.font)
            .fontWeight(.semibold)
            .foregroundStyle(AppColorKeyText.primary.color)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                isPulsing ? AppColorKeyContainer.accent.color : Color.clear,
                in: RoundedRectangle(cornerRadius: AppDimenKeyCornerRadius.medium.value)
            )
            .scaleEffect(isPulsing ? 0.96 : 1, anchor: .leading)
            .contentShape(Rectangle())
            .onLongPressGesture { copy() }
            .sensoryFeedback(.impact(weight: .medium), trigger: copies)
    }

    private func copy() {
        UIPasteboard.general.string = title
        copies += 1
        onCopied()
        withAnimation(.spring(duration: 0.15)) { isPulsing = true }
        Task {
            try? await Task.sleep(for: .milliseconds(150))
            withAnimation(.spring(duration: 0.25)) { isPulsing = false }
        }
    }
}
