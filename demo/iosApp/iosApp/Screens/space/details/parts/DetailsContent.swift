import SwiftUI
import SharedLogic

/// Under the picture: the title, the description and the cards, exactly as `display` lists them.
struct DetailsContent: View {
    let display: SpaceDetailsDisplay
    let onIntent: (SpaceDetailsIntent) -> Void
    let onCopied: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: AppDimenKeyPadding.large.value) {
            CopyableTitle(title: display.title, onCopied: onCopied)
            if let description = display.description_ {
                // A long press selects words: the text can be copied in parts.
                SelectableText(text: description)
            }
            // The sections are two kinds of one Kotlin interface: Swift sees them as separate classes.
            ForEach(Array(display.sections.enumerated()), id: \.offset) { _, section in
                DetailsCard(title: section.title) {
                    if let fields = section as? DetailsSectionFields {
                        DetailsFieldRows(rows: fields.rows, onIntent: onIntent)
                    } else if let chips = section as? DetailsSectionChips {
                        DetailsChips(chips: chips.chips, onIntent: onIntent)
                    }
                }
            }
        }
        .padding(AppDimenKeyPadding.large.value)
    }
}
