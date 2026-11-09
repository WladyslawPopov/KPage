import SwiftUI
import SharedLogic

/// The picture with the heart on it; a nil `display` (not loaded yet) is an empty frame of the usual
/// proportions. `onFavorite` gets the value to set; `onOpen` is a tap on the picture.
struct DetailsHero: View {
    let display: SpaceDetailsDisplay?
    let onFavorite: (Bool) -> Void
    let onOpen: () -> Void

    var body: some View {
        AppColorKeyContainer.secondary.color
            .aspectRatio(CGFloat(display?.picture.aspectRatio ?? 4.0 / 3.0), contentMode: .fit)
            .overlay {
                if let display { RemoteImage(url: display.picture.url) }
            }
            .overlay(alignment: .bottomTrailing) {
                if let display {
                    Heart(isFavorite: display.isFavorite, onClick: { onFavorite(!display.isFavorite) }, onImage: true)
                        .padding(AppDimenKeyPadding.small.value)
                }
            }
            .clipped()
            // The heart is a button of its own and keeps its taps.
            .contentShape(Rectangle())
            .onTapGesture {
                if display?.fullPictureUrl != nil { onOpen() }
            }
    }
}
