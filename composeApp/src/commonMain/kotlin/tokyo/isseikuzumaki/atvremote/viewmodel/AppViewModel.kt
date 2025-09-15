package tokyo.isseikuzumaki.atvremote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shepeliev.webrtckmp.IceConnectionState
import com.shepeliev.webrtckmp.OfferAnswerOptions
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.SessionDescription
import com.shepeliev.webrtckmp.SessionDescriptionType
import com.shepeliev.webrtckmp.SignalingState
import com.shepeliev.webrtckmp.VideoTrack
import com.shepeliev.webrtckmp.onIceCandidate
import com.shepeliev.webrtckmp.onIceConnectionStateChange
import com.shepeliev.webrtckmp.onSignalingStateChange
import io.ktor.client.HttpClient
import io.ktor.http.path
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import tokyo.isseikuzumaki.atvremote.shared.AtvControlService
import tokyo.isseikuzumaki.atvremote.shared.Logger
import tokyo.isseikuzumaki.atvremote.shared.SERVER_DOMAIN
import tokyo.isseikuzumaki.atvremote.shared.SERVER_PORT
import tokyo.isseikuzumaki.atvremote.shared.SdpOffer


class AppViewModel : ViewModel() {
    val rpcClient by lazy {
        HttpClient {
            installKrpc {
                waitForServices = true
            }
        }.rpc {
            url {
                host = SERVER_DOMAIN
                port = SERVER_PORT
                path("rpc")
            }

            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }
    val service by lazy {
        rpcClient.withService<AtvControlService>()
    }

    val activeVideo = flowOf<VideoTrack?>(null)

    fun openVideo() {
        Logger.d(TAG, "openVideo")
        val remote = PeerConnection().apply {
            onIceCandidate.onEach { candidate ->
                Logger.d(TAG, "onIceCandidate: $candidate")
            }.launchIn(viewModelScope)
            onSignalingStateChange.onEach { signalingState ->
                Logger.d(TAG, "onSignalingStateChange: $signalingState")
                when (signalingState) {
                    SignalingState.Stable -> TODO()
                    SignalingState.HaveLocalOffer -> TODO()
                    SignalingState.HaveLocalPranswer -> TODO()
                    SignalingState.HaveRemoteOffer -> TODO()
                    SignalingState.HaveRemotePranswer -> TODO()
                    SignalingState.Closed -> TODO()
                }
            }.launchIn(viewModelScope)
            onIceConnectionStateChange.onEach { iceConnectionState ->
                Logger.d(TAG, "onIceConnectionStateChange: $iceConnectionState")
                when (iceConnectionState) {
                    IceConnectionState.New -> TODO()
                    IceConnectionState.Checking -> TODO()
                    IceConnectionState.Connected -> TODO()
                    IceConnectionState.Completed -> TODO()
                    IceConnectionState.Failed -> TODO()
                    IceConnectionState.Disconnected -> TODO()
                    IceConnectionState.Closed -> TODO()
                    IceConnectionState.Count -> TODO()
                }
            }.launchIn(viewModelScope)
        }

        val local = PeerConnection().apply {

        }

        viewModelScope.launch {
            val offer = local.createOffer(
                OfferAnswerOptions(
                    offerToReceiveAudio = false,
                    offerToReceiveVideo = true,
                )
            )
            local.setLocalDescription(offer)

            service.sendSdpOffer(
                SdpOffer(
                    sdp = offer.sdp,
                    type = "offer"
                )
            ).onEach { answer ->
                Logger.d(TAG, "onSdpAnswer: $answer")

                //FIXME: Js Exception
                remote.setRemoteDescription(
                    SessionDescription(
                        type = SessionDescriptionType.Answer,
                        sdp = answer.sdp
                    )
                )
            }.first()
        }
    }

    companion object {
        private const val TAG = "AppViewModel"
    }
}
