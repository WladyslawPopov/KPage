import SwiftUI
import SharedLogic

struct ChatsScreen: View {
    let component: ChatsComponent
    let state: ChatsState

    // The fields of a new chat belong to the screen; only the result goes to the component.
    @State private var newTitle = ""
    @State private var newInterlocutor = ""

    var body: some View {
        PageLayout(page: state.page, onPageIntent: { component.onPageIntent(intent: $0) }) {
            PagedGrid(
                list: state.list,
                spacing: AppDimenKeyPadding.none.value,
                placeholder: { _ in ChatRow(chat: nil) }
            ) { chat, _ in
                row(chat)
            }
        }
        .navigationTitle(state.isSelecting ? state.selectionTitle.string : state.topBar.title.text)
        .toolbar {
            ToolbarItemGroup(placement: .topBarLeading) {
                if state.isSelecting {
                    Button { component.onIntent(intent: ChatsIntentClearSelection.shared) } label: {
                        Image(systemName: AppIconKey.close.sfSymbol)
                    }
                    .accessibilityLabel(AppStringKeyCore.close.text)
                }
            }
            ToolbarItemGroup(placement: .topBarTrailing) {
                if state.isSelecting {
                    Button { component.onIntent(intent: ChatsIntentDeleteSelected.shared) } label: {
                        Image(systemName: AppIconKey.trash.sfSymbol)
                    }
                    .accessibilityLabel(AppStringKeyChat.delete_.text)
                } else {
                    Button { component.onIntent(intent: ChatsIntentStartCreate.shared) } label: {
                        Image(systemName: AppIconKey.add.sfSymbol)
                    }
                    .accessibilityLabel(AppStringKeyChat.create.text)
                }
            }
        }
        // The state decides when they are shown; the buttons answer through it, so the bindings only read.
        .alert(AppStringKeyChat.deleteTitle.text, isPresented: .constant(!state.pendingDelete.isEmpty)) {
            Button(AppStringKeyChat.delete_.text, role: .destructive) {
                component.onIntent(intent: ChatsIntentConfirmDelete.shared)
            }
            Button(AppStringKeyCore.cancel.text, role: .cancel) {
                component.onIntent(intent: ChatsIntentCancelDelete.shared)
            }
        } message: {
            Text(AppStringKeyChat.deleteMessage.text)
        }
        .alert(AppStringKeyChat.createTitle.text, isPresented: .constant(state.isCreating)) {
            TextField(AppStringKeyChat.titleHint.text, text: $newTitle)
            TextField(AppStringKeyChat.interlocutorHint.text, text: $newInterlocutor)
            Button(AppStringKeyChat.create.text) {
                component.onIntent(intent: ChatsIntentCreate(title: newTitle, interlocutor: newInterlocutor))
                newTitle = ""
                newInterlocutor = ""
            }
            .disabled(newTitle.trimmingCharacters(in: .whitespaces).isEmpty
                || newInterlocutor.trimmingCharacters(in: .whitespaces).isEmpty)
            Button(AppStringKeyCore.cancel.text, role: .cancel) {
                component.onIntent(intent: ChatsIntentCancelCreate.shared)
            }
        }
    }

    @ViewBuilder private func row(_ chat: Chat) -> some View {
        let isSelected = state.selected.contains(KotlinLong(value: chat.id))
        SwipeToRemove(
            isEnabled: !state.isSelecting,
            onRemove: { component.onIntent(intent: ChatsIntentDeleteOne(id: chat.id)) }
        ) {
            ChatRow(chat: chat) {
                if state.isSelecting {
                    Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                        .font(.title2)
                        .foregroundStyle(isSelected ? AppColorKeyIcon.accent.color : AppColorKeyIcon.secondary.color)
                }
            }
            .contentShape(Rectangle())
            .onTapGesture { component.onIntent(intent: ChatsIntentClick(id: chat.id)) }
            .onLongPressGesture {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                component.onIntent(intent: ChatsIntentLongClick(id: chat.id))
            }
        }
    }
}
