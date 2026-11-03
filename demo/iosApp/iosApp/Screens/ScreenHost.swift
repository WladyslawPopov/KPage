import SwiftUI
import SharedLogic

/// Picks the UI of a component.
struct ScreenHost: View {
    let component: ScreenComponent

    var body: some View {
        switch component {
        case let c as SpaceComponent: SpaceScreen(component: c)
        case let c as ChatComponent: ChatScreen(component: c)
        case let c as LabComponent: LabScreen(component: c)
        case let c as CacheComponent: CacheScreen(component: c)
        case let c as SettingsComponent: SettingsScreen(component: c)
        default: Text("No UI for \(String(describing: component))")
        }
    }
}
