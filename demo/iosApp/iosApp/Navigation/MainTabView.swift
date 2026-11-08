import SwiftUI
import SharedLogic

struct MainTabView: View {
    @SceneStorage("navigation") private var savedNavigation = ""
    @State private var navigator: IosNavigator?

    var body: some View {
        // A real container (not Group): it is on screen before the navigator exists, so onAppear fires.
        ZStack {
            Color.clear
            if let navigator { TabsContent(navigator: navigator) }
        }
        .onAppear {
            guard navigator == nil else { return }
            navigator = IosNavigator(
                factory: IosKoinKt.screenFactory(),
                saved: NavigationSnapshotCodec.shared.decode(text: savedNavigation)
            )
        }
        .onChange(of: navigator?.savedState) { _, text in
            if let text { savedNavigation = text }
        }
    }
}

private struct TabsContent: View {
    @Bindable var navigator: IosNavigator
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        TabView(selection: $navigator.selectedTab) {
            ForEach(AppTab.entries, id: \.self) { tab in
                NavigationStack(path: navigator.path(for: tab)) {
                    if let root = navigator.roots[tab] {
                        ScreenHost(entry: root)
                    }
                }
                .navigationDestination(for: StackEntry.self) { ScreenHost(entry: $0) }
                .tabItem { Label(tab.title.text, systemImage: tab.icon.sfSymbol) }
                .tag(tab)
            }
        }
        .modifier(MinimizeTabBarOnScroll())
        .onAppear { navigator.setAppActive(scenePhase == .active) }
        .onChange(of: scenePhase) { _, phase in navigator.setAppActive(phase == .active) }
    }
}

/// The tab bar folds into one button while a list is scrolled down and comes back when it is tapped,
/// as in system apps. Only iOS 26 has it; before it the bar stays as it is.
private struct MinimizeTabBarOnScroll: ViewModifier {
    @ViewBuilder func body(content: Content) -> some View {
        if #available(iOS 26, *) {
            content.tabBarMinimizeBehavior(.onScrollDown)
        } else {
            content
        }
    }
}
