package com.contact.ktcall.core.data

import android.telecom.Call
import android.telecom.CallAudioState

data class CallData(
    val id: String,
    val state: CallState,
    val number: String?,
    val displayName: String?,
    val simSlot: Int = 0,
    val isMuted: Boolean = false,
    val isOnHold: Boolean = false,
    val audioRoute: AudioRoute = AudioRoute.EARPIECE
) {
    companion object {
        fun fromTelecomCall(call: Call): CallData {
            return CallData(
                id = call.details.handle?.schemeSpecificPart ?: "unknown",
                state = CallState.fromTelecomState(call.state),
                number = call.details.handle?.schemeSpecificPart,
                displayName = call.details.callerDisplayName,
                simSlot = call.details.accountHandle?.id?.toIntOrNull() ?: 0
            )
        }
    }
}

enum class CallState {
    CONNECTING,
    DIALING,
    RINGING,
    ACTIVE,
    HOLDING,
    DISCONNECTED;

    companion object {
        fun fromTelecomState(state: Int): CallState {
            return when (state) {
                Call.STATE_CONNECTING -> CONNECTING
                Call.STATE_DIALING -> DIALING
                Call.STATE_RINGING -> RINGING
                Call.STATE_ACTIVE -> ACTIVE
                Call.STATE_HOLDING -> HOLDING
                Call.STATE_DISCONNECTED -> DISCONNECTED
                else -> DISCONNECTED
            }
        }
    }
}

enum class AudioRoute {
    EARPIECE,
    SPEAKER,
    BLUETOOTH,
    WIRED_HEADSET;

    val route: Int
        get() = when (this) {
            EARPIECE -> CallAudioState.ROUTE_EARPIECE
            SPEAKER -> CallAudioState.ROUTE_SPEAKER
            BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
            WIRED_HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
        }
}

enum class CallActionType {
    ANSWER,
    DENY,
    HANGUP,
    TOGGLE_MUTE,
    TOGGLE_SPEAKER,
    TOGGLE_HOLD
}

enum class CallsState {
    NO_CALL,
    INCOMING,
    OUTGOING,
    ACTIVE,
    HOLDING
}
