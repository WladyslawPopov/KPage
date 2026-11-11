import SwiftUI
import SharedLogic

/// The favorite mark; over an image it gets a background to stay visible on any picture.
struct Heart: View {
    let isFavorite: Bool
    let onClick: () -> Void
    var onImage: Bool = false

    var body: some View {
        Button(action: onClick) {
            Image(systemName: (isFavorite ? AppIconKey.favorite : AppIconKey.favoriteOff).sfSymbol)
                .foregroundStyle(isFavorite ? AppColorKeyIcon.favorite.color : AppColorKeyIcon.secondary.color)
                .frame(width: 32, height: 32)
                .background {
                    if onImage { Circle().fill(.regularMaterial) }
                }
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
