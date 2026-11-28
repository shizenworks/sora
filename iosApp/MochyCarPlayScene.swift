// CarPlay template scene for Mochy — the one iOS surface that does not use the shared Compose UI.
// Apple only allows audio apps to display system CPTemplate views in CarPlay (never custom views),
// so this scene builds templates natively while playback and library data stay in the shared Kotlin runtime.

import CarPlay
import Foundation
import Shared
import UIKit

final class MochyCarPlayScene: UIResponder, CPTemplateApplicationSceneDelegate {
    private let notificationCenter = NotificationCenter.default
    private var interfaceController: CPInterfaceController?
    private var observers: [NSObjectProtocol] = []
    private var savedTemplate: CPListTemplate?
    private var recentTemplate: CPListTemplate?
    private var libraryRows: [String: [[String: Any]]] = [:]
    private var currentSongID: String?
    private var isPlaying = false
    private var pendingCompletions: [String: () -> Void] = [:]

    func templateApplicationScene(
        _ scene: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController
    ) {
        self.interfaceController = interfaceController
        installObservers()
        installRootTemplate()

        // This can be the first app scene. It creates the same shared player and database used by Compose.
        IosCarPlayBridgeKt.installIosCarPlayBridge()
        requestLibrary("saved")
        requestLibrary("recent")
        // the phone may have changed songs while the car was away; ask for what's playing now
        post("playbackRequest", payload: [:])
    }

    func templateApplicationScene(
        _ scene: CPTemplateApplicationScene,
        didDisconnect interfaceController: CPInterfaceController
    ) {
        observers.forEach { notificationCenter.removeObserver($0) }
        observers.removeAll()
        pendingCompletions.values.forEach { $0() }
        pendingCompletions.removeAll()
        self.interfaceController = nil
        savedTemplate = nil
        recentTemplate = nil
        libraryRows.removeAll()
    }

    private func installObservers() {
        observe("librarySnapshot") { [weak self] payload in self?.receiveLibrary(payload) }
        observe("playResult") { [weak self] payload in self?.receivePlayResult(payload) }
        observe("playbackChanged") { [weak self] payload in self?.receivePlaybackState(payload) }
    }

    private func observe(_ suffix: String, handler: @escaping (String?) -> Void) {
        let name = Notification.Name("com.mochyy.mochymusic.ios.carplay.\(suffix)")
        observers.append(notificationCenter.addObserver(forName: name, object: nil, queue: .main) { notification in
            handler(notification.object as? String)
        })
    }

    private func installRootTemplate() {
        let saved = CPListTemplate(
            title: "Library",
            sections: [CPListSection(items: [placeholder("Loading library…")])]
        )
        saved.tabTitle = "Library"
        saved.tabImage = UIImage(systemName: "music.note.list")

        let recent = CPListTemplate(
            title: "Recently Played",
            sections: [CPListSection(items: [placeholder("Loading recent tracks…")])]
        )
        recent.tabTitle = "Recent"
        recent.tabImage = UIImage(systemName: "clock")

        savedTemplate = saved
        recentTemplate = recent
        interfaceController?.setRootTemplate(CPTabBarTemplate(templates: [saved, recent]), animated: false, completion: nil)
    }

    private func placeholder(_ title: String) -> CPListItem {
        let item = CPListItem(text: title, detailText: nil)
        item.isEnabled = false
        return item
    }

    private func requestLibrary(_ collection: String) {
        post("libraryRequest", payload: [
            "collection": collection,
            "limit": CPListTemplate.maximumItemCount,
        ])
    }

    private func receiveLibrary(_ encoded: String?) {
        guard let payload = decode(encoded),
              let collection = payload["collection"] as? String,
              let rows = payload["items"] as? [[String: Any]]
        else { return }

        libraryRows[collection] = rows
        updateList(collection)
    }

    private func updateList(_ collection: String) {
        guard let rows = libraryRows[collection] else { return }
        let items = rows.prefix(CPListTemplate.maximumItemCount).compactMap { row -> CPListItem? in
            guard let id = row["id"] as? String,
                  let title = row["title"] as? String
            else { return nil }

            let item = CPListItem(text: title, detailText: row["artist"] as? String)
            item.isExplicitContent = row["isExplicit"] as? Bool ?? false
            item.isPlaying = id == currentSongID && isPlaying
            item.userInfo = ["id": id, "collection": collection]
            item.handler = { [weak self] _, completion in
                guard let self else {
                    completion()
                    return
                }
                self.play(id: id, from: collection, completion: completion)
            }
            return item
        }

        let visibleItems = items.isEmpty ? [placeholder(emptyText(for: collection))] : items
        let section = CPListSection(items: visibleItems)
        if collection == "saved" {
            savedTemplate?.updateSections([section])
        } else if collection == "recent" {
            recentTemplate?.updateSections([section])
        }
    }

    private func emptyText(for collection: String) -> String {
        collection == "saved" ? "No saved tracks yet" : "No recently played tracks"
    }

    private func play(id: String, from collection: String, completion: @escaping () -> Void) {
        let requestID = UUID().uuidString
        pendingCompletions[requestID] = completion
        post("playRequest", payload: [
            "requestId": requestID,
            "collection": collection,
            "id": id,
        ])

        DispatchQueue.main.asyncAfter(deadline: .now() + 50) { [weak self] in
            guard let self, let completion = self.pendingCompletions.removeValue(forKey: requestID) else { return }
            completion()
        }
    }

    private func receivePlayResult(_ encoded: String?) {
        guard let payload = decode(encoded),
              let requestID = payload["requestId"] as? String,
              let completion = pendingCompletions.removeValue(forKey: requestID)
        else { return }

        let ready = payload["ready"] as? Bool ?? false
        completion()
        // CarPlay refuses a template that is already in the stack, so only push Now Playing once.
        if ready, let controller = interfaceController,
           !controller.templates.contains(where: { $0 === CPNowPlayingTemplate.shared }) {
            controller.pushTemplate(CPNowPlayingTemplate.shared, animated: true, completion: nil)
        }
    }

    private func receivePlaybackState(_ encoded: String?) {
        guard let payload = decode(encoded) else { return }
        currentSongID = payload["songId"] as? String
        isPlaying = payload["isPlaying"] as? Bool ?? false
        updateList("saved")
        updateList("recent")
    }

    private func decode(_ encoded: String?) -> [String: Any]? {
        guard let encoded, let data = encoded.data(using: .utf8) else { return nil }
        return (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
    }

    private func post(_ suffix: String, payload: [String: Any]) {
        guard let data = try? JSONSerialization.data(withJSONObject: payload),
              let encoded = String(data: data, encoding: .utf8)
        else { return }
        notificationCenter.post(
            name: Notification.Name("com.mochyy.mochymusic.ios.carplay.\(suffix)"),
            object: encoded
        )
    }
}
