import SwiftUI
import SharedLogic

/// The frame of every page: the content or the empty state, the error banner under the navigation
/// bar, the toast at the bottom and a loader while there is nothing to show. The error never hides
/// the content, so a cached list stays readable. With refresh enabled, pulling the content down sends
/// `PageIntent.Refresh`; the content must be scrollable for that. The title is the screen's own
/// `navigationTitle`.
struct PageLayout<Content: View>: View {
    let page: PageState
    let onPageIntent: (PageIntent) -> Void
    /// False when the content draws the empty state itself, for example inside its own scroll view.
    var showsEmpty: Bool = true
    @ViewBuilder let content: () -> Content

    var body: some View {
        Group {
            if showsEmpty, let empty = page.empty {
                PageEmptyContent(empty: empty, onAction: onPageIntent)
            } else {
                content()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppColorKeyContainer.background.color)
        .modifier(PullToRefresh(isEnabled: page.isRefreshEnabled) { onPageIntent(PageIntentRefresh.shared) })
        .safeAreaInset(edge: .top) {
            if let error = page.error {
                PageErrorBanner(
                    error: error,
                    canRetry: page.isRefreshEnabled && !page.isLoading,
                    onClose: { onPageIntent(PageIntentClearError.shared) },
                    onRetry: { onPageIntent(PageIntentRefresh.shared) }
                )
                .padding(.horizontal, AppDimenKeyPadding.small.value)
            }
        }
        .overlay(alignment: .bottom) {
            PageToastHost(toast: page.toast) { onPageIntent(PageIntentDismissToast.shared) }
        }
        .overlay {
            if page.isLoading { ProgressView() }
        }
    }
}

/// The system pull to refresh, only where the page allows it.
private struct PullToRefresh: ViewModifier {
    let isEnabled: Bool
    let action: () -> Void

    func body(content: Content) -> some View {
        if isEnabled {
            content.refreshable { action() }
        } else {
            content
        }
    }
}
