package tokyo.isseikuzumaki.signalinglib.shared

import android.util.Log

actual object Logger {
    actual fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    actual fun e(tag: String, msg: String, error: Throwable?) {
        Log.e(tag, msg, error)
    }

    actual fun todo(tag: String, msg: String) {
        Log.w(tag, "TODO: $msg")
    }
}