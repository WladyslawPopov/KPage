import SwiftUI
import SharedLogic

/// Loads the strings once, then shows the app.
struct RootView: View {
    @State private var isReady = false

    var body: some View {
        if isReady {
            MainTabView()
        } else {
            ProgressView().task {
                try? await IosStrings.shared.load()
                isReady = true
            }
        }
    }
}
