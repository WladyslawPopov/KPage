import SwiftUI
import SharedLogic

struct SettingsScreen: View {
    let component: SettingsComponent
    let state: SettingsState

    var body: some View {
        PageLayout(page: state.page, onPageIntent: { component.onPageIntent(intent: $0) }) {
            List {
                SettingsOptionsSection(title: AppStringKeySettings.theme.text, options: state.themes) {
                    component.onIntent(intent: SettingsIntentSetTheme(mode: $0))
                }
                SettingsOptionsSection(title: AppStringKeySettings.language.text, options: state.languages) {
                    component.onIntent(intent: SettingsIntentSetLanguage(language: $0))
                }
                Section(AppStringKeySettings.cache.text) {
                    LabeledContent(AppStringKeySettings.cacheLists.text) { Text(state.cacheLists.string) }
                    LabeledContent(AppStringKeySettings.cacheSize.text) { Text(state.cacheSize.string) }
                    Button(AppStringKeySettings.clearCache.text, role: .destructive) {
                        component.onIntent(intent: SettingsIntentClearCache.shared)
                    }
                }
            }
        }
        .navigationTitle(state.topBar.title.text)
        // The state decides when it is shown; the buttons answer through it, so the binding only reads.
        .alert(AppStringKeySettings.clearCacheTitle.text, isPresented: .constant(state.confirmClear)) {
            Button(AppStringKeySettings.clearCache.text, role: .destructive) {
                component.onIntent(intent: SettingsIntentConfirmClearCache.shared)
            }
            Button(AppStringKeyCore.cancel.text, role: .cancel) {
                component.onIntent(intent: SettingsIntentCancelClearCache.shared)
            }
        } message: {
            Text(AppStringKeySettings.clearCacheMessage.text)
        }
    }
}
