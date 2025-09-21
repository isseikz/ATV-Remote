package tokyo.isseikuzumaki.atvremote.service

import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.atvremote.SessionManager
import tokyo.isseikuzumaki.atvremote.shared.ISessionService
import tokyo.isseikuzumaki.atvremote.shared.Session

class SessionServiceImpl(
    private val sessionManager: SessionManager
): ISessionService {

    override fun waitingSessions(): Flow<List<Session>> = sessionManager.sessions()
}
