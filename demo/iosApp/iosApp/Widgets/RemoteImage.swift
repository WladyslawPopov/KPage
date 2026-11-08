import SwiftUI
import SharedLogic

/// An image from the network over a quiet background. A missing url or a failed load shows the
/// "no image" icon. A failed load is tried again `maxRetries` times: `AsyncImage` stays failed on its own,
/// for example after a load cancelled by a fast scroll. Loaded images are kept by `URLCache.shared`,
/// sized in `AppDelegate`.
struct RemoteImage: View {
    let url: String?
    var contentMode: ContentMode = .fill

    @State private var attempt = 0

    var body: some View {
        AsyncImage(url: url.flatMap(URL.init(string:))) { phase in
            switch phase {
            case .success(let image):
                image.resizable().aspectRatio(contentMode: contentMode)
            case .failure:
                noImage.task {
                    guard attempt < Self.maxRetries else { return }
                    try? await Task.sleep(for: .milliseconds(300))
                    if !Task.isCancelled { attempt += 1 }
                }
            default:
                if url == nil { noImage } else { Color.clear }
            }
        }
        // A new `AsyncImage` per try, so the request runs again.
        .id(attempt)
        .onChange(of: url) { _, _ in attempt = 0 }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppColorKeyContainer.secondary.color)
        .clipped()
    }

    private static let maxRetries = 2

    private var noImage: some View {
        Image(systemName: AppIconKey.noImage.sfSymbol)
            .font(.title)
            .foregroundStyle(AppColorKeyIcon.secondary.color)
    }
}
