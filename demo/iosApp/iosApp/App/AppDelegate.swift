import UIKit
import SharedLogic

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        IosKoinKt.startKoinIos()
        // AsyncImage loads through the shared URL session; the default cache is too small for lists.
        URLCache.shared = URLCache(memoryCapacity: 50 * 1024 * 1024, diskCapacity: 100 * 1024 * 1024)
        return true
    }
}
