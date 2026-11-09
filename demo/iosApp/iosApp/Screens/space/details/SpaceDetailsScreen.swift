import SwiftUI
import SharedLogic

/// The details of an image: the picture from the very top with the heart on it, the title and the
/// description, then the cards the state lists. A tappable row or chip opens the list filtered by it.
/// A tap on the picture opens it full screen; whether it is open is the state of this screen only.
struct SpaceDetailsScreen: View {
    let component: SpaceDetailsComponent
    let state: SpaceDetailsState

    @State private var isViewerShown = false

    var body: some View {
        PageLayout(page: state.page, onPageIntent: { component.onPageIntent(intent: $0) }) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    DetailsHero(
                        display: state.display,
                        onFavorite: { isFavorite in
                            component.onIntent(intent: SpaceDetailsIntentSetFavorite(isFavorite: isFavorite))
                        },
                        onOpen: { isViewerShown = true }
                    )
                    if let display = state.display {
                        DetailsContent(
                            display: display,
                            onIntent: { component.onIntent(intent: $0) },
                            onCopied: {
                                component.onPageIntent(intent: PageIntentShowToast(text: AppStringKeyCore.copied, type: .success))
                            }
                        )
                    }
                }
            }
            // The picture starts under the navigation bar: only the back button stays over it.
            .ignoresSafeArea(.container, edges: .top)
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .fullScreenCover(isPresented: $isViewerShown) {
            if let url = state.display?.fullPictureUrl {
                SpaceImageViewer(url: url) { isViewerShown = false }
            }
        }
    }
}
