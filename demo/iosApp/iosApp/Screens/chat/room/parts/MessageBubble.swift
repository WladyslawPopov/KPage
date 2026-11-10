import SwiftUI
import SharedLogic

/// A message: a bubble at the trailing side for mine, at the leading side for the interlocutor's, as
/// wide as its content. A long press opens its menu; the actions come ready from the shared code. A tap
/// on a photo opens it.
///
/// The menu is a list of actions, not `.contextMenu`: the context menu lifts a snapshot of the bubble,
/// and inside the flipped list of a chat that snapshot turns upside down.
struct MessageBubble: View {
    let item: ChatMessageItem
    let onIntent: (ChatRoomIntent) -> Void
    let onOpenPhoto: (ChatAttachment) -> Void

    @State private var isMenuShown = false

    var body: some View {
        HStack {
            if item.isMine { Spacer(minLength: 60) }
            // Trailing: the time sits under the end of the text. No view here takes the whole width,
            // so the bubble stays as wide as its content.
            VStack(alignment: .trailing, spacing: AppDimenKeyPadding.extraSmall.value) {
                if !item.attachments.isEmpty { photos }
                if !item.text.isEmpty {
                    Text(item.text)
                        .font(AppTypographyKey.body.font)
                        .foregroundStyle(AppColorKeyText.primary.color)
                        .multilineTextAlignment(.leading)
                }
                footer
            }
            .fixedSize(horizontal: false, vertical: true)
            .padding(AppDimenKeyPadding.small.value)
            .background(
                item.isMine ? AppColorKeyContainer.accent.color : AppColorKeyContainer.elevated.color,
                in: RoundedRectangle(cornerRadius: 18)
            )
            .onLongPressGesture {
                guard !item.actions.isEmpty else { return }
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                isMenuShown = true
            }
            .confirmationDialog("", isPresented: $isMenuShown, titleVisibility: .hidden) {
                ForEach(Array(item.actions.enumerated()), id: \.offset) { _, action in
                    Button(action.title.text) {
                        if let text = action.copyText { UIPasteboard.general.string = text }
                        onIntent(action.intent)
                    }
                }
            }
            if !item.isMine { Spacer(minLength: 60) }
        }
        .padding(.horizontal, AppDimenKeyPadding.small.value)
        .padding(.vertical, 2)
    }

    /// The time, and for a local message whether it failed.
    private var footer: some View {
        HStack(spacing: AppDimenKeyPadding.extraSmall.value) {
            if item.sendState == .failed {
                Image(systemName: AppIconKey.warning.sfSymbol)
                Text(AppStringKeyChat.sendFailed.text)
            }
            Text(item.time.string).foregroundStyle(AppColorKeyText.secondary.color)
            if item.isMine && item.sendState == nil {
                Image(systemName: AppIconKey.done.sfSymbol).foregroundStyle(AppColorKeyIcon.accent.color)
            }
        }
        .font(AppTypographyKey.caption.font)
        .foregroundStyle(AppColorKeyStatus.fail.color)
    }

    // Two photos in a row; a single one keeps its own proportions.
    @ViewBuilder private var photos: some View {
        let list = item.attachments
        if list.count == 1, let photo = list.first {
            let ratio = min(max(CGFloat(photo.width) / CGFloat(max(photo.height, 1)), 0.5), 2)
            ChatPhoto(photo: photo)
                .frame(width: 260, height: 260 / ratio)
                .clipShape(RoundedRectangle(cornerRadius: AppDimenKeyCornerRadius.medium.value))
                .onTapGesture { onOpenPhoto(photo) }
        } else {
            LazyVGrid(columns: [GridItem(.fixed(128)), GridItem(.fixed(128))], spacing: 4) {
                ForEach(list, id: \.path) { photo in
                    ChatPhoto(photo: photo)
                        .frame(width: 128, height: 128)
                        .clipShape(RoundedRectangle(cornerRadius: AppDimenKeyCornerRadius.medium.value))
                        .onTapGesture { onOpenPhoto(photo) }
                }
            }
        }
    }
}

/// The day of the messages under it, in the middle of the list.
struct DateSeparatorView: View {
    let date: UiText

    var body: some View {
        Text(date.string)
            .font(AppTypographyKey.caption.font)
            .foregroundStyle(AppColorKeyText.secondary.color)
            .padding(.horizontal, AppDimenKeyPadding.medium.value)
            .padding(.vertical, AppDimenKeyPadding.extraSmall.value)
            .background(AppColorKeyContainer.elevated.color, in: Capsule())
            .frame(maxWidth: .infinity)
            .padding(.vertical, AppDimenKeyPadding.small.value)
    }
}

/// A message that is not loaded yet: a grey bubble, on alternating sides.
struct MessagePlaceholder: View {
    let serverIndex: Int

    var body: some View {
        RoundedRectangle(cornerRadius: 18)
            .fill(AppColorKeyContainer.secondary.color)
            .frame(width: 200, height: 48)
            .frame(maxWidth: .infinity, alignment: serverIndex % 2 == 0 ? .leading : .trailing)
            .padding(.horizontal, AppDimenKeyPadding.small.value)
            .padding(.vertical, 2)
    }
}
