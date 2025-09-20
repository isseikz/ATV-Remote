package tokyo.isseikuzumaki.atvremote

import com.shepeliev.webrtckmp.IceCandidate
import tokyo.isseikuzumaki.atvremote.shared.IceCandidateData

fun IceCandidate.asDomain(): IceCandidateData {
    return IceCandidateData(
        sdpMid = this.sdpMid,
        sdpMLineIndex = this.sdpMLineIndex,
        candidate = this.candidate
    )
}
