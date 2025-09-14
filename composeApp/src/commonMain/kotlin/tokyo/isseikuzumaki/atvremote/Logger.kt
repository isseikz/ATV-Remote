package tokyo.isseikuzumaki.atvremote

expect object Logger {
    fun d(tag: String, msg: String)
    fun e(tag: String, msg: String, error: Throwable? = null)
}
