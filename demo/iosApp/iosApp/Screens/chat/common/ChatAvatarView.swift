import SwiftUI
import SharedLogic

/// The first letter of the interlocutor on their color; a grey circle while there is no avatar.
struct ChatAvatarView: View {
    let avatar: ChatAvatar?
    let size: CGFloat
    let font: Font

    var body: some View {
        Circle()
            .fill(avatar.map { avatarPalette[Int($0.color) % avatarPalette.count] } ?? AppColorKeyContainer.secondary.color)
            .frame(width: size, height: size)
            .overlay {
                if let avatar {
                    Text(avatar.letter).font(font).foregroundStyle(.white)
                }
            }
    }
}

/// One color per index of `ChatAvatar.color`; the same order as on Android.
private let avatarPalette: [Color] = [
    Color(red: 0.36, green: 0.42, blue: 0.75), Color(red: 0.15, green: 0.65, blue: 0.60),
    Color(red: 0.94, green: 0.42, blue: 0.0), Color(red: 0.67, green: 0.28, blue: 0.74),
    Color(red: 0.26, green: 0.65, blue: 0.96), Color(red: 0.93, green: 0.25, blue: 0.48),
]
