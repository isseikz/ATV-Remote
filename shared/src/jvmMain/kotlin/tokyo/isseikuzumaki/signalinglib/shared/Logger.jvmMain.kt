package tokyo.isseikuzumaki.signalinglib.shared

actual object Logger {
    actual fun d(tag: String, msg: String) {
        println("DEBUG: [$tag] $msg")
    }

    actual fun e(tag: String, msg: String, error: Throwable?) {
        println("ERROR: [$tag] $msg")
        error?.printStackTrace()
    }

    actual fun todo(tag: String, msg: String) {
        println("TODO: [$tag] $msg")
    }
}