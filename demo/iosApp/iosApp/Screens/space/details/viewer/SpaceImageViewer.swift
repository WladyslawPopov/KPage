import SwiftUI
import SharedLogic

/// The picture full screen on black: pinch or double tap zooms, a zoomed picture moves within its
/// edges. Closes by the cross or by dragging the unzoomed picture down or up.
///
/// The zoom is the system `UIScrollView` (see `ZoomableView`). The drag to close is reported by UIKit,
/// but the picture is moved by SwiftUI (`offset`): it owns the position of the view.
struct SpaceImageViewer: View {
    let url: String
    let onClose: () -> Void

    // How far the unzoomed picture is dragged from its place.
    @State private var dragOffset: CGFloat = 0

    var body: some View {
        ZStack(alignment: .topLeading) {
            // Still while the picture is dragged: only the picture moves, never a black frame behind it.
            Color.black.ignoresSafeArea()
            GeometryReader { geometry in
                ZoomableView(
                    onDrag: { dragOffset = $0 },
                    onDragEnd: { shift, velocity in finishDrag(shift: shift, velocity: velocity, height: geometry.size.height) }
                ) {
                    RemoteImage(url: url, contentMode: .fit, background: .clear)
                }
                .offset(y: dragOffset)
                .opacity(1 - min(abs(dragOffset) / max(geometry.size.height, 1), 1) * 0.6)
            }
            .ignoresSafeArea()
            // Inside the safe area: a touch at the very top edge of the screen is held back by the system.
            Button(action: onClose) {
                Image(systemName: AppIconKey.close.sfSymbol)
                    .font(.headline)
                    .foregroundStyle(.white)
                    .frame(width: 44, height: 44)
                    .glassBackground(in: Circle(), interactive: true)
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(AppStringKeyCore.close.text)
            .padding(AppDimenKeyPadding.large.value)
        }
        .presentationBackground(.black)
    }

    /// A long drag, or a fling that goes the same way as the drag, sends the picture away and closes;
    /// anything else brings it back. A fling alone is not enough: a tap-like flick must not close it.
    private func finishDrag(shift: CGFloat, velocity: CGFloat, height: CGFloat) {
        let isLong = abs(shift) > height * closeShare
        let isFling = abs(velocity) > closeVelocity && abs(shift) > minFlingShift && shift * velocity > 0
        if isLong || isFling {
            let target = (shift < 0 ? -1 : 1) * height
            withAnimation(.easeIn(duration: 0.2)) {
                dragOffset = target
            } completion: {
                onClose()
            }
        } else {
            withAnimation(.spring(duration: 0.35, bounce: 0.2)) { dragOffset = 0 }
        }
    }
}

// A drag longer than this share of the height closes the picture.
private let closeShare: CGFloat = 0.2
// A fling faster than this (points per second) closes it after a drag of at least `minFlingShift`.
private let closeVelocity: CGFloat = 1800
private let minFlingShift: CGFloat = 60
