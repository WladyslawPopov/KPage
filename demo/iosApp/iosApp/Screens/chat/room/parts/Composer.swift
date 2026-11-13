import SwiftUI
import PhotosUI
import SharedLogic

/// The field under the messages: the photos to send, the photo picker, the text, the send button.
///
/// The typed text lives in a local `@State` and goes to Kotlin on every change: the state from Kotlin
/// comes a moment later, and a late answer must not overwrite what is being typed. A change from
/// outside (the draft restored, the field cleared after a send) is taken over with `onChange`.
struct Composer: View {
    let state: ChatRoomState
    let onIntent: (ChatRoomIntent) -> Void

    @State private var text = ""
    @State private var picked: [PhotosPickerItem] = []

    var body: some View {
        VStack(spacing: AppDimenKeyPadding.small.value) {
            if !state.attachments.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: AppDimenKeyPadding.small.value) {
                        ForEach(state.attachments, id: \.path) { photo in
                            ChatPhoto(photo: photo)
                                .frame(width: 80, height: 80)
                                .clipShape(RoundedRectangle(cornerRadius: AppDimenKeyCornerRadius.large.value))
                                .overlay(alignment: .topTrailing) {
                                    Button { onIntent(ChatRoomIntentRemovePhoto(photo: photo)) } label: {
                                        Image(systemName: "xmark.circle.fill")
                                            .font(.title3)
                                            .symbolRenderingMode(.palette)
                                            .foregroundStyle(.white, .black.opacity(0.55))
                                    }
                                    .buttonStyle(.plain)
                                    .padding(AppDimenKeyPadding.extraSmall.value)
                                    .accessibilityLabel(AppStringKeyCore.clear.text)
                                }
                        }
                    }
                    .padding(.horizontal, AppDimenKeyPadding.small.value)
                }
            }
            HStack(alignment: .bottom, spacing: AppDimenKeyPadding.small.value) {
                PhotosPicker(
                    selection: $picked,
                    maxSelectionCount: max(1, Int(ChatRoomState.companion.MAX_ATTACHMENTS) - state.attachments.count),
                    matching: .images
                ) {
                    Image(systemName: AppIconKey.attach.sfSymbol).font(.title3)
                }
                .disabled(!state.canAttach)
                TextField(AppStringKeyChat.messageHint.text, text: $text, axis: .vertical)
                    .lineLimit(1...5)
                    .padding(.horizontal, AppDimenKeyPadding.medium.value)
                    .padding(.vertical, AppDimenKeyPadding.small.value)
                    .background(AppColorKeyContainer.background.color, in: RoundedRectangle(cornerRadius: 18))
                    .uiTestId(.messageField)
                Button { onIntent(ChatRoomIntentSend.shared) } label: {
                    Image(systemName: AppIconKey.send.sfSymbol).font(.title2)
                }
                .disabled(!state.canSend)
                .uiTestId(.send)
            }
            .padding(.horizontal, AppDimenKeyPadding.small.value)
        }
        .padding(.vertical, AppDimenKeyPadding.small.value)
        .background(.bar)
        .onAppear { text = state.text }
        .onChange(of: text) { _, new in
            if new != state.text { onIntent(ChatRoomIntentTextChanged(text: new)) }
        }
        .onChange(of: state.text) { _, new in
            if new != text { text = new }
        }
        .onChange(of: picked) { _, items in
            guard !items.isEmpty else { return }
            picked = []
            Task {
                let photos = await ChatPhotos.save(items)
                if !photos.isEmpty { onIntent(ChatRoomIntentAddPhotos(photos: photos)) }
            }
        }
    }
}
