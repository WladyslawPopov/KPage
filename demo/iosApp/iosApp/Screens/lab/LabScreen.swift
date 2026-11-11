import SwiftUI
import SharedLogic

struct LabScreen: View {
    let component: LabComponent
    let state: LabState

    var body: some View {
        // The page shows "coming soon" as its empty state, so there is no content yet.
        PageLayout(page: state.page, onPageIntent: { component.onPageIntent(intent: $0) }) {
            EmptyView()
        }
        .navigationTitle(state.topBar.title.text)
    }
}
