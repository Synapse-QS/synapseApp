import XCTest
@testable import iosApp

class DateFormatterUtilTests: XCTestCase {

    func testParseIso8601WithFractionalSeconds() {
        let dateString = "2023-10-27T10:15:30.123Z"
        let date = DateFormatterUtil.parse(dateString: dateString)
        XCTAssertNotNil(date)
    }

    func testParseIso8601WithoutFractionalSeconds() {
        let dateString = "2023-10-27T10:15:30Z"
        let date = DateFormatterUtil.parse(dateString: dateString)
        XCTAssertNotNil(date)
    }

    func testFormatDate() {
        let dateString = "2023-10-27T10:15:30.123Z"
        let formattedDate = DateFormatterUtil.formatDate(dateString: dateString)
        XCTAssertEqual(formattedDate, "2023-10-27")
    }

    func testFormatTime() {
        let dateString = "2023-10-27T10:15:30.123Z"
        let formattedTime = DateFormatterUtil.formatTime(dateString: dateString)
        // Note: This might depend on the timezone of the environment if not handled,
        // but ISO8601DateFormatter defaults to UTC for 'Z'.
        // DateFormatter defaults to local timezone.
        // Let's check what we expect.
        XCTAssertEqual(formattedTime, "10:15")
    }

    func testInvalidDateString() {
        let dateString = "invalid-date"
        let formattedDate = DateFormatterUtil.formatDate(dateString: dateString)
        XCTAssertEqual(formattedDate, "invalid-date")
    }
}
