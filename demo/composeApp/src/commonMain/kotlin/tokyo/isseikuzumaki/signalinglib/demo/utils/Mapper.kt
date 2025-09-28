package tokyo.isseikuzumaki.signalinglib.demo.utils

import com.shepeliev.webrtckmp.IceCandidate
import tokyo.isseikuzumaki.signalinglib.shared.IceCandidateData

fun IceCandidate.asDomain(): IceCandidateData {
    return IceCandidateData(
        sdpMid = this.sdpMid,
        sdpMLineIndex = this.sdpMLineIndex,
        candidate = this.candidate
    )
}