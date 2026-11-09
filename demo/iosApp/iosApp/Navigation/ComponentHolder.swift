import SharedLogic

/// Creates, keeps and destroys the components of the app: a root and a path per tab. Moves their
/// lifecycles by one rule: only the top screen of the selected tab is shown, and only while the app is.
@Observable
final class ComponentHolder {
    @ObservationIgnored private let createComponent: (Route, ComponentContext) -> ScreenComponent
    @ObservationIgnored private var nextId = 0

    private(set) var roots: [AppTab: StackEntry] = [:]
    private var paths: [AppTab: [StackEntry]] = [:]

    init(
        initialPaths: [AppTab: [Route]] = [:],
        createComponent: @escaping (Route, ComponentContext) -> ScreenComponent
    ) {
        self.createComponent = createComponent
        for tab in AppTab.entries {
            roots[tab] = create(route: tab.root)
            paths[tab] = (initialPaths[tab] ?? []).map { create(route: $0) }
        }
    }

    func path(for tab: AppTab) -> [StackEntry] { paths[tab] ?? [] }

    /// The routes of every tab above its root.
    func snapshotPaths() -> [AppTab: [Route]] { paths.mapValues { $0.map(\.route) } }

    /// Opens the route on top of the tab. A route is the identity of its screen: one already in the
    /// stack moves to the top with its component, so the same screen is never open twice.
    func push(tab: AppTab, route: Route) {
        // Kotlin `equals` is `isEqual` of the object; the Route protocol itself has no equality in Swift.
        func isSame(_ other: Route) -> Bool { (other as? NSObject)?.isEqual(route) == true }
        if let root = roots[tab], isSame(root.route) { return }
        var path = paths[tab] ?? []
        if let index = path.firstIndex(where: { isSame($0.route) }) {
            path.append(path.remove(at: index))
        } else {
            path.append(create(route: route))
        }
        paths[tab] = path
    }

    /// Removes and destroys the top entry of the path; false when the path is empty.
    func pop(tab: AppTab) -> Bool {
        guard let last = paths[tab]?.last else { return false }
        paths[tab]?.removeLast()
        last.context.destroy()
        return true
    }

    /// Replaces the path (swipe back from NavigationStack); entries that are gone are destroyed.
    func setPath(_ newPath: [StackEntry], for tab: AppTab) {
        let removed = (paths[tab] ?? []).filter { !newPath.contains($0) }
        paths[tab] = newPath
        removed.forEach { $0.context.destroy() }
    }

    func updateLifecycle(selectedTab: AppTab, isAppActive: Bool) {
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
            component: createComponent(route, context)
        )
        nextId += 1
        return entry
    }
}
