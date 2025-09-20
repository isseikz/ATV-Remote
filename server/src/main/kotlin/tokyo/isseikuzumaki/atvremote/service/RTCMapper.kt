package tokyo.isseikuzumaki.atvremote.service

import dev.onvoid.webrtc.RTCIceCandidate
import tokyo.isseikuzumaki.atvremote.shared.IceCandidateData

fun RTCIceCandidate.asDomain(): IceCandidateData {
    return IceCandidateData(
        sdpMid = this.sdpMid,
        sdpMLineIndex = this.sdpMLineIndex,
        candidate = this.sdp
    )
}

fun IceCandidateData.asLibrary(): RTCIceCandidate {
    return RTCIceCandidate(this.sdpMid, this.sdpMLineIndex, this.candidate)
}
