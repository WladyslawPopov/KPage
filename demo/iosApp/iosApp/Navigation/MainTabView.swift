import SwiftUI
import SharedLogic

struct MainTabView: View {
    @State private var navigator = IosNavigator(factory: IosKoinKt.screenFactory())
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        @Bindable var navigator = navigator
        TabView(selection: $navigator.selectedTab) {
            ForEach(AppTab.entries, id: \.self) { tab in
                NavigationStack(path: navigator.path(for: tab)) {
                    if let root = navigator.roots[tab] {
                        ScreenHost(component: root.component)
                    }
                }
                .navigationDestination(for: StackEntry.self) { ScreenHost(component: $0.component) }
                .tabItem { Label(tab.title.text, systemImage: tab.icon.sfSymbol) }
                .tag(tab)
            }
        }
        .onAppear { navigator.setAppActive(scenePhase == .active) }
        .onChange(of: scenePhase) { _, phase in navigator.setAppActive(phase == .active) }
    }
}
