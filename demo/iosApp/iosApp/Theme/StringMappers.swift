import SwiftUI
import SharedLogic

extension AppStringKey {
    /// The text of an interface string, read from `Localizable.xcstrings` when it is shown, so the
    /// system language applies. A missing string shows its key.
    var text: String { String(localized: String.LocalizationValue(catalogKey)) }

    /// The catalog key: the group and the entry name, as `core_no_internet_title`.
    /// A new group of AppStringKey must be added here and to the catalog.
    private var catalogKey: String {
        switch self {
        case let key as AppStringKeyCore: return "core_" + key.name.lowercased()
        case let key as AppStringKeyTabs: return "tabs_" + key.name.lowercased()
        case let key as AppStringKeyPlaceholder: return "placeholder_" + key.name.lowercased()
        default: return String(describing: self)
        }
    }
}
