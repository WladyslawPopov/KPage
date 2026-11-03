import SwiftUI
import SharedLogic

/// Navigation of the app: a stack per tab (a root and a path above it). Creates the components of the
/// routes, destroys them when they leave a stack and decides which one is shown.
@Observable
final class IosNavigator: NSObject, AppNavigator {
    @ObservationIgnored private let factory: ScreenFactory
    @ObservationIgnored private var nextId = 0
    @ObservationIgnored private var isAppActive = false

    private(set) var roots: [AppTab: StackEntry] = [:]
    private var paths: [AppTab: [StackEntry]] = [:]

    var selectedTab: AppTab = .space {
        didSet { updateLifecycle() }
    }

    init(factory: ScreenFactory) {
        self.factory = factory
        super.init()
        for tab in AppTab.entries {
            roots[tab] = create(route: tab.root)
            paths[tab] = []
        }
    }

    func open(route: Route) {
        paths[selectedTab, default: []].append(create(route: route))
        updateLifecycle()
    }

    func back() -> Bool {
        guard let last = paths[selectedTab]?.last else { return false }
        paths[selectedTab]?.removeLast()
        last.context.destroy()
        updateLifecycle()
        return true
    }

    /// The path of a tab for NavigationStack; entries removed by the user (swipe back) are destroyed.
    func path(for tab: AppTab) -> Binding<[StackEntry]> {
        Binding(
            get: { self.paths[tab] ?? [] },
            set: { newPath in
                let removed = (self.paths[tab] ?? []).filter { !newPath.contains($0) }
                self.paths[tab] = newPath
                removed.forEach { $0.context.destroy() }
                self.updateLifecycle()
            }
        )
    }

    func setAppActive(_ active: Bool) {
        isAppActive = active
        updateLifecycle()
    }

    private func updateLifecycle() {
        for tab in AppTab.entries {
            let stack = [roots[tab]].compactMap { $0 } + (paths[tab] ?? [])
            for (index, entry) in stack.enumerated() {
                let shown = isAppActive && tab == selectedTab && index == stack.count - 1
                if entry.isShown == shown { continue }
                entry.isShown = shown
                if shown { entry.context.show() } else { entry.context.hide() }
                print("KPageDemo: \(entry.route) \(shown ? "shown" : "hidden")")
            }
        }
    }

    private func create(route: Route) -> StackEntry {
        let context = ComponentContext()
        let entry = StackEntry(
            id: nextId,
            route: route,
            context: context,
            component: factory.create(route: route, context: context, navigator: self)
        )
        nextId += 1
        return entry
    }
}
