import SwiftUI
import SharedLogic

/// Navigation of the app: the selected tab and app activity. The components live in
/// `ComponentHolder`, which is told about every change. `savedState` is the text of the current
/// navigation snapshot, for the scene storage.
@Observable
final class IosNavigator: NSObject, AppNavigator {
    @ObservationIgnored private let factory: ScreenFactory
    @ObservationIgnored private var holder: ComponentHolder!
    @ObservationIgnored private var isAppActive = false

    var roots: [AppTab: StackEntry] { holder.roots }

    private(set) var savedState = ""

    var selectedTab: AppTab = .space {
        didSet { changed() }
    }

    init(factory: ScreenFactory, saved: NavigationSnapshot? = nil) {
        self.factory = factory
        selectedTab = saved?.selectedTab ?? .space
        super.init()
        holder = ComponentHolder(initialPaths: saved?.paths ?? [:]) { [unowned self] route, context in
            factory.create(route: route, context: context, navigator: self)
        }
        savedState = encodeSnapshot()
    }

    func open(route: Route) {
        holder.push(tab: selectedTab, route: route)
        changed()
    }

    func back() -> Bool {
        guard holder.pop(tab: selectedTab) else { return false }
        changed()
        return true
    }

    /// The path of a tab for NavigationStack; entries removed by the user (swipe back) are destroyed.
    func path(for tab: AppTab) -> Binding<[StackEntry]> {
        Binding(
            get: { self.holder.path(for: tab) },
            set: { newPath in
                self.holder.setPath(newPath, for: tab)
                self.changed()
            }
        )
    }

    func setAppActive(_ active: Bool) {
        isAppActive = active
        updateLifecycle()
    }

    private func changed() {
        updateLifecycle()
        savedState = encodeSnapshot()
    }

    private func updateLifecycle() {
        holder.updateLifecycle(selectedTab: selectedTab, isAppActive: isAppActive)
    }

    private func encodeSnapshot() -> String {
        let snapshot = NavigationSnapshot(selectedTab: selectedTab, paths: holder.snapshotPaths())
        return NavigationSnapshotCodec.shared.encode(snapshot: snapshot)
    }
}
