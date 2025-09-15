package tokyo.isseikuzumaki.atvremote

actual object Logger {
    actual fun d(tag: String, msg: String) {
        println("DEBUG: [$tag] $msg")
    }

    actual fun e(tag: String, msg: String, error: Throwable?) {
        println("ERROR: [$tag] $msg")
        error?.printStackTrace()
    }
}