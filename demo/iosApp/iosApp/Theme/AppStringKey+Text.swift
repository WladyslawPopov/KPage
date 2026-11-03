import SharedLogic

extension AppStringKey {
    /// The text of an interface string.
    var text: String { IosStrings.shared.get(key: self) }
}
