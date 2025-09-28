package tokyo.isseikuzumaki.signalinglib.demo.server.service

import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.signalinglib.server.SessionManager
import tokyo.isseikuzumaki.signalinglib.shared.ISessionService
import tokyo.isseikuzumaki.signalinglib.shared.Session

class SessionServiceImpl(
    private val sessionManager: SessionManager
): ISessionService {

    override fun waitingSessions(): Flow<List<Session>> = sessionManager.sessions()
}
