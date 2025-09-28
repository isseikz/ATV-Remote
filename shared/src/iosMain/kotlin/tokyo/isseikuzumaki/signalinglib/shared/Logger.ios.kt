package tokyo.isseikuzumaki.signalinglib.shared

import platform.Foundation.NSLog

actual object Logger {
    actual fun d(tag: String, msg: String) {
        NSLog("[$tag] $msg")
    }

    actual fun e(tag: String, msg: String, error: Throwable?) {
        if (error != null) {
            NSLog("[$tag] $msg\nError: ${error.message}\n${error.stackTraceToString()}")
        } else {
            NSLog("[$tag] $msg")
        }
    }

    actual fun todo(tag: String, msg: String) {
        NSLog("[$tag] TODO: $msg")
    }
}