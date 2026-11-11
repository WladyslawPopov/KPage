import SwiftUI
import SharedLogic

/// A titled group of choices, one of them marked.
struct SettingsOptionsSection<T: AnyObject>: View {
    let title: String
    let options: [SettingsOption<T>]
    let onSelect: (T) -> Void

    var body: some View {
        Section(title) {
            // The choices never reorder, so the position is a stable identity.
            ForEach(options.indices, id: \.self) { index in
                let option = options[index]
                Button { onSelect(option.value) } label: {
                    HStack {
                        Text(option.label.string).foregroundStyle(.primary)
                        Spacer()
                        if option.selected {
                            Image(systemName: "checkmark").foregroundStyle(AppColorKeyIcon.accent.color)
                        }
                    }
                }
                .accessibilityAddTraits(option.selected ? .isSelected : [])
            }
        }
    }
}
