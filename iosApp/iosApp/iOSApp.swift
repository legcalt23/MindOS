// iOSApp.swift
//
// SwiftUI application entry point for MindOS iOS.
//
// The Kotlin shared module exposes `MainViewControllerKt.MainViewController()`
// which returns a UIViewController wrapping the Compose Multiplatform tree.
// We host it inside a SwiftUI `UIViewControllerRepresentable` so the rest of
// the iOS shell (status bar, safe area, lifecycle) stays in SwiftUI land.
//
// Firebase is initialized via FirebaseApp.configure() before the Compose tree
// mounts so that any shared-module Firebase call lands in the configured app.

import SwiftUI
import UIKit
import Shared
import FirebaseCore

@main
struct iOSApp: App {

    init() {
        // Reads GoogleService-Info.plist that ships with the app bundle.
        // The plist is mirrored from shared/src/iosMain/resources by the
        // CocoaPods integration (see shared/build.gradle.kts extraSpecAttributes).
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
                .preferredColorScheme(.dark)
        }
    }
}

/// SwiftUI bridge that hosts the Kotlin Compose UIViewController.
private struct ComposeView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Nothing to update from SwiftUI side — Compose owns its own state.
    }
}
