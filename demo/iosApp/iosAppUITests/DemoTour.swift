import XCTest

/// The demo tour on iOS, the story of `DemoTour` of the `tour` module on Android: Space (the grid, a
/// favorite, back to the list; a search by a word, a favorite in it) -> far down the found images, a
/// favorite -> its details (take the heart off, back) -> back up, more favorites -> Favorites (remove one
/// by a swipe) -> a chat room (create it, look through the history, send a message).
///
/// XCUITest runs out of the app's process: the app lives on the real clock and the gestures come from
/// the system, so the flicks glide and slow down as a person's do. Elements are found by the ids of
/// `UiTestId` (ui-core), set by `uiTestId` / `uiTestRow` in the app; the test does not link the shared
/// framework, so the ids are written here as strings. The tabs, the back button and the buttons of the
/// alerts have no ids: they are found by their place. The pauses are the pace of the video.
final class DemoTour: XCTestCase {
    @MainActor
    func testTour() throws {
        continueAfterFailure = false
        Quiescence.skip()
        let app = XCUIApplication()
        app.launch()
        let tour = Tour(app: app)
        tour.warmUp()
        tour.run()
    }
}

@MainActor
private struct Tour {
    let app: XCUIApplication

    /// The first showing of the keyboard and of every tab is slow; it is done once before the tour, and the
    /// recording is cut to start after it.
    func warmUp() {
        waitFor { count(Id.imageCard) > 0 }
        element(Id.searchOpen).tap()
        waitFor { count(Id.searchField) > 0 }
        look(Pause.long)
        element(Id.searchDone).tap()
        look(Pause.short)
        for tab in Tab.all.dropFirst() { openTab(tab) }
        openTab(Tab.space)
    }

    func run() {
        waitFor { count(Id.imageCard) > 0 }
        look(Pause.long)

        // The grid: the same images in two columns, a calm glide down them, a favorite. The layout switch
        // is shown only near the top (another layout starts the list from its first row), so back up first.
        element(Id.layoutToggle).tap()
        look(Pause.transition)
        flickUntil(up: true, velocity: Flick.gridVelocity, burst: 1) { $0 >= Position.grid - Position.gridCoast }
        settle()
        tapHeart(of: cardInMiddle())
        look(Pause.heart)
        flickUntilShown(Id.layoutToggle, velocity: Flick.gridVelocity)
        settle()
        element(Id.layoutToggle).tap()
        look(Pause.layoutHold)

        // Search: a word narrows the list; the check mark keeps it, and the tour goes on in it.
        element(Id.searchOpen).tap()
        waitFor { count(Id.searchField) > 0 }
        look(Pause.short)
        typeSlowly(Text.search)
        look(Pause.short)
        element(Id.searchDone).tap()
        waitFor { count(Id.imageCard) > 0 }
        look(Pause.long)
        for _ in 0..<2 { flick(up: true, velocity: Flick.softVelocity) }
        settle()
        tapHeart(of: cardInMiddle())
        look(Pause.heart)

        // Far down the found images, by the position counter, to show the paginator on a long list.
        flickUntil(up: true) { $0 >= Position.far - Position.coast }
        settle()
        let card = cardInMiddle()
        tapHeart(of: card)
        look(Pause.heart)

        // Its details: the heart is set there too; take it off and come back, the list shows it off.
        tap(at: CGPoint(x: card.midX, y: card.midY))
        waitFor { count(Id.imageCard) == 0 && count(Id.heart) > 0 }
        look(Pause.transition)
        element(Id.heart).tap()
        look(Pause.long)
        back()
        waitFor { count(Id.imageCard) > 0 }
        look(Pause.transition)

        // Back up, almost to the start of the list: a few more favorites there.
        flickUntil(up: false) { $0 <= Position.nearStart + Position.coast }
        settle()
        for _ in 0..<2 {
            scroll()
            tapHeart(of: cardInMiddle())
            look(Pause.heart)
        }

        // Favorites: they are there; remove one with a swipe.
        openTab(Tab.favorites)
        waitFor { count(Id.imageCard) > 0 }
        look(Pause.long)
        swipeLeft(over: frames(Id.imageCard)[0])
        waitFor { app.alerts.count > 0 }
        look(Pause.short)
        alertAction().tap()
        look(Pause.long)

        // Chat: create a room (the app opens it), look through its history, send a message.
        openTab(Tab.chat)
        look(Pause.short)
        element(Id.newChat).tap()
        waitFor { app.alerts.textFields.count >= 2 }
        look(Pause.short)
        app.alerts.textFields.element(boundBy: 0).tap()
        typeSlowly(Text.chatTitle)
        app.alerts.textFields.element(boundBy: 1).tap()
        typeSlowly(Text.chatInterlocutor)
        look(Pause.short)
        alertAction().tap()
        waitFor { count(Id.messageField) > 0 }
        // The room opens, then its history loads: give it time to appear before looking through it.
        look(Pause.chatLoad)
        for _ in 0..<2 { flick(up: false, velocity: Flick.softVelocity) }
        look(Pause.glide)
        element(Id.messageField).tap()
        look(Pause.short)
        typeSlowly(Text.message)
        look(Pause.short)
        waitFor { element(Id.send).isEnabled }
        element(Id.send).tap()
        // The message is in the list: the field is not a static text.
        waitFor { app.staticTexts[Text.message].exists }
        look(Pause.end)
    }

    // MARK: - Steps

    /// The card nearest to the middle of the screen whose middle and heart are clear of the translucent
    /// bars. A tile of the grid is too tall to lie wholly in the middle band, so only these two points count.
    private func cardInMiddle() -> CGRect {
        let height = app.frame.height
        let band = (height * 0.3)...(height * 0.8)
        let hearts = frames(Id.heart).map { CGPoint(x: $0.midX, y: $0.midY) }
        let card = frames(Id.imageCard)
            .filter { card in band.contains(card.midY) && hearts.contains { card.contains($0) && band.contains($0.y) } }
            .min { abs($0.midY - height / 2) < abs($1.midY - height / 2) }
        guard let card else {
            XCTFail("No card in the middle of the screen")
            return .zero
        }
        return card
    }

    /// The heart lying inside the card: the card and its heart are of the same image.
    private func tapHeart(of card: CGRect) {
        guard let heart = frames(Id.heart).first(where: { card.contains(CGPoint(x: $0.midX, y: $0.midY)) }) else {
            XCTFail("No heart inside the card")
            return
        }
        tap(at: CGPoint(x: heart.midX, y: heart.midY))
    }

    /// One slow swipe over the content, to the next images, and the wait until the list stands.
    private func scroll() {
        drag(from: CGVector(dx: 0.5, dy: 0.7), to: CGVector(dx: 0.5, dy: 0.35), velocity: Flick.scrollVelocity)
        settle()
    }

    /// A swipe of a card to the left, the removal gesture.
    private func swipeLeft(over card: CGRect) {
        let origin = app.coordinate(withNormalizedOffset: .zero)
        let from = origin.withOffset(CGVector(dx: card.minX + card.width * 0.9, dy: card.midY))
        let to = origin.withOffset(CGVector(dx: card.minX + card.width * 0.1, dy: card.midY))
        from.press(forDuration: Flick.hold, thenDragTo: to, withVelocity: XCUIGestureVelocity(Flick.swipeVelocity), thenHoldForDuration: 0)
    }

    /// A scroll down folds the tab bar into the button of the current tab; a tap on it opens the bar again.
    private func openTab(_ index: Int) {
        let buttons = app.tabBars.buttons
        if buttons.count <= index {
            buttons.firstMatch.tap()
            look(Pause.settle)
        }
        buttons.element(boundBy: index).tap()
        look(Pause.transition)
    }

    /// The back button of the navigation bar, or the swipe from the left edge where there is none.
    private func back() {
        let button = app.navigationBars.buttons.element(boundBy: 0)
        if button.exists && button.isHittable {
            button.tap()
        } else {
            drag(from: CGVector(dx: 0.01, dy: 0.5), to: CGVector(dx: 0.8, dy: 0.5), velocity: Flick.scrollVelocity)
        }
    }

    /// The action of a two-button alert: the cancel button is placed first.
    private func alertAction() -> XCUIElement {
        let buttons = app.alerts.firstMatch.buttons
        return buttons.element(boundBy: buttons.count - 1)
    }

    /// Types like a person: a key of the on-screen keyboard at a time, into the field with the focus. A
    /// `typeText` per character hides and shows the keyboard on every one; it is left for a key not found.
    private func typeSlowly(_ text: String) {
        let keyboard = app.keyboards.firstMatch
        for character in text {
            if keyboard.exists {
                typeKey(character, on: keyboard)
            } else {
                app.typeText(String(character))
            }
            look(Pause.key)
        }
    }

    /// A capital takes the shift first, a sign the layout of numbers and signs.
    private func typeKey(_ character: Character, on keyboard: XCUIElement) {
        let label = character == " " ? "space" : String(character)
        var key = keyboard.keys[label]
        if !key.exists && character.isUppercase {
            keyboard.buttons["shift"].tap()
            key = keyboard.keys[label]
        }
        if !key.exists && !character.isLetter {
            keyboard.keys["more"].tap()
            key = keyboard.keys[label]
        }
        if key.exists {
            key.tap()
        } else {
            app.typeText(String(character))
        }
    }

    // MARK: - Flicks

    /// A flick: the list goes on by itself after it and slows down. A flick down starts below the
    /// navigation bar and the search field: a touch there does not move the list.
    private func flick(up: Bool, velocity: CGFloat = Flick.velocity) {
        drag(
            from: CGVector(dx: 0.5, dy: up ? 0.85 : 0.3),
            to: CGVector(dx: 0.5, dy: up ? 0.15 : 0.95),
            velocity: velocity
        )
    }

    /// The "last visible / total" counter of the list: the number the viewer sees on the screen, before the
    /// slash of its label. The app shows it with a group separator ("1,414"), so only the digits are kept.
    private func position() -> Int? {
        guard let root = try? app.snapshot() else { return nil }
        var labels: [String] = []
        func visit(_ node: XCUIElementSnapshot) {
            if node.identifier == Id.position { labels.append(node.label) }
            node.children.forEach(visit)
        }
        visit(root)
        return labels
            .compactMap { label in label.split(separator: "/").first.flatMap { Int(String($0.filter(\.isNumber))) } }
            .max()
    }

    /// A run along the list: flicks one after another until the counter says `reached`. The counter is
    /// read between bursts: a read takes time and would slow the run down. An unreadable counter does not
    /// stop the tour, the cap does; a counter that stands still (the end of the list) stops it too.
    private func flickUntil(up: Bool, velocity: CGFloat = Flick.velocity, burst: Int = Flick.burst, reached: (Int) -> Bool) {
        var last: Int?
        for _ in 0..<(Flick.max / burst) {
            let now = position()
            if let now, reached(now) { return }
            if let now, now == last { return }
            last = now
            for _ in 0..<burst { flick(up: up, velocity: velocity) }
        }
    }

    /// Flicks back up until the element is on screen again.
    private func flickUntilShown(_ id: String, velocity: CGFloat) {
        for _ in 0..<Flick.max {
            if element(id).exists { return }
            flick(up: false, velocity: velocity)
        }
    }

    // MARK: - Touch

    private func drag(from: CGVector, to: CGVector, velocity: CGFloat) {
        app.coordinate(withNormalizedOffset: from).press(
            forDuration: Flick.hold,
            thenDragTo: app.coordinate(withNormalizedOffset: to),
            withVelocity: XCUIGestureVelocity(velocity),
            thenHoldForDuration: 0
        )
    }

    private func tap(at point: CGPoint) {
        app.coordinate(withNormalizedOffset: .zero).withOffset(CGVector(dx: point.x, dy: point.y)).tap()
    }

    // MARK: - Finding and waiting

    private func element(_ id: String) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: id).firstMatch
    }

    /// The frames of the elements with the id, from one snapshot of the screen: reading them one by one
    /// would ask the app for the whole tree each time.
    private func frames(_ id: String) -> [CGRect] {
        guard let root = try? app.snapshot() else { return [] }
        var found: [CGRect] = []
        func visit(_ node: XCUIElementSnapshot) {
            if node.identifier == id { found.append(node.frame) }
            node.children.forEach(visit)
        }
        visit(root)
        return found
    }

    private func count(_ id: String) -> Int { frames(id).count }

    /// Waits until the list stands still, no longer than it takes: a card is where it was a moment ago.
    private func settle() {
        var last: CGRect?
        waitFor {
            let now = frames(Id.imageCard).first
            defer { last = now }
            return now != nil && now == last
        }
    }

    private func waitFor(timeout: TimeInterval = 30, _ condition: () -> Bool) {
        let end = Date().addingTimeInterval(timeout)
        while !condition() {
            if Date() > end {
                XCTFail("Timed out after \(timeout) s")
                return
            }
            Thread.sleep(forTimeInterval: Pause.poll)
        }
    }

    /// The pause that gives the viewer time to see the screen.
    private func look(_ seconds: TimeInterval) {
        Thread.sleep(forTimeInterval: seconds)
    }
}

/// XCUITest waits for the app to be quiet before and after every gesture and at launch: while a list
/// glides or images load, that is seconds, and a flick never comes while the list still goes. There is
/// no public setting, so the private wait of `XCUIApplicationProcess` is made to return at once; the tour
/// waits itself. If a version of Xcode renames it, nothing is replaced and the tour is only slower.
private enum Quiescence {
    static func skip() {
        guard let process = NSClassFromString("XCUIApplicationProcess") else { return }
        let wait: @convention(block) (AnyObject, Bool) -> Void = { _, _ in }
        let waitPreEvent: @convention(block) (AnyObject, Bool, Bool) -> Void = { _, _, _ in }
        replace(process, "waitForQuiescenceIncludingAnimationsIdle:", with: wait)
        replace(process, "waitForQuiescenceIncludingAnimationsIdle:isPreEvent:", with: waitPreEvent)
    }

    private static func replace(_ type: AnyClass, _ selector: String, with block: Any) {
        guard let method = class_getInstanceMethod(type, NSSelectorFromString(selector)) else { return }
        method_setImplementation(method, imp_implementationWithBlock(block))
    }
}

/// The ids of `UiTestId` (ui-core).
private enum Id {
    static let imageCard = "image_card"
    static let heart = "heart"
    static let layoutToggle = "layout_toggle"
    static let searchOpen = "search_open"
    static let searchField = "search_field"
    static let searchDone = "search_done"
    static let newChat = "new_chat"
    static let messageField = "message_field"
    static let send = "send"
    static let position = "position"
}

/// The places of the tabs in the tab bar, as in `AppTab`.
private enum Tab {
    static let space = 0
    static let favorites = 1
    static let chat = 2
    static let all = 0..<5
}

private enum Text {
    static let search = "Titan"
    static let chatTitle = "Demo chat"
    static let chatInterlocutor = "Neil"
    static let message = "Hello from KPage!"
}

private enum Position {
    /// The counter value to flick down to, and the one to come back up to: almost the start.
    static let far = 1000
    static let nearStart = 120

    /// The run stops flicking this many items short of its goal: the list rolls on by itself.
    static let coast = 250

    /// The counter value to glide down to in the grid, and how far short of it the flicks stop.
    static let grid = 150
    static let gridCoast = 40
}

private enum Flick {
    /// The speed of the finger at the end of a gesture, points a second: a sharp flick of a run, the calm
    /// flicks of the grid and the search, a slow scroll, the removal swipe.
    static let velocity: CGFloat = 20000
    static let gridVelocity: CGFloat = 5000
    static let softVelocity: CGFloat = 3000
    static let scrollVelocity: CGFloat = 1200
    static let swipeVelocity: CGFloat = 1500

    /// The touch before the move.
    static let hold: TimeInterval = 0.01

    /// Flicks between the reads of the counter, and a cap for a run.
    static let burst = 5
    static let max = 150
}

private enum Pause {
    static let poll: TimeInterval = 0.05
    static let key: TimeInterval = 0.15
    static let settle: TimeInterval = 0.4
    static let short: TimeInterval = 0.6
    static let long: TimeInterval = 1.0
    static let heart: TimeInterval = 1.0
    static let transition: TimeInterval = 1.2
    static let layoutHold: TimeInterval = 2.0
    static let chatLoad: TimeInterval = 2.0
    static let glide: TimeInterval = 2.5
    static let end: TimeInterval = 4.0
}
