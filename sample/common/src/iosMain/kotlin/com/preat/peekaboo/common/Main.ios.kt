import androidx.compose.ui.window.ComposeUIViewController
import com.preat.peekaboo.common.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}