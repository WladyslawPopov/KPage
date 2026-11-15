import XCTest

/// The demo tour on iOS, the story of `DemoTour` of the `tour` module on Android: Space (the grid, a
/// favorite, back to the list; a search by a word, a favorite in it) -> far down the found images, a
/// favorite -> its details (take the heart off, back) -> Settings (a dark theme, Polish) -> back up, more
/// favorites -> Settings (English again) -> Favorites, the details of one -> the list of one of its tags
/// (the filters: the tag and a year, favorites there) -> back to the details (the heart off) -> Favorites
/// (the changes are there; remove one by a swipe) -> a chat room (create it, look through the history,
/// send a message) -> Settings (clear the cache) -> the lists again -> Settings (the system theme).
///
/// XCUITest runs out of the app's process: the app lives on the real clock and the gestures come from
/// the system, so the flicks glide and slow down as a person's do. Elements are found by the ids of
/// `UiTestId` (ui-core), set by `uiTestId` / `uiTestRow` in the app; the test does not link the shared
/// framework, so the ids are written here as strings. The tabs, the back button and the buttons of the
/// alerts have no ids: they are found by their place. The tour pauses only where the viewer needs to see a
/// result: a heart set or taken off, the details or a chat room just opened or left, the filters or the
/// settings just applied, a tap that changes the screen; everything else goes on at once, waiting only for
/// what it needs.
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
    let ramp = FlickRamp()

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

        // The grid: the same images in two columns, a calm glide down them, a favorite. The layout switch
        // is shown only near the top (another layout starts the list from its first row), so back up first.
        element(Id.layoutToggle).tap()
        look(Pause.change)
        flickUntil(
            up: true,
            to: Position.grid - Position.gridCoast,
            velocity: Flick.gridVelocity,
            burst: 1,
            slowZone: Position.gridSlowZone
        )
        settle()
        tapHeart(of: cardInMiddle())
        look(Pause.heart)
        flickUntilShown(Id.layoutToggle, velocity: Flick.gridVelocity)
        settle()
        element(Id.layoutToggle).tap()
        look(Pause.change)

        // Search: a word narrows the list; the check mark keeps it, and the tour goes on in it.
        element(Id.searchOpen).tap()
        waitFor { count(Id.searchField) > 0 }
        typeSlowly(Text.search)
        element(Id.searchDone).tap()
        waitFor { count(Id.imageCard) > 0 }
        look(Pause.change)
        for _ in 0..<2 { flick(up: true, velocity: Flick.softVelocity) }
        settle()
        tapHeart(of: cardInMiddle())
        look(Pause.heart)

        // Far down the found images, by the position counter, to show the paginator on a long list.
        flickUntil(up: true, to: Position.far - Position.coast)
        settle()
        let card = cardInMiddle()
        tapHeart(of: card)
        look(Pause.heart)

        // Its details: the heart is set there too; take it off and come back, the list shows it off.
        tap(at: CGPoint(x: card.midX, y: card.midY))
        waitFor { count(Id.imageCard) == 0 && count(Id.heart) > 0 }
        look(Pause.details)
        element(Id.heart).tap()
        look(Pause.heart)
        back()
        waitFor { count(Id.imageCard) > 0 }
        look(Pause.change)

        // Settings in the middle of the way: a dark theme and Polish, applied at once; the list keeps its place.
        openTab(Tab.settings)
        tapShown(Id.option("dark"))
        look(Pause.settings)
        tapShown(Id.option("polish"))
        look(Pause.settings)
        openTab(Tab.space)
        waitFor { count(Id.imageCard) > 0 }

        // Back up, almost to the start of the list: a few more favorites there.
        flickUntil(up: false, to: Position.nearStart + Position.coast)
        settle()
        for _ in 0..<2 {
            for _ in 0..<Count.nearStartScrolls { scroll() }
            tapHeart(of: cardInMiddle())
            look(Pause.heart)
        }

        // English again: the rest of the tour is read, not only seen.
        openTab(Tab.settings)
        tapShown(Id.option("english"))
        look(Pause.settings)

        // Favorites: they are there; the details of one.
        openTab(Tab.favorites)
        waitFor { count(Id.imageCard) > 0 }
        let favorite = frames(Id.imageCard)[0]
        tap(at: CGPoint(x: favorite.midX, y: favorite.midY))
        waitFor { count(Id.imageCard) == 0 && count(Id.heart) > 0 }
        look(Pause.details)

        // One of its tags opens a new list filtered by it.
        let tag = tagInDetails()
        tap(at: CGPoint(x: tag.midX, y: tag.midY))
        waitFor { count(Id.imageCard) > 0 }
        look(Pause.change)

        // The filters show the tag; a year narrows the list.
        tapShown(Id.filtersOpen)
        waitFor { count(Id.filterYearFrom) > 0 }
        look(Pause.change)
        tapSteady(Id.filterYearFrom)
        pickFilterYear()
        look(Pause.change)
        tapSteady(Id.filtersShow)
        // The sheet is gone and the list under it shows the result.
        waitFor { count(Id.filtersShow) == 0 && count(Id.imageCard) > 0 }
        look(Pause.filters)

        // Down the list, a heart on an image that is not a favorite yet after every scroll.
        for _ in 0..<Count.tagFavorites {
            scroll()
            if let heart = heartsOff().first {
                tap(at: CGPoint(x: heart.midX, y: heart.midY))
                look(Pause.heart)
            }
        }

        // Back to the details, scrolled down to the tag: up to the picture, the heart off.
        back()
        waitFor { count(Id.imageCard) == 0 }
        detailsToTop()
        takeHeartOff()
        look(Pause.heart)

        // Favorites: the image is gone, the new ones are there; remove one with a swipe.
        back()
        waitFor { count(Id.imageCard) > 0 }
        settle()
        look(Pause.change)
        swipeLeft(over: frames(Id.imageCard)[0])
        confirmAlert()
        look(Pause.heart)

        // Chat: create a room (the app opens it), look through its history, send a message.
        openTab(Tab.chat)
        element(Id.newChat).tap()
        waitFor { app.alerts.textFields.count >= 2 }
        typeSlowly(Text.chatTitle, into: app.alerts.textFields.element(boundBy: 0))
        typeSlowly(Text.chatInterlocutor, into: app.alerts.textFields.element(boundBy: 1))
        confirmAlert()
        waitFor { count(Id.messageField) > 0 }
        // The room opens, then its history loads: time to see it before looking through it.
        look(Pause.chat)
        for _ in 0..<2 { flick(up: false, velocity: Flick.softVelocity) }
        element(Id.messageField).tap()
        typeSlowly(Text.message)
        waitFor { element(Id.send).isEnabled }
        element(Id.send).tap()
        // The message is in the list: the field is not a static text. A look before leaving.
        waitFor { app.staticTexts[Text.message].exists }
        look(Pause.chat)
        // Back to the list of chats, the new one on top.
        back()
        waitFor { count(Id.chatRow) > 0 }
        look(Pause.chatList)

        // Settings: clear the cache; the lists load again, the favorites and the chats stay.
        openTab(Tab.settings)
        tapShown(Id.clearCache)
        confirmAlert()
        look(Pause.settings)
        openTab(Tab.space)
        waitFor { count(Id.imageCard) > 0 }
        openTab(Tab.favorites)
        waitFor { count(Id.imageCard) > 0 }
        openTab(Tab.chat)
        waitFor { count(Id.chatRow) > 0 }

        // The end: the system theme again, as the next run expects.
        openTab(Tab.settings)
        tapShown(Id.option("system"))
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
        look(Pause.tabSwitch)
    }

    /// Taps the element once it shows, with one look at the screen: for what does not move (a row of
    /// Settings, a button of a bar). Each look at the screen costs XCUITest a snapshot of the whole app.
    private func tapShown(_ id: String) {
        let target = element(id)
        // `waitForExistence` spends a whole second before its first look: only for what is not there yet.
        if !target.exists && !target.waitForExistence(timeout: 10) { XCTFail("No \(id)") }
        target.tap()
    }

    /// Taps the element once it shows and stands still: a sheet or a screen still moving would take the tap
    /// elsewhere.
    private func tapSteady(_ id: String) {
        var last: CGRect?
        waitFor(timeout: 10) {
            let now = frames(id).first
            defer { last = now }
            return now != nil && now == last
        }
        element(id).tap()
    }

    /// The hearts on the screen with whether each is set: the app marks a set heart as selected.
    private func hearts() -> [(frame: CGRect, isSet: Bool)] {
        guard let root = try? app.snapshot() else { return [] }
        var found: [(frame: CGRect, isSet: Bool)] = []
        func visit(_ node: XCUIElementSnapshot) {
            if node.identifier == Id.heart { found.append((node.frame, node.isSelected)) }
            node.children.forEach(visit)
        }
        visit(root)
        return found
    }

    /// The hearts that are not set, clear of the bars: a new favorite, not one taken off.
    private func heartsOff() -> [CGRect] {
        let height = app.frame.height
        let band = (height * 0.2)...(height * 0.85)
        return hearts().filter { !$0.isSet && band.contains($0.frame.midY) }.map(\.frame)
    }

    /// Scrolls the details down to a field or a chip that opens a list by a tag. After each scroll it waits
    /// until the details stand still: a tap while they still glide only stops them and opens nothing.
    private func tagInDetails() -> CGRect {
        let height = app.frame.height
        let band = (height * 0.2)...(height * 0.85)
        for _ in 0..<Count.detailsScrolls {
            if let tag = frames(Id.detailsTag).first(where: { band.contains($0.midY) }) { return tag }
            drag(from: CGVector(dx: 0.5, dy: 0.7), to: CGVector(dx: 0.5, dy: 0.35), velocity: Flick.scrollVelocity)
            var last: CGRect?
            var isFirst = true
            waitFor {
                let now = frames(Id.detailsTag).first ?? frames(Id.heart).first
                defer {
                    last = now
                    isFirst = false
                }
                return !isFirst && now == last
            }
        }
        XCTFail("No tag in the details")
        return .zero
    }

    /// Flicks the details up to the picture with the heart, each time waiting only until they stand still: by
    /// the heart, or by a tag while the picture is above the screen. It stops as soon as the heart is clear
    /// of the top bar, which would take the tap: no swipe at the very top, which would pull to refresh.
    private func detailsToTop() {
        let height = app.frame.height
        for _ in 0..<Count.detailsScrolls {
            if let heart = frames(Id.heart).first, heart.minY > height * Position.heartClearTop, heart.maxY < height {
                return
            }
            flick(up: false)
            var last: CGRect?
            var isFirst = true
            waitFor {
                let now = frames(Id.heart).first ?? frames(Id.detailsTag).first
                defer {
                    last = now
                    isFirst = false
                }
                return !isFirst && now == last
            }
        }
        XCTFail("No picture at the top of the details")
    }

    /// Taps the set heart of the details until it is off: a tap the screen did not take is repeated.
    private func takeHeartOff() {
        for _ in 0..<Count.heartTaps {
            guard let heart = hearts().first else {
                XCTFail("No heart in the details")
                return
            }
            if !heart.isSet { return }
            tap(at: CGPoint(x: heart.frame.midX, y: heart.frame.midY))
            let end = Date().addingTimeInterval(Pause.heartWait)
            while Date() < end {
                if hearts().first?.isSet == false { return }
                Thread.sleep(forTimeInterval: Pause.poll)
            }
        }
        XCTFail("The heart of the details stays set")
    }

    /// Picks `Text.filterYear` in the open menu of years, newest first. It is usually shown at once; when it
    /// is not, slow drags up the menu, held at the end so the menu does not fling and no row is chosen.
    private func pickFilterYear() {
        let target = app.buttons[Text.filterYear]
        waitFor { yearFrames().count >= 2 }
        for _ in 0..<Count.menuDrags {
            if target.exists && target.isHittable {
                target.tap()
                return
            }
            let years = yearFrames()
            guard let top = years.first, let bottom = years.last, years.count >= 2 else { continue }
            let origin = app.coordinate(withNormalizedOffset: .zero)
            origin.withOffset(CGVector(dx: bottom.midX, dy: bottom.midY)).press(
                forDuration: Flick.hold,
                thenDragTo: origin.withOffset(CGVector(dx: top.midX, dy: top.midY)),
                withVelocity: XCUIGestureVelocity(Flick.scrollVelocity),
                thenHoldForDuration: Pause.settle
            )
        }
        XCTFail("No \(Text.filterYear) in the menu")
    }

    /// The frames of the years of the open menu, from the top: buttons with four digits.
    private func yearFrames() -> [CGRect] {
        guard let root = try? app.snapshot() else { return [] }
        var found: [CGRect] = []
        func visit(_ node: XCUIElementSnapshot) {
            if node.elementType == .button, node.label.count == 4, node.label.allSatisfy(\.isNumber) {
                found.append(node.frame)
            }
            node.children.forEach(visit)
        }
        visit(root)
        return found.sorted { $0.minY < $1.minY }
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

    /// Taps the action of the alert once it stands still, and again while the alert stays: a tap while it
    /// still slides in is lost, and XCUITest would later dismiss the alert left behind by its cancel button.
    private func confirmAlert() {
        waitFor { app.alerts.count > 0 }
        for _ in 0..<Count.alertTaps {
            var last: CGRect?
            waitFor(timeout: 10) {
                let now = alertAction().exists ? alertAction().frame : nil
                defer { last = now }
                return now != nil && now == last
            }
            alertAction().tap()
            let end = Date().addingTimeInterval(Pause.alertWait)
            while Date() < end {
                if app.alerts.count == 0 { return }
                Thread.sleep(forTimeInterval: Pause.poll)
            }
        }
        XCTFail("The alert stays open")
    }

    /// Types like a person: a key of the on-screen keyboard at a time, into the field with the focus. A
    /// `typeText` per character hides and shows the keyboard on every one; it is left for a key not found.
    private func typeSlowly(_ text: String) {
        let keyboard = app.keyboards.firstMatch
        let isShown = waitForKeyboard(keyboard)
        for character in text {
            if isShown {
                typeKey(character, on: keyboard)
            } else {
                app.typeText(String(character))
            }
            look(Pause.key)
        }
    }

    /// Waits until the keyboard has slid in and stands within the screen: a key tapped while it still moves
    /// lies below the screen. False when it does not come in time: with a hardware keyboard connected to a
    /// simulator, the on-screen one stays below the screen, and the typing falls back to `typeText`.
    private func waitForKeyboard(_ keyboard: XCUIElement) -> Bool {
        let end = Date().addingTimeInterval(Pause.keyboardWait)
        var last: CGRect?
        while Date() < end {
            let now = keyboard.exists ? keyboard.frame : nil
            if let now, now == last, now.maxY <= app.frame.maxY + 1 { return true }
            last = now
            Thread.sleep(forTimeInterval: Pause.poll)
        }
        return false
    }

    /// Types a character at a time into a field of an alert. Not by the keys: the keyboard lies outside the
    /// alert, so XCUITest takes the alert for an interruption of a key's tap and dismisses it.
    private func typeSlowly(_ text: String, into field: XCUIElement) {
        field.tap()
        for character in text {
            field.typeText(String(character))
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
    /// navigation bar and the search field: a touch there does not move the list. Its speed comes from the
    /// ramp: a series of flicks starts slow and gathers speed up to `velocity`.
    private func flick(up: Bool, velocity: CGFloat = Flick.velocity) {
        drag(
            from: CGVector(dx: 0.5, dy: up ? 0.85 : 0.3),
            to: CGVector(dx: 0.5, dy: up ? 0.15 : 0.95),
            velocity: ramp.next(up: up, top: velocity)
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

    /// A run along the list: flicks one after another until the counter passes `goal`. The counter is read
    /// between bursts: a read takes time and would slow the run down. An unreadable counter does not stop the
    /// tour, the cap does; a counter that stands still (the end of the list) stops it too. The run gathers
    /// speed from the start (the ramp of `flick`) and slows down within `slowZone` rows of the goal, reading
    /// the counter more often there, so it comes to the goal gently.
    private func flickUntil(
        up: Bool,
        to goal: Int,
        velocity: CGFloat = Flick.velocity,
        burst: Int = Flick.burst,
        slowZone: Int = Position.slowZone
    ) {
        var last: Int?
        var flicks = 0
        while flicks < Flick.max {
            let now = position()
            if let now, up ? now >= goal : now <= goal { return }
            if let now, now == last { return }
            last = now
            let left = now.map { abs(goal - $0) } ?? slowZone
            let isNear = left < slowZone
            let top = isNear ? max(Flick.startVelocity, velocity * CGFloat(left) / CGFloat(slowZone)) : velocity
            for _ in 0..<(isNear ? min(burst, Flick.nearBurst) : burst) {
                flick(up: up, velocity: top)
                flicks += 1
            }
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
        ramp.reset()
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

    /// The pause that gives the viewer time to see the screen. A pause of half a second or more is an activity
    /// of the test log, "hold" with its length, so the edit of the video can tell it from the waits of
    /// XCUITest; the short ones between keys and steps are not marked.
    private func look(_ seconds: TimeInterval) {
        ramp.reset()
        guard seconds >= 0.5 else {
            Thread.sleep(forTimeInterval: seconds)
            return
        }
        XCTContext.runActivity(named: "hold \(seconds)") { _ in
            Thread.sleep(forTimeInterval: seconds)
        }
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

/// The speed of the flicks: a series starts slow and grows a flick at a time, so the first rows have time
/// for their animations and the picture does not tear. A series goes on while the flicks keep their
/// direction and follow soon one after another; a pause, a tap or a turn starts a new one.
@MainActor
private final class FlickRamp {
    private var speed: CGFloat = 0
    private var isUp: Bool?
    private var lastTime = Date.distantPast

    /// The speed of the next flick, a step nearer to `top` from the last one: up while gathering speed,
    /// down while slowing to the end of a run.
    func next(up: Bool, top: CGFloat) -> CGFloat {
        let goesOn = isUp == up && Date().timeIntervalSince(lastTime) < Flick.seriesGap
        if !goesOn {
            speed = min(Flick.startVelocity, top)
        } else if speed < top {
            speed = min(speed * Flick.speedUp, top)
        } else {
            speed = max(speed / Flick.speedUp, top)
        }
        isUp = up
        lastTime = Date()
        return speed
    }

    func reset() {
        isUp = nil
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
    static let chatRow = "chat_row"
    static let filtersOpen = "filters_open"
    static let filterYearFrom = "filter_year_from"
    static let filtersShow = "filters_show"
    static let detailsTag = "details_tag"
    static let clearCache = "clear_cache"

    /// A choice of Settings, by the name of its value, as `UiTestId.option`.
    static func option(_ name: String) -> String { "option_\(name)" }
}

/// The places of the tabs in the tab bar, as in `AppTab`.
private enum Tab {
    static let space = 0
    static let favorites = 1
    static let chat = 2
    static let settings = 4
    static let all = 0..<5
}

private enum Text {
    static let search = "Titan"
    static let chatTitle = "Demo chat"
    static let chatInterlocutor = "Neil"
    static let message = "Hello from KPage!"

    /// The year the filters start from, a few rows down the menu of years (newest first).
    static let filterYear = "2018"
}

private enum Count {
    /// Scrolls before each heart near the start of the list. The last heart is the first favorite, whose
    /// details the tour opens for a tag: a few scrolls move it off an image that has none.
    static let nearStartScrolls = 4

    /// Scrolls down the list of a tag, each with a heart on a new image.
    static let tagFavorites = 2

    /// Caps for the scrolls of the details, the taps on their heart and the drags of the menu of years.
    static let detailsScrolls = 12
    static let heartTaps = 3
    static let menuDrags = 10

    /// A cap for the taps on the action of an alert.
    static let alertTaps = 3
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

    /// How many rows short of its goal a run starts to slow down: a long run and the glide of the grid.
    static let slowZone = 400
    static let gridSlowZone = 40

    /// The share of the screen the heart of the details must be below: the top bar lies over the picture.
    static let heartClearTop: CGFloat = 0.12
}

private enum Flick {
    /// The speed of the finger at the end of a gesture, points a second: a sharp flick of a run, the calm
    /// flicks of the grid and the search, a slow scroll, the removal swipe.
    static let velocity: CGFloat = 8000

    /// A run along the list begins with this speed and grows by `speedUp` a flick up to its own.
    static let startVelocity: CGFloat = 2000
    static let speedUp: CGFloat = 1.4

    /// The longest time between two flicks of one series; XCUITest itself takes about two between them.
    static let seriesGap: TimeInterval = 3.0

    /// Flicks between the reads of the counter near the goal of a run, where it slows down.
    static let nearBurst = 2
    static let gridVelocity: CGFloat = 5000
    static let softVelocity: CGFloat = 3000
    static let scrollVelocity: CGFloat = 1200
    static let swipeVelocity: CGFloat = 1500

    /// The touch before the move.
    static let hold: TimeInterval = 0.01

    /// Flicks between the reads of the counter, and a cap for a run. A read is a snapshot of the whole app.
    static let burst = 8
    static let max = 150
}

private enum Pause {
    static let poll: TimeInterval = 0.05
    static let key: TimeInterval = 0.15
    static let settle: TimeInterval = 0.4

    /// The warm-up, off the record.
    static let short: TimeInterval = 0.6
    static let long: TimeInterval = 1.0

    /// The looks at a result: a heart set or taken off, the details just opened, the filters or the settings
    /// just applied, a chat room just opened or left, a tap that changes the screen, the last screen.
    static let heart: TimeInterval = 1.5
    static let change: TimeInterval = 1.5
    static let details: TimeInterval = 1.5
    static let filters: TimeInterval = 1.5
    static let settings: TimeInterval = 1.5
    static let chat: TimeInterval = 2.5
    static let chatList: TimeInterval = 1.5
    static let end: TimeInterval = 4.0

    /// After a tab is selected: the screens change places before the next step.
    static let tabSwitch: TimeInterval = 1.0

    /// How long a tap on the heart of the details waits for it to go off.
    static let heartWait: TimeInterval = 2.0

    /// How long the typing waits for the keyboard to slide in.
    static let keyboardWait: TimeInterval = 1.0

    /// How long a tap on the action of an alert waits for the alert to close.
    static let alertWait: TimeInterval = 2.0
}
