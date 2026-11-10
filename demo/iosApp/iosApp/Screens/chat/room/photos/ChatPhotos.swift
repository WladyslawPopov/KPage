import SwiftUI
import PhotosUI
import SharedLogic

/// The photos of the chat on disk. The picked photos are scaled down and saved as JPEG in the app's
/// folder; only the path crosses into Kotlin, never the bytes.
enum ChatPhotos {
    private static let maxSide: CGFloat = 1600
    private static let quality: CGFloat = 0.8

    /// The folder of the photos. The app's container may move after an update, so a stored path is
    /// read by its file name inside the current folder.
    private static var folder: URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        return base.appendingPathComponent("chat_photos", isDirectory: true)
    }

    static func url(of photo: ChatAttachment) -> URL {
        folder.appendingPathComponent((photo.path as NSString).lastPathComponent)
    }

    /// Saves the picked photos; a photo that cannot be read is skipped.
    static func save(_ items: [PhotosPickerItem]) async -> [ChatAttachment] {
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        var saved: [ChatAttachment] = []
        for item in items {
            guard let data = try? await item.loadTransferable(type: Data.self),
                  let image = UIImage(data: data),
                  let photo = write(image) else { continue }
            saved.append(photo)
        }
        return saved
    }

    private static func write(_ image: UIImage) -> ChatAttachment? {
        let scale = min(1, maxSide / max(image.size.width, image.size.height))
        let size = CGSize(width: (image.size.width * scale).rounded(), height: (image.size.height * scale).rounded())
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let scaled = UIGraphicsImageRenderer(size: size, format: format).image { _ in
            image.draw(in: CGRect(origin: .zero, size: size))
        }
        guard let jpeg = scaled.jpegData(compressionQuality: quality) else { return nil }
        let file = folder.appendingPathComponent(UUID().uuidString + ".jpg")
        guard (try? jpeg.write(to: file)) != nil else { return nil }
        return ChatAttachment(path: file.path, width: Int32(size.width), height: Int32(size.height))
    }
}

/// A photo of the chat from the app's folder, read off the main thread.
struct ChatPhoto: View {
    let photo: ChatAttachment

    @State private var image: UIImage?

    var body: some View {
        ZStack {
            AppColorKeyContainer.secondary.color
            if let image {
                Image(uiImage: image).resizable().aspectRatio(contentMode: .fill)
            }
        }
        .clipped()
        .task(id: photo.path) {
            let url = ChatPhotos.url(of: photo)
            image = await Task.detached { UIImage(contentsOfFile: url.path) }.value
        }
    }
}
