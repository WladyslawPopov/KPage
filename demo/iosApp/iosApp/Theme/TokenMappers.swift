import SwiftUI
import SharedLogic

// Kotlin enums are not exhaustive in Swift, so every switch has a default.

extension AppColorKey {
    var color: Color {
        switch self {
        case let key as AppColorKeyText:
            switch key {
            case .primary: return .primary
            case .secondary: return .secondary
            case .muted: return Color(.tertiaryLabel)
            case .accent: return .accentColor
            case .onAccent: return .white
            case .theme1: return .indigo
            case .theme2: return .teal
            default: return .primary
            }
        case let key as AppColorKeyContainer:
            switch key {
            case .background: return Color(.systemBackground)
            case .primary: return Color(.secondarySystemBackground)
            case .secondary: return Color(.tertiarySystemBackground)
            case .accent: return Color.accentColor.opacity(0.15)
            case .elevated: return Color(.secondarySystemGroupedBackground)
            case .theme1: return Color.indigo.opacity(0.15)
            case .theme2: return Color.teal.opacity(0.15)
            default: return Color(.systemBackground)
            }
        case let key as AppColorKeyStatus:
            switch key {
            case .success: return .green
            case .warning: return .yellow
            case .fail: return .red
            case .none: return .gray
            default: return .gray
            }
        case let key as AppColorKeyBorder:
            switch key {
            case .default_: return Color(.separator)
            case .accent: return .accentColor
            case .focused: return .accentColor
            default: return Color(.separator)
            }
        case let key as AppColorKeyIcon:
            switch key {
            case .primary: return .primary
            case .secondary: return .secondary
            case .accent: return .accentColor
            case .onAccent: return .white
            default: return .primary
            }
        case let key as AppColorKeyOverlay:
            switch key {
            case .scrim: return Color.black.opacity(0.4)
            case .disabled: return Color.gray.opacity(0.38)
            default: return Color.gray.opacity(0.38)
            }
        default:
            return .primary
        }
    }
}

extension AppDimenKey {
    var value: CGFloat {
        switch self {
        case let key as AppDimenKeyPadding:
            switch key {
            case .none: return 0
            case .extraSmall: return 4
            case .small: return 8
            case .medium: return 12
            case .large: return 16
            case .extraLarge: return 24
            default: return 0
            }
        case let key as AppDimenKeyCornerRadius:
            switch key {
            case .none: return 0
            case .small: return 4
            case .medium: return 8
            case .large: return 16
            case .full: return 999
            default: return 0
            }
        case let key as AppDimenKeyIconSize:
            switch key {
            case .extraSmall: return 12
            case .small: return 16
            case .medium: return 24
            case .large: return 32
            case .extraLarge: return 48
            default: return 24
            }
        default:
            return 0
        }
    }
}

extension AppTypographyKey {
    var font: Font {
        switch self {
        case .display: return .largeTitle
        case .headline: return .title
        case .title: return .title2
        case .subtitle: return .headline
        case .body: return .body
        case .label: return .subheadline
        case .caption: return .caption
        default: return .body
        }
    }
}

extension AppIconKey {
    var sfSymbol: String {
        switch self {
        case .back: return "chevron.left"
        case .close: return "xmark"
        case .refresh: return "arrow.clockwise"
        case .success: return "checkmark.circle"
        case .warning: return "exclamationmark.triangle"
        case .fail: return "xmark.octagon"
        case .info: return "info.circle"
        case .noInternet: return "wifi.slash"
        case .nothingFound: return "magnifyingglass"
        case .noImage: return "photo"
        case .search: return "magnifyingglass"
        case .filter: return "line.3.horizontal.decrease"
        case .dropdown: return "chevron.up.chevron.down"
        case .done: return "checkmark"
        case .favorite: return "heart.fill"
        case .favoriteOff: return "heart"
        case .layoutList: return "list.bullet"
        case .layoutGrid: return "square.grid.2x2"
        case .space: return "sparkles"
        case .chat: return "bubble.left.and.bubble.right"
        case .lab: return "flask"
        case .cache: return "externaldrive"
        case .settings: return "gearshape"
        default: return "questionmark"
        }
    }
}
