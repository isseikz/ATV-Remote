package tokyo.isseikuzumaki.signalinglib.shared

expect object Logger {
    fun d(tag: String, msg: String)
    fun e(tag: String, msg: String, error: Throwable? = null)
    fun todo(tag: String, msg: String)
}