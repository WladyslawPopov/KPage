import SwiftUI
import SharedLogic

extension UiText {
    /// The text of a `UiText`: strings from the catalog, a file size, a date and a time as the system formats them.
    /// (Not `text`: `UiTextPlain` and `UiTextKeyWith` already have a property of that name.)
    var string: String {
        switch self {
        case let value as UiTextPlain: return value.text
        case let value as UiTextKey: return value.key.text
        case let value as UiTextKeyWith: return "\(value.key.text) \(value.text)"
        case let value as UiTextFileSize: return ByteCountFormatter.string(fromByteCount: value.bytes, countStyle: .file)
        case let value as UiTextDate:
            return value.epochMillis.date.formatted(Date.FormatStyle(date: .abbreviated, time: .omitted).locale(AppSettings.shared.locale))
        case let value as UiTextTime:
            return value.epochMillis.date.formatted(Date.FormatStyle(date: .omitted, time: .shortened).locale(AppSettings.shared.locale))
        default: return ""
        }
    }
}

private extension Int64 {
    /// Epoch milliseconds from Kotlin as a date.
    var date: Date { Date(timeIntervalSince1970: TimeInterval(self) / 1000) }
}

extension AppStringKey {
    /// The text of an interface string, read from `Localizable.xcstrings` when it is shown, in the
    /// language of the app (`AppSettings.bundle`, which a view reads, so it is drawn again on a change).
    /// A missing string shows its key.
    var text: String { String(localized: String.LocalizationValue(catalogKey), bundle: AppSettings.shared.bundle) }

    /// The catalog key: the group and the entry name, as `core_no_internet_title`.
    /// A new group of AppStringKey must be added here and to the catalog.
    private var catalogKey: String {
        switch self {
        case let key as AppStringKeyCore: return "core_" + key.name.lowercased()
        case let key as AppStringKeyTabs: return "tabs_" + key.name.lowercased()
        case let key as AppStringKeyFavorites: return "favorites_" + key.name.lowercased()
        case let key as AppStringKeyChat: return "chat_" + key.name.lowercased()
        case let key as AppStringKeySpace: return "space_" + key.name.lowercased()
        case let key as AppStringKeySettings: return "settings_" + key.name.lowercased()
        case let key as AppStringKeyPlaceholder: return "placeholder_" + key.name.lowercased()
        default: return String(describing: self)
        }
    }
}
