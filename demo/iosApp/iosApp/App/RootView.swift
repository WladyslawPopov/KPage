import SwiftUI

struct RootView: View {
    private let settings = AppSettings.shared

    var body: some View {
        MainTabView()
            // nil follows the system theme.
            .preferredColorScheme(settings.colorScheme)
            .onAppear { settings.start() }
    }
}
