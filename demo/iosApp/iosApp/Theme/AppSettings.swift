import SwiftUI
import SharedLogic

/// The theme and the language of the app, one for the whole UI.
///
/// `@Observable` makes every view whose `body` reads a property draw again when it changes (like a
/// Compose function reading a `State`). That is how a new language reaches every text without
/// recreating the screens: `AppStringKey.text` reads `bundle`.
@Observable
final class AppSettings {
    static let shared = AppSettings()

    private(set) var themeMode: ThemeMode
    private(set) var language: AppLanguage
    /// The strings of `language`: its `<tag>.lproj` folder in the app, the app's main bundle if there is none.
    private(set) var bundle: Bundle

    @ObservationIgnored private let observer: AppSettingsObserver
    @ObservationIgnored private var binding: UiBinding?

    private init() {
        let observer = IosKoinKt.appSettingsObserver()
        self.observer = observer
        themeMode = observer.themeMode
        language = observer.language
        bundle = Self.bundle(for: observer.language)
    }

    /// Follows the repository from now on. The object lives as long as the app, so the subscription is never ended.
    func start() {
        guard binding == nil else { return }
        binding = observer.observe { self.update() }
    }

    /// `nil` follows the system.
    var colorScheme: ColorScheme? {
        switch themeMode {
        case .light: return .light
        case .dark: return .dark
        // A Kotlin enum is not exhaustive in Swift: `system` and anything new follow the system.
        default: return nil
        }
    }

    var locale: Locale { Locale(identifier: language.tag) }

    private func update() {
        themeMode = observer.themeMode
        language = observer.language
        bundle = Self.bundle(for: language)
    }

    private static func bundle(for language: AppLanguage) -> Bundle {
        guard let path = Bundle.main.path(forResource: language.tag, ofType: "lproj"),
              let bundle = Bundle(path: path) else { return .main }
        return bundle
    }
}
