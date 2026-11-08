import SwiftUI

extension View {
    /// Liquid Glass from iOS 26; a system material before it. `interactive` makes the glass react to touches.
    @ViewBuilder func glassBackground<S: Shape>(in shape: S, interactive: Bool = false) -> some View {
        if #available(iOS 26, *) {
            glassEffect(interactive ? .regular.interactive() : .regular, in: shape)
        } else {
            background(.regularMaterial, in: shape)
        }
    }
}
