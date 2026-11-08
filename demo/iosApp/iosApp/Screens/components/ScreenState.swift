import SwiftUI
import SharedLogic

/// Keeps a screen subscribed to its component while on screen and draws it for the latest state.
struct StateHost<S: AnyObject, Content: View>: View {
    let observer: StateObserver<S>
    @ViewBuilder let content: (S) -> Content
    @State private var screen = ScreenState<S>()

    var body: some View {
        // A real container (not Group): it is on screen before the first state, so onAppear binds.
        ZStack {
            Color.clear
            if let state = screen.value { content(state) }
        }
        .onAppear { screen.bind(observer) }
        .onDisappear { screen.unbind() }
    }
}

/// The last state of a component, for SwiftUI.
@Observable
private final class ScreenState<S: AnyObject> {
    private(set) var value: S?
    @ObservationIgnored private var binding: UiBinding?

    func bind(_ observer: StateObserver<S>) {
        guard binding == nil else { return }
        // weak: the Kotlin coroutine holds this closure; it must not keep the model alive.
        binding = observer.observe { [weak self] in self?.value = $0 }
    }

    func unbind() {
        binding?.unbind()
        binding = nil
    }
}
