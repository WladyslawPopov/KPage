import SwiftUI
import SharedLogic

/// The bottom sheet of the Space filters. A choice is applied at once, so the list under the sheet
/// changes and the button shows how many images the filters leave. Whether the sheet is open is the
/// state of the screen.
struct SpaceFiltersSheet: View {
    let state: SpaceState
    let onIntent: (SpaceIntent) -> Void

    @Environment(\.dismiss) private var dismiss

    private var query: SpaceQuery { state.list.query }
    private var options: SpaceFilterOptions { SpaceFilterOptions.shared }
    private var filters: SpaceFiltersDisplay { state.filters }

    var body: some View {
        NavigationStack {
            Form {
                // Set from outside (a field of an image): free text, so it can only be removed, not chosen.
                if let tag = query.tag {
                    HStack {
                        Text(options.title(kind: tag.kind).text)
                        Spacer()
                        Text(tag.value)
                            .foregroundStyle(AppColorKeyText.secondary.color)
                            .lineLimit(1)
                        Button {
                            setFilters(query.center, query.yearStart?.intValue, query.yearEnd?.intValue, nil)
                        } label: {
                            Image(systemName: AppIconKey.close.sfSymbol)
                                .foregroundStyle(AppColorKeyIcon.secondary.color)
                        }
                        // In a Form a button row reacts to a tap anywhere in it; plain makes only the cross a button.
                        .buttonStyle(.plain)
                        .accessibilityLabel(AppStringKeyCore.clear.text)
                    }
                }
                Picker(AppStringKeySpace.center.text, selection: center) {
                    Text(AppStringKeySpace.any.text).tag(SpaceCenter?.none)
                    ForEach(options.centers, id: \.self) { item in
                        Text(options.title(center: item).text).tag(SpaceCenter?.some(item))
                    }
                }
                Picker(AppStringKeySpace.from.text, selection: yearStart) { yearChoices(filters.fromYears) }
                Picker(AppStringKeySpace.to.text, selection: yearEnd) { yearChoices(filters.toYears) }
            }
            .pickerStyle(.menu)
            .navigationTitle(AppStringKeyCore.filters.text)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(AppStringKeyCore.reset.text) { setFilters(nil, nil, nil, nil) }
                        .disabled(!state.isFiltered)
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button { dismiss() } label: {
                    Text(showLabel).frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .padding()
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    /// "Any" and the years the state offers (the latest first).
    @ViewBuilder private func yearChoices(_ years: [KotlinInt]) -> some View {
        Text(AppStringKeySpace.any.text).tag(Int?.none)
        ForEach(years, id: \.self) { year in
            Text(String(year.intValue)).tag(Int?.some(year.intValue))
        }
    }

    /// "Show", and how many images the filters leave once the list has loaded.
    private var showLabel: String {
        let show = AppStringKeySpace.show.text
        guard let count = filters.resultCount else { return show }
        return "\(show) \(count.intValue.formatted())"
    }

    private var center: Binding<SpaceCenter?> {
        Binding(get: { query.center }, set: { setFilters($0, query.yearStart?.intValue, query.yearEnd?.intValue, query.tag) })
    }

    private var yearStart: Binding<Int?> {
        Binding(get: { query.yearStart?.intValue }, set: { setFilters(query.center, $0, query.yearEnd?.intValue, query.tag) })
    }

    private var yearEnd: Binding<Int?> {
        Binding(get: { query.yearEnd?.intValue }, set: { setFilters(query.center, query.yearStart?.intValue, $0, query.tag) })
    }

    private func setFilters(_ center: SpaceCenter?, _ from: Int?, _ to: Int?, _ tag: SpaceTag?) {
        onIntent(SpaceIntentSetFilters(
            center: center,
            yearStart: from.map { KotlinInt(int: Int32($0)) },
            yearEnd: to.map { KotlinInt(int: Int32($0)) },
            tag: tag
        ))
    }
}
