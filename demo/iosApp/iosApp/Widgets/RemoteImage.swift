import SwiftUI
import SharedLogic

/// An image from the network over a quiet background. A missing url or a failed load shows the
/// "no image" icon. Loaded images are kept by `URLCache.shared`, sized in `AppDelegate`.
struct RemoteImage: View {
    let url: String?
    var contentMode: ContentMode = .fill

    var body: some View {
        AsyncImage(url: url.flatMap(URL.init(string:))) { phase in
            switch phase {
            case .success(let image):
                image.resizable().aspectRatio(contentMode: contentMode)
            case .failure:
                noImage
            default:
                if url == nil { noImage } else { Color.clear }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppColorKeyContainer.secondary.color)
        .clipped()
    }

    private var noImage: some View {
        Image(systemName: AppIconKey.noImage.sfSymbol)
            .font(.title)
            .foregroundStyle(AppColorKeyIcon.secondary.color)
    }
}
