package tokyo.isseikuzumaki.signalinglib.server

import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.signalinglib.shared.ISessionService
import tokyo.isseikuzumaki.signalinglib.shared.Session

class SessionServiceImpl(
    private val sessionManager: SessionManager
): ISessionService {

    override fun waitingSessions(): Flow<List<Session>> = sessionManager.sessions()
}