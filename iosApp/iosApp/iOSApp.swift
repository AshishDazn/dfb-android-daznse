import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        KoinIOS.shared.initialize()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
