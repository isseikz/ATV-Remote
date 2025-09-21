package tokyo.isseikuzumaki.atvremote

expect suspend fun exportToLocal(
    fileName: String,
    byteArray: ByteArray,
): Result<Unit>
