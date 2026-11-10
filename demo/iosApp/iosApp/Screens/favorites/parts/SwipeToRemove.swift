import SwiftUI
import SharedLogic

/// A swipe to the start asks to remove the row. The row always springs back: it leaves the list only
/// when the answer is yes and the list renews.
///
/// Made by hand because `.swipeActions` works only in a `List`, and the paged list is a lazy grid.
/// The drag is read together with the scroll and takes over only a mostly horizontal move.
struct SwipeToRemove<Content: View>: View {
    let isEnabled: Bool
    let onRemove: () -> Void
    @ViewBuilder let content: () -> Content

    @State private var offset: CGFloat = 0

    private let threshold: CGFloat = 96

    var body: some View {
        content()
            // Opaque, so the red shows only where the row has moved away.
            .background(AppColorKeyContainer.background.color)
            .offset(x: offset)
            .background(alignment: .trailing) {
                ZStack(alignment: .trailing) {
                    AppColorKeyStatus.fail.color
                    Image(systemName: AppIconKey.trash.sfSymbol)
                        .foregroundStyle(AppColorKeyIcon.onAccent.color)
                        .padding(.trailing, AppDimenKeyPadding.large.value)
                }
                .opacity(offset < 0 ? 1 : 0)
            }
            .simultaneousGesture(drag)
    }

    private var drag: some Gesture {
        DragGesture(minimumDistance: 20)
            .onChanged { value in
                guard isEnabled else { return }
                // A vertical move is the scroll's.
                guard offset != 0 || abs(value.translation.width) > abs(value.translation.height) else { return }
                offset = min(0, value.translation.width)
            }
            .onEnded { _ in
                if isEnabled, offset < -threshold { onRemove() }
                withAnimation(.spring(duration: 0.3)) { offset = 0 }
            }
    }
}
