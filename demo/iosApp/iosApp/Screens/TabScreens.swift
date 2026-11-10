import SwiftUI
import SharedLogic

struct ChatScreen: View {
    let component: ChatComponent
    let state: ChatState

    var body: some View { PlaceholderScreen(topBar: state.topBar, text: state.text) }
}

struct LabScreen: View {
    let component: LabComponent
    let state: LabState

    var body: some View { PlaceholderScreen(topBar: state.topBar, text: state.text) }
}

struct SettingsScreen: View {
    let component: SettingsComponent
    let state: SettingsState

    var body: some View { PlaceholderScreen(topBar: state.topBar, text: state.text) }
}
