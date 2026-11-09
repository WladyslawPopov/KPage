import SwiftUI

/// A read-only text whose words can be selected with the system handles, as on Android. SwiftUI's own
/// `.textSelection(.enabled)` on `Text` only copies the whole text on iOS, so this is a UIKit
/// `UITextView` that cannot be edited or scrolled: it only shows the text and lets it be selected.
struct SelectableText: UIViewRepresentable {
    let text: String
    var textStyle: UIFont.TextStyle = .body

    func makeUIView(context: Context) -> UITextView {
        let view = UITextView()
        view.isEditable = false
        view.isSelectable = true
        view.isScrollEnabled = false
        view.backgroundColor = .clear
        // No inner margins: the text lines up with a SwiftUI `Text` placed in the same spot.
        view.textContainerInset = .zero
        view.textContainer.lineFragmentPadding = 0
        view.font = .preferredFont(forTextStyle: textStyle)
        view.adjustsFontForContentSizeCategory = true
        view.textColor = .label
        return view
    }

    func updateUIView(_ view: UITextView, context: Context) {
        if view.text != text { view.text = text }
    }

    /// The height of the text for the width SwiftUI offers; without it the view would have none.
    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UITextView, context: Context) -> CGSize? {
        guard let width = proposal.width, width.isFinite else { return nil }
        let size = uiView.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude))
        return CGSize(width: width, height: size.height)
    }
}
