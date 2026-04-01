import Foundation
import OSLog

protocol CrashReportingService {
    func recordError(_ error: Error)
    func setUserId(_ id: String)
    func log(_ message: String)
}

class DefaultCrashReportingService: CrashReportingService {
    private let logger = Logger(subsystem: Bundle.main.bundleIdentifier ?? "com.synapse.social", category: "CrashReporting")

    func recordError(_ error: Error) {
        // Placeholder for real crash reporting (e.g. Crashlytics)
        logger.error("💥 [Crashlytics] Recorded Error: \(error.localizedDescription)")
    }

    func setUserId(_ id: String) {
        logger.debug("💥 [Crashlytics] Set User ID: \(id)")
    }

    func log(_ message: String) {
        logger.debug("💥 [Crashlytics] Log: \(message)")
    }
}
