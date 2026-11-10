import SwiftUI
import SharedLogic

/// A chat of the list: the avatar, the title, the last message and its time; a nil chat is the
/// placeholder of the same size. `trailing` is the end of the row: a check mark in the selection mode.
struct ChatRow<Trailing: View>: View {
    let chat: Chat?
    @ViewBuilder let trailing: () -> Trailing

    private let avatarSize: CGFloat = 48

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: AppDimenKeyPadding.medium.value) {
                avatar
                VStack(alignment: .leading, spacing: 2) {
                    if let chat {
                        HStack {
                            Text(chat.title)
                                .font(AppTypographyKey.subtitle.font)
                                .foregroundStyle(AppColorKeyText.primary.color)
                                .lineLimit(1)
                            Spacer(minLength: AppDimenKeyPadding.small.value)
                            Text(chat.time().string)
                                .font(AppTypographyKey.caption.font)
                                .foregroundStyle(AppColorKeyText.secondary.color)
                        }
                        Text(chat.lastMessage)
                            .font(AppTypographyKey.body.font)
                            .foregroundStyle(AppColorKeyText.secondary.color)
                            .lineLimit(1)
                    } else {
                        bar(width: 0.6)
                        bar(width: 0.9)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                trailing()
            }
            .padding(.horizontal, AppDimenKeyPadding.large.value)
            .padding(.vertical, AppDimenKeyPadding.medium.value)
            Divider().padding(.leading, AppDimenKeyPadding.large.value + avatarSize + AppDimenKeyPadding.medium.value)
        }
    }

    private var avatar: some View {
        ChatAvatarView(avatar: chat?.avatar(), size: avatarSize, font: AppTypographyKey.title.font)
    }

    private func bar(width: CGFloat) -> some View {
        RoundedRectangle(cornerRadius: AppDimenKeyCornerRadius.small.value)
            .fill(AppColorKeyContainer.secondary.color)
            .frame(height: AppDimenKeyIconSize.extraSmall.value)
            .containerRelativeFrame(.horizontal) { length, _ in length * width * 0.6 }
    }
}

extension ChatRow where Trailing == EmptyView {
    init(chat: Chat?) {
        self.init(chat: chat) { EmptyView() }
    }
}
