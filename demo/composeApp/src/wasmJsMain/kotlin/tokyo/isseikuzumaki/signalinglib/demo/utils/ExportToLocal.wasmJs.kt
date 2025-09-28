package tokyo.isseikuzumaki.signalinglib.demo.utils

import kotlin.js.toJsArray
import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

actual suspend fun exportToLocal(
    fileName: String,
    byteArray: ByteArray,
): Result<Unit> {
    val blob = Blob(
        (arrayOf(byteArray) as Array<JsAny?>).toJsArray(),
        BlobPropertyBag("application/octet-stream")
    )

    // Blobへの一時的なURLを生成します。
    val url = URL.createObjectURL(blob)

    // ダウンロード用の<a>アンカータグをメモリ上に作成します。
    val anchor = document.createElement("a") as HTMLAnchorElement
    anchor.href = url
    anchor.download = fileName

    // ページに追加してクリックし、すぐに削除します。
    // これにより、ユーザーにダウンロードダイアログが表示されます。
    document.body?.appendChild(anchor)
    anchor.click()
    document.body?.removeChild(anchor)

    // 使い終わった一時URLを解放してメモリリークを防ぎます。
    URL.revokeObjectURL(url)

    return Result.success(Unit)
}