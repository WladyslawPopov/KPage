import SwiftUI
import SharedLogic

/// A failure shown over the page without hiding it: close it, or retry when the page allows.
struct PageErrorBanner: View {
    let error: ScreenError
    let canRetry: Bool
    let onClose: () -> Void
    let onRetry: () -> Void

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: AppDimenKeyCornerRadius.large.value)
        HStack(spacing: AppDimenKeyPadding.small.value) {
            Button(action: onClose) {
                Image(systemName: AppIconKey.close.sfSymbol)
            }
            .tint(AppColorKeyStatus.fail.color)
            .accessibilityLabel(AppStringKeyCore.close.text)

            Text(error.displayText)
                .font(AppTypographyKey.body.font)
                .foregroundStyle(AppColorKeyText.primary.color)
                .frame(maxWidth: .infinity, alignment: .leading)

            Button(action: onRetry) {
                Image(systemName: AppIconKey.refresh.sfSymbol)
            }
            .disabled(!canRetry)
            .accessibilityLabel(AppStringKeyCore.retry.text)
        }
        .padding(AppDimenKeyPadding.medium.value)
        .background(.regularMaterial, in: shape)
        .overlay(shape.stroke(AppColorKeyStatus.fail.color, lineWidth: 1))
        .frame(maxWidth: 560)
    }
}

private extension ScreenError {
    /// The server's text when it gave one, else ours; the code is added for support.
    var displayText: String {
        let text = message.flatMap { $0.isEmpty ? nil : $0 } ?? title.text
        guard let code else { return text }
        return "\(text) (\(code))"
    }
}

/// Shows the page toast and hides it with an animation; a tap dismisses it.
struct PageToastHost: View {
    let toast: PageToast?
    let onTap: () -> Void

    var body: some View {
        ZStack {
            if let toast {
                HStack(spacing: AppDimenKeyPadding.small.value) {
                    Image(systemName: toast.type.icon.sfSymbol)
                        .foregroundStyle(toast.type.tint.color)
                    Text(toast.text.text)
                        .font(AppTypographyKey.body.font)
                        .foregroundStyle(AppColorKeyText.primary.color)
                }
                .padding(.horizontal, AppDimenKeyPadding.large.value)
                .padding(.vertical, AppDimenKeyPadding.medium.value)
                .background(.regularMaterial, in: Capsule())
                .shadow(radius: 4)
                .onTapGesture(perform: onTap)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                // A new toast with the same text replaces the old one instead of keeping it.
                .id(toast.id)
            }
        }
        .padding(AppDimenKeyPadding.medium.value)
        .animation(.spring, value: toast?.id)
    }
}

private extension ToastType {
    var icon: AppIconKey {
        switch self {
        case .success: return .success
        case .warning: return .warning
        case .fail: return .fail
        default: return .info
        }
    }

    var tint: AppColorKey {
        switch self {
        case .success: return AppColorKeyStatus.success
        case .warning: return AppColorKeyStatus.warning
        case .fail: return AppColorKeyStatus.fail
        default: return AppColorKeyIcon.accent
        }
    }
}

/// Shown instead of the page content. Scrollable, so the page can still be pulled to refresh; the
/// button sends the action of the empty state.
struct PageEmptyContent: View {
    let empty: PageEmptyState
    let onAction: (PageIntent) -> Void

    var body: some View {
        ScrollView {
            ContentUnavailableView {
                Label(empty.title.text, systemImage: empty.icon.sfSymbol)
            } actions: {
                if let action = empty.action, let actionText = empty.actionText {
                    Button(actionText.text) { onAction(action) }
                        .buttonStyle(.borderedProminent)
                }
            }
            .containerRelativeFrame(.vertical)
        }
    }
}
