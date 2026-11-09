import SwiftUI
import SharedLogic

/// A list card with a divider under the texts; a nil image is the placeholder of the same size.
struct ImageRow: View {
    let image: SpaceImage?
    let onFavorite: () -> Void

    private let thumbnail: CGFloat = 72
    private let gap = AppDimenKeyPadding.medium.value
    private let start = AppDimenKeyPadding.large.value

    var body: some View {
        VStack(spacing: 0) {
            row
            // From the start of the texts to the end of the screen.
            Divider().padding(.leading, start + thumbnail + gap)
        }
    }

    private var row: some View {
        HStack(spacing: gap) {
            Group {
                if let image {
                    RemoteImage(url: image.previewUrl)
                } else {
                    AppColorKeyContainer.secondary.color
                }
            }
            .frame(width: thumbnail, height: thumbnail)
            .clipShape(RoundedRectangle(cornerRadius: AppDimenKeyCornerRadius.medium.value))

            VStack(alignment: .leading, spacing: AppDimenKeyPadding.extraSmall.value) {
                ImageTexts(image: image)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // Keeps the text width of a placeholder equal to a card.
            Heart(isFavorite: image?.isFavorite ?? false, onClick: onFavorite)
                .opacity(image == nil ? 0 : 1)
                .disabled(image == nil)
        }
        .padding(.leading, start)
        .padding(.trailing, AppDimenKeyPadding.extraSmall.value)
        .padding(.vertical, AppDimenKeyPadding.small.value)
    }
}

/// A grid card; a nil image is the placeholder of the same size.
struct ImageTile: View {
    let image: SpaceImage?
    let onFavorite: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: AppDimenKeyPadding.extraSmall.value) {
            // A square that takes the column width; the picture fills it.
            AppColorKeyContainer.secondary.color
                .aspectRatio(1, contentMode: .fit)
                .overlay {
                    if let image { RemoteImage(url: image.previewUrl) }
                }
                .overlay(alignment: .topTrailing) {
                    if let image {
                        Heart(isFavorite: image.isFavorite, onClick: onFavorite, onImage: true)
                            .padding(AppDimenKeyPadding.extraSmall.value)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: AppDimenKeyCornerRadius.medium.value))

            ImageTexts(image: image)
        }
    }
}

/// The title in two lines and "date · center" under it; grey bars for a placeholder.
private struct ImageTexts: View {
    let image: SpaceImage?

    var body: some View {
        if let image {
            Text(image.title)
                .font(AppTypographyKey.body.font)
                .fontWeight(.semibold)
                .foregroundStyle(AppColorKeyText.primary.color)
                .lineLimit(2, reservesSpace: true)
            Text(image.caption())
                .font(AppTypographyKey.caption.font)
                .foregroundStyle(AppColorKeyText.secondary.color)
                .lineLimit(1)
        } else {
            // `redacted` draws grey bars in place of the text, so a placeholder is as tall as a card.
            Group {
                Text(String(repeating: "x", count: 24))
                    .font(AppTypographyKey.body.font)
                    .lineLimit(2, reservesSpace: true)
                Text(String(repeating: "x", count: 10))
                    .font(AppTypographyKey.caption.font)
            }
            .redacted(reason: .placeholder)
        }
    }
}
