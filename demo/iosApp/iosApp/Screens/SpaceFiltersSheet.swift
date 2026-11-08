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

    var body: some View {
        NavigationStack {
            Form {
                Picker(AppStringKeySpace.center.text, selection: center) {
                    Text(AppStringKeySpace.any.text).tag(SpaceCenter?.none)
                    ForEach(options.centers, id: \.self) { item in
                        Text(options.title(center: item).text).tag(SpaceCenter?.some(item))
                    }
                }
                // The range cannot end before it starts: each picker offers only the years that keep it valid.
                Picker(AppStringKeySpace.from.text, selection: yearStart) {
                    yearChoices(from: Int(options.firstYear), to: query.yearEnd?.intValue ?? Int(options.lastYear))
                }
                Picker(AppStringKeySpace.to.text, selection: yearEnd) {
                    yearChoices(from: query.yearStart?.intValue ?? Int(options.firstYear), to: Int(options.lastYear))
                }
            }
            .pickerStyle(.menu)
            .navigationTitle(AppStringKeyCore.filters.text)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(AppStringKeyCore.reset.text) { setFilters(nil, nil, nil) }
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

    /// "Any" and the years, the latest first.
    @ViewBuilder private func yearChoices(from first: Int, to last: Int) -> some View {
        Text(AppStringKeySpace.any.text).tag(Int?.none)
        ForEach(Array((first...max(first, last)).reversed()), id: \.self) { year in
            Text(String(year)).tag(Int?.some(year))
        }
    }

    /// "Show", and how many images the filters leave once the list has loaded.
    private var showLabel: String {
        let show = AppStringKeySpace.show.text
        if state.page.isLoading { return show }
        return "\(show) \(Int(state.list.totalCount).formatted())"
    }

    private var center: Binding<SpaceCenter?> {
        Binding(get: { query.center }, set: { setFilters($0, query.yearStart?.intValue, query.yearEnd?.intValue) })
    }

    private var yearStart: Binding<Int?> {
        Binding(get: { query.yearStart?.intValue }, set: { setFilters(query.center, $0, query.yearEnd?.intValue) })
    }

    private var yearEnd: Binding<Int?> {
        Binding(get: { query.yearEnd?.intValue }, set: { setFilters(query.center, query.yearStart?.intValue, $0) })
    }

    private func setFilters(_ center: SpaceCenter?, _ from: Int?, _ to: Int?) {
        onIntent(SpaceIntentSetFilters(
            center: center,
            yearStart: from.map { KotlinInt(int: Int32($0)) },
            yearEnd: to.map { KotlinInt(int: Int32($0)) }
        ))
    }
}
