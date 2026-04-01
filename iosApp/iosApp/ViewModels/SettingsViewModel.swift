import Foundation
import shared

@MainActor
class SettingsViewModel: ObservableObject {
    @Published var securityNotificationsEnabled: Bool = true
    private let preferencesRepo = IOSDependencies.shared.getUserPreferencesRepository()
    private let crashReporter: CrashReportingService
    private let currentUid = "dummy-uid"

    init(crashReporter: CrashReportingService = DependencyContainer.shared.crashReportingService) {
        self.crashReporter = crashReporter
    }

    func savePreferences() async {
        do {
            _ = try await preferencesRepo.setSecurityNotificationsEnabled(userId: currentUid, enabled: securityNotificationsEnabled)
        } catch {
            crashReporter.recordError(error)
        }
    }
}
