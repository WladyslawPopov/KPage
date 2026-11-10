import SwiftUI
import SharedLogic

/// A chat: the reversed list of messages (row 0 is at the bottom) and the field under it, without the
/// tab bar. When the bottom row changes (a new message, mine or the interlocutor's), the list goes back
/// to the bottom; the first show already starts there. A tap on the messages hides the keyboard, a tap
/// on a photo opens it full screen.
struct ChatRoomScreen: View {
    let component: ChatRoomComponent
    let state: ChatRoomState

    /// The photo open full screen; shown only by this screen, so not a part of the state.
    @State private var viewerURL: String?

    var body: some View {
        PageLayout(page: state.page, onPageIntent: { component.onPageIntent(intent: $0) }) {
            VStack(spacing: 0) {
                // Also keeps the flipped list off the top edge, so the scroll view does not take the
                // inset of the navigation bar, which a flipped list would show at its bottom.
                if let note = state.scenarioNote {
                    scenarioNote(note)
                } else {
                    Divider()
                }
                PagedGrid(
                    list: state.list,
                    reversed: true,
                    showPosition: false,
                    spacing: AppDimenKeyPadding.none.value,
                    startTrigger: state.newestKey,
                    decoration: { (decoration: ChatDecoration) in
                        if let local = decoration as? ChatDecoration.Local {
                            bubble(local.item)
                        } else if let separator = decoration as? ChatDecoration.DateSeparator {
                            DateSeparatorView(date: separator.date)
                        }
                    },
                    placeholder: { MessagePlaceholder(serverIndex: $0) }
                ) { message, _ in
                    bubble(component.messageItem(message: message))
                }
                // Together with the taps of the rows: a photo still opens, a long press still shows the menu.
                .simultaneousGesture(TapGesture().onEnded { hideKeyboard() })
                Composer(state: state) { component.onIntent(intent: $0) }
            }
        }
        .navigationTitle(state.title)
        .navigationBarTitleDisplayMode(.inline)
        // The bar keeps its background: over a flipped list the system cannot tell whether the content
        // is scrolled under it.
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .toolbar {
            ToolbarItem(placement: .principal) {
                // The bar does not redraw an item whose input changed, so new data makes a new item.
                ChatRoomTitle(avatar: state.avatar, title: state.title, interlocutor: state.interlocutor)
                    .id("\(state.title)|\(state.interlocutor)|\(state.avatar?.letter ?? "")")
            }
            ToolbarItem(placement: .topBarTrailing) {
                if !state.actions.isEmpty { actionsMenu }
            }
        }
        // The state decides when it is shown; the buttons answer through it, so the binding only reads.
        .alert(AppStringKeyChat.deleteChatTitle.text, isPresented: .constant(state.isDeleteAsked)) {
            Button(AppStringKeyChat.delete_.text, role: .destructive) {
                component.onIntent(intent: ChatRoomIntentConfirmDeleteChat.shared)
            }
            Button(AppStringKeyCore.cancel.text, role: .cancel) {
                component.onIntent(intent: ChatRoomIntentCancelDeleteChat.shared)
            }
        } message: {
            Text(AppStringKeyChat.deleteChatMessage.text)
        }
        .fullScreenCover(isPresented: Binding(get: { viewerURL != nil }, set: { if !$0 { viewerURL = nil } })) {
            if let viewerURL {
                SpaceImageViewer(url: viewerURL) { self.viewerURL = nil }
            }
        }
    }

    private func bubble(_ item: ChatMessageItem) -> some View {
        MessageBubble(
            item: item,
            onIntent: { component.onIntent(intent: $0) },
            onOpenPhoto: { viewerURL = ChatPhotos.url(of: $0).absoluteString }
        )
    }

    /// The menu of the chat; its entries come ready from the shared code.
    private var actionsMenu: some View {
        Menu {
            ForEach(Array(state.actions.enumerated()), id: \.offset) { _, action in
                Button { component.onIntent(intent: action.intent) } label: {
                    Label(action.title.text, systemImage: action.icon.sfSymbol)
                }
            }
        } label: {
            Image(systemName: AppIconKey.more.sfSymbol)
        }
        .accessibilityLabel(AppStringKeyChat.actions.text)
    }

    /// How the mock behind this chat behaves, under the bar.
    private func scenarioNote(_ note: AppStringKey) -> some View {
        Label(note.text, systemImage: AppIconKey.info.sfSymbol)
            .font(.caption2)
            .lineLimit(1)
            .foregroundStyle(AppColorKeyText.secondary.color)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, AppDimenKeyPadding.large.value)
            .padding(.vertical, AppDimenKeyPadding.extraSmall.value)
            .background(.bar)
    }

    private func hideKeyboard() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}

/// The avatar and the title of the chat with the interlocutor under it.
private struct ChatRoomTitle: View {
    let avatar: ChatAvatar?
    let title: String
    let interlocutor: String

    var body: some View {
        HStack(spacing: AppDimenKeyPadding.small.value) {
            ChatAvatarView(avatar: avatar, size: 32, font: .subheadline.weight(.semibold))
            VStack(alignment: .leading, spacing: 0) {
                Text(title).font(.headline).lineLimit(1)
                if !interlocutor.isEmpty {
                    Text(interlocutor)
                        .font(.subheadline)
                        .foregroundStyle(AppColorKeyText.secondary.color)
                        .lineLimit(1)
                }
            }
        }
    }
}
