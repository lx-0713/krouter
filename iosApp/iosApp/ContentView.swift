import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let vc = Main_iosKt.MainViewController()
        
        // iOS 原生左边缘手势 → 驱动 Decompose 交互式返回动画
        let edgePan = UIScreenEdgePanGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handleEdgePan(_:))
        )
        edgePan.edges = .left
        vc.view.addGestureRecognizer(edgePan)
        
        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
    
    func makeCoordinator() -> Coordinator { Coordinator() }
    
    class Coordinator: NSObject {
        private var isActive = false
        
        @objc func handleEdgePan(_ gesture: UIScreenEdgePanGestureRecognizer) {
            guard let view = gesture.view else { return }
            let translation = gesture.translation(in: view)
            let progress = Float(max(min(translation.x / view.bounds.width, 1.0), 0.0))
            
            switch gesture.state {
            case .began:
                isActive = true
                Main_iosKt.onBackGestureStarted()
                
            case .changed:
                if isActive {
                    Main_iosKt.onBackGestureProgress(progress: progress)
                }
                
            case .ended:
                if isActive {
                    isActive = false
                    let velocity = gesture.velocity(in: view).x
                    if progress > 0.3 || velocity > 800 {
                        Main_iosKt.onBackGestureCompleted()
                    } else {
                        Main_iosKt.onBackGestureCancelled()
                    }
                }
                
            case .cancelled, .failed:
                if isActive {
                    isActive = false
                    Main_iosKt.onBackGestureCancelled()
                }
                
            default:
                break
            }
        }
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea(.keyboard)
    }
}
