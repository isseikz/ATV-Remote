package tokyo.isseikuzumaki.signalinglib.server

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import tokyo.isseikuzumaki.signalinglib.shared.Session
import tokyo.isseikuzumaki.signalinglib.shared.SessionID
import tokyo.isseikuzumaki.signalinglib.shared.SessionRequest

class SessionManager {
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    fun sessions(): Flow<List<Session>> = _sessions

    fun register(request: SessionRequest): Flow<Result<Session>> {
        val newSession = Session(
            id = SessionID(java.util.UUID.randomUUID().toString()),
            name = request.name,
            capabilities = mutableListOf<String>().apply {
            }
        )
        _sessions.value = _sessions.value + newSession
        return flowOf(Result.success(newSession))
    }

    fun unregister(sessionID: SessionID): Flow<Result<Unit>> {
        _sessions.value = _sessions.value.filterNot { it.id == sessionID }
        return flowOf(Result.success(Unit))
    }
}