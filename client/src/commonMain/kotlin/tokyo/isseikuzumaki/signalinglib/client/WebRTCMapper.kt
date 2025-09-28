package tokyo.isseikuzumaki.signalinglib.client

import com.shepeliev.webrtckmp.IceCandidate
import tokyo.isseikuzumaki.signalinglib.shared.IceCandidateData

fun IceCandidate.asDomain(): IceCandidateData {
    return IceCandidateData(
        sdpMid = this.sdpMid,
        sdpMLineIndex = this.sdpMLineIndex,
        candidate = this.candidate
    )
}

fun IceCandidateData.asLibrary(): IceCandidate {
    return IceCandidate(
        sdpMid = this.sdpMid,
        sdpMLineIndex = this.sdpMLineIndex,
        candidate = this.candidate
    )
}