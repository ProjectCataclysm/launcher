package ru.cataclysm.services.account

import ru.cataclysm.helpers.ApiException
import ru.cataclysm.helpers.RequestHelper
import ru.cataclysm.services.Log
import ru.cataclysm.services.Settings

object AccountService {
    var session: Session? = null
        private set
    private val sessionFile = Settings.LAUNCHER_DIR_PATH.resolve("session").toFile()


    fun validateSession() {
        if (!sessionFile.exists()) return
        val token = Token(sessionFile.readText())

        try {
            val json = RequestHelper.post("client/validate", token.toJson())
            session = RequestHelper.parseResponse<Session>(json)
        } catch (e: ApiException) {
            session = null; throw e
        } finally {
            saveSession()
        }
    }

    fun authorize(login: String, password: String) {
        val auth = Auth(login, password)
        val json = RequestHelper.post("client/auth", auth.toJson())
        session = RequestHelper.parseResponse<Session>(json)
        saveSession()
    }
    fun logout() {
        val token = Token(session!!.accessToken)
        val json = RequestHelper.post("client/invalidate", token.toJson())
        RequestHelper.parseResponse<String>(json)
        session = null
        saveSession()
    }

    private fun saveSession() {
        try {
            if (session != null) {
                sessionFile.writeText(session!!.accessToken)
            } else {
                sessionFile.delete()
            }
        } catch (e: Exception) {
            Log.err(e, "Failed to write session to file")
        }
    }
}