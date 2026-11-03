import SwiftUI
import SharedLogic

/// The Space UI: the shared code pushes the state into it.
@Observable
final class SpaceUiModel: NSObject, SpaceUi {
    private(set) var state: SpaceState?

    func renderSpace(state: SpaceState) { self.state = state }
}

struct SpaceScreen: View {
    let component: SpaceComponent
    @State private var model = SpaceUiModel()
    @State private var binding: UiBinding?

    var body: some View {
        // A real container (not Group): it is on screen before the first state, so onAppear binds.
        ZStack {
            Color.clear
            if let state = model.state {
                PlaceholderScreen(topBar: state.topBar, text: state.text)
            }
        }
        .onAppear { binding = component.bindSpace(ui: model) }
        .onDisappear {
            binding?.unbind()
            binding = nil
        }
    }
}

/// The Chat UI: the shared code pushes the state into it.
@Observable
final class ChatUiModel: NSObject, ChatUi {
    private(set) var state: ChatState?

    func renderChat(state: ChatState) { self.state = state }
}

struct ChatScreen: View {
    let component: ChatComponent
    @State private var model = ChatUiModel()
    @State private var binding: UiBinding?

    var body: some View {
        // A real container (not Group): it is on screen before the first state, so onAppear binds.
        ZStack {
            Color.clear
            if let state = model.state {
                PlaceholderScreen(topBar: state.topBar, text: state.text)
            }
        }
        .onAppear { binding = component.bindChat(ui: model) }
        .onDisappear {
            binding?.unbind()
            binding = nil
        }
    }
}

/// The Lab UI: the shared code pushes the state into it.
@Observable
final class LabUiModel: NSObject, LabUi {
    private(set) var state: LabState?

    func renderLab(state: LabState) { self.state = state }
}

struct LabScreen: View {
    let component: LabComponent
    @State private var model = LabUiModel()
    @State private var binding: UiBinding?

    var body: some View {
        // A real container (not Group): it is on screen before the first state, so onAppear binds.
        ZStack {
            Color.clear
            if let state = model.state {
                PlaceholderScreen(topBar: state.topBar, text: state.text)
            }
        }
        .onAppear { binding = component.bindLab(ui: model) }
        .onDisappear {
            binding?.unbind()
            binding = nil
        }
    }
}

/// The Cache UI: the shared code pushes the state into it.
@Observable
final class CacheUiModel: NSObject, CacheUi {
    private(set) var state: CacheState?

    func renderCache(state: CacheState) { self.state = state }
}

struct CacheScreen: View {
    let component: CacheComponent
    @State private var model = CacheUiModel()
    @State private var binding: UiBinding?

    var body: some View {
        // A real container (not Group): it is on screen before the first state, so onAppear binds.
        ZStack {
            Color.clear
            if let state = model.state {
                PlaceholderScreen(topBar: state.topBar, text: state.text)
            }
        }
        .onAppear { binding = component.bindCache(ui: model) }
        .onDisappear {
            binding?.unbind()
            binding = nil
        }
    }
}

/// The Settings UI: the shared code pushes the state into it.
@Observable
final class SettingsUiModel: NSObject, SettingsUi {
    private(set) var state: SettingsState?

    func renderSettings(state: SettingsState) { self.state = state }
}

struct SettingsScreen: View {
    let component: SettingsComponent
    @State private var model = SettingsUiModel()
    @State private var binding: UiBinding?

    var body: some View {
        // A real container (not Group): it is on screen before the first state, so onAppear binds.
        ZStack {
            Color.clear
            if let state = model.state {
                PlaceholderScreen(topBar: state.topBar, text: state.text)
            }
        }
        .onAppear { binding = component.bindSettings(ui: model) }
        .onDisappear {
            binding?.unbind()
            binding = nil
        }
    }
}
