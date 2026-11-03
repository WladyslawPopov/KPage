import SharedLogic

/// One screen in a stack: its route, lifecycle and component live while it is in the stack.
final class StackEntry: Hashable, Identifiable {
    let id: Int
    let route: Route
    let context: ComponentContext
    let component: ScreenComponent
    /// What the navigator last told the context.
    var isShown = false

    init(id: Int, route: Route, context: ComponentContext, component: ScreenComponent) {
        self.id = id
        self.route = route
        self.context = context
        self.component = component
    }

    static func == (lhs: StackEntry, rhs: StackEntry) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}
