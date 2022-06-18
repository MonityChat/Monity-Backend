package de.devin.monity.network.wsrouting

import de.devin.monity.network.db.user.UserDB
import de.devin.monity.network.wsrouting.actions.*
import de.devin.monity.util.Error
import de.devin.monity.util.validUUID
import filemanagment.util.logInfo
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

object ActionHandler {

    private val registeredActionHandlers = ArrayList<Action>()

    fun loadDefaultActions() {
        //Contact Actions
        registerAction(ContactSearchAction())
        registerAction(ContactAddAction())
        registerAction(ContactDeclineAction())
        registerAction(ContactAcceptAction())
        registerAction(ContactBlockAction())
        registerAction(ContactGetOpenRequest())
        registerAction(ContactGetAction())

        //Profile actions
        registerAction(ProfileUpdateAction())
        registerAction(ProfileGetSelfAction())

        //Private chat actions
        registerAction(PrivateChatSendMessage())
        registerAction(PrivateChatDeleteMessage())
        registerAction(PrivateChatGetLatestMessages())
        registerAction(PrivateChatGetMessages())
        registerAction(PrivateChatEditMessage())
        registerAction(PrivateChatReactMessage())
        registerAction(PrivateChatMessageRead())

        //User Settings
        registerAction(UserSettingsGetAction())
        registerAction(UserSettingsChangeAction())

        //User actions
        registerAction(UserStartTyping())

    }
    private fun registerAction(action: Action) {
        registeredActionHandlers += action
    }

    fun removeAction(action: Action) {
        registeredActionHandlers -= action
    }

    fun handleIncomingActionRequest(sender: UUID, actionRequest: String, request: JSONObject): JSONObject {
        if (!UserDB.has(sender))
            return Error.USER_NOT_FOUND.toJson()

        logInfo("Action: $actionRequest")

        for (action in registeredActionHandlers) {
            if (action.name == actionRequest) {
                for (parameter in action.parameters) {
                    if (!request.has(parameter.key)) return Error.INVALID_JSON_PARAMETER.toJson()

                    if (parameter.key == "target") {
                        val rawID = request.getString("target")
                        if (!validUUID(rawID)) Error.INVALID_JSON_PARAMETER.toJson()
                        val uuid = UUID.fromString(rawID)
                        if (!UserDB.has(uuid)) return Error.USER_NOT_FOUND.toJson()
                     }
                }

                val returnJson = JSONObject()
                returnJson.put("content", action.execute(sender, request))
                returnJson.put("action", actionRequest)

                return returnJson
            }
        }
        return Error.ACTION_NOT_FOUND.toJson()
    }
}