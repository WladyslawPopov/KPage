import SwiftUI
import UIKit

private let maxZoom: CGFloat = 5
private let doubleTapZoom: CGFloat = 2.5

/// Puts a SwiftUI view into a `UIScrollView` that zooms it, as in Photos: it zooms around the fingers
/// and keeps the inertia and the bounce at the edges, which SwiftUI gestures cannot give. A UIKit view
/// inside SwiftUI is a `UIViewRepresentable`: SwiftUI creates it once (`makeUIView`) and calls
/// `updateUIView` on changes. The `Coordinator` is the object that lives as long as the view and
/// receives its UIKit callbacks.
///
/// `onDrag` gets the vertical shift of the finger while the unzoomed picture is dragged; `onDragEnd`
/// the shift and the speed (points per second) when the finger is lifted.
struct ZoomableView<Content: View>: UIViewRepresentable {
    let onDrag: (CGFloat) -> Void
    let onDragEnd: (CGFloat, CGFloat) -> Void
    @ViewBuilder let content: () -> Content

    func makeCoordinator() -> Coordinator { Coordinator(host: UIHostingController(rootView: content())) }

    func makeUIView(context: Context) -> FittingScrollView {
        let coordinator = context.coordinator
        let scroll = FittingScrollView()
        scroll.delegate = coordinator
        scroll.minimumZoomScale = 1
        scroll.maximumZoomScale = maxZoom
        scroll.showsHorizontalScrollIndicator = false
        scroll.showsVerticalScrollIndicator = false
        scroll.contentInsetAdjustmentBehavior = .never
        scroll.backgroundColor = .clear

        let host = coordinator.host
        host.view.backgroundColor = .clear
        host.safeAreaRegions = []
        scroll.content = host.view
        scroll.addSubview(host.view)

        let doubleTap = UITapGestureRecognizer(target: coordinator, action: #selector(Coordinator.doubleTapped))
        doubleTap.numberOfTapsRequired = 2
        scroll.addGestureRecognizer(doubleTap)

        // One finger only, so it never fights the pinch; it works only while the picture is not zoomed.
        let pan = UIPanGestureRecognizer(target: coordinator, action: #selector(Coordinator.panned))
        pan.maximumNumberOfTouches = 1
        pan.delegate = coordinator
        scroll.addGestureRecognizer(pan)

        coordinator.scroll = scroll
        coordinator.dismissPan = pan
        return scroll
    }

    func updateUIView(_ scroll: FittingScrollView, context: Context) {
        context.coordinator.onDrag = onDrag
        context.coordinator.onDragEnd = onDragEnd
        context.coordinator.host.rootView = content()
    }

    @MainActor
    final class Coordinator: NSObject, UIScrollViewDelegate, UIGestureRecognizerDelegate {
        let host: UIHostingController<Content>
        var onDrag: (CGFloat) -> Void = { _ in }
        var onDragEnd: (CGFloat, CGFloat) -> Void = { _, _ in }
        weak var scroll: UIScrollView?
        weak var dismissPan: UIPanGestureRecognizer?

        init(host: UIHostingController<Content>) { self.host = host }

        func viewForZooming(in scrollView: UIScrollView) -> UIView? { host.view }

        @objc func doubleTapped(_ gesture: UITapGestureRecognizer) {
            guard let scroll else { return }
            if scroll.zoomScale > 1 {
                scroll.setZoomScale(1, animated: true)
                return
            }
            // Zoom into the place that was tapped.
            let point = gesture.location(in: host.view)
            let size = CGSize(
                width: scroll.bounds.width / doubleTapZoom,
                height: scroll.bounds.height / doubleTapZoom
            )
            let rect = CGRect(x: point.x - size.width / 2, y: point.y - size.height / 2, width: size.width, height: size.height)
            scroll.zoom(to: rect, animated: true)
        }

        // The shift is measured in the window (`in: nil`): the view moves while it is dragged, and a shift
        // measured in a moving view would shrink as the picture follows the finger.
        @objc func panned(_ gesture: UIPanGestureRecognizer) {
            let shift = gesture.translation(in: nil).y
            switch gesture.state {
            case .changed:
                onDrag(shift)
            case .ended:
                onDragEnd(shift, gesture.velocity(in: nil).y)
            case .cancelled, .failed:
                onDragEnd(shift, 0)
            default:
                break
            }
        }

        func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
            guard let pan = gestureRecognizer as? UIPanGestureRecognizer, pan === dismissPan, let scroll else { return true }
            let velocity = pan.velocity(in: nil)
            return scroll.zoomScale <= 1.01 && abs(velocity.y) > abs(velocity.x)
        }

        func gestureRecognizer(
            _ gestureRecognizer: UIGestureRecognizer,
            shouldRecognizeSimultaneouslyWith other: UIGestureRecognizer
        ) -> Bool { true }
    }
}

/// A scroll view that keeps its content as large as itself while it is not zoomed, so the picture
/// always fits the screen and follows a rotation.
final class FittingScrollView: UIScrollView {
    var content: UIView?

    override func layoutSubviews() {
        super.layoutSubviews()
        guard let content, zoomScale == 1, content.frame.size != bounds.size else { return }
        content.frame = bounds
        contentSize = bounds.size
    }
}
