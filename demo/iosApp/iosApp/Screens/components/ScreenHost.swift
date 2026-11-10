import SwiftUI
import SharedLogic

/// Picks the UI of a stack entry's component and subscribes it to the component's state. The only
/// place where a screen meets its context: screens get the component and a ready state.
struct ScreenHost: View {
    let entry: StackEntry

    var body: some View {
        switch entry.component {
        case let c as SpaceComponent:
            StateHost(observer: observer(c)) { (state: SpaceState) in SpaceScreen(component: c, state: state) }
        case let c as SpaceDetailsComponent:
            StateHost(observer: observer(c)) { (state: SpaceDetailsState) in SpaceDetailsScreen(component: c, state: state) }
        case let c as FavoritesComponent:
            StateHost(observer: observer(c)) { (state: FavoritesState) in FavoritesScreen(component: c, state: state) }
        case let c as ChatsComponent:
            StateHost(observer: observer(c)) { (state: ChatsState) in ChatsScreen(component: c, state: state) }
        case let c as ChatRoomComponent:
            StateHost(observer: observer(c)) { (state: ChatRoomState) in ChatRoomScreen(component: c, state: state) }
        case let c as LabComponent:
            StateHost(observer: observer(c)) { (state: LabState) in LabScreen(component: c, state: state) }
        case let c as SettingsComponent:
            StateHost(observer: observer(c)) { (state: SettingsState) in SettingsScreen(component: c, state: state) }
        default:
            Text("No UI for \(String(describing: entry.component))")
        }
    }

    /// The state type comes from the caller: Kotlin interfaces lose their generics in Swift.
    private func observer<S: AnyObject>(_ owner: StateOwner) -> StateObserver<S> {
        StateObserver(state: owner.state, context: entry.context)
    }
}
