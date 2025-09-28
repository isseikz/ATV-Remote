package tokyo.isseikuzumaki.signalinglib.demo.utils

expect suspend fun exportToLocal(
    fileName: String,
    byteArray: ByteArray,
): Result<Unit>