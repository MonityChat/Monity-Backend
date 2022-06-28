package de.devin.monity.network.wsrouting

import de.devin.monity.network.db.user.UserDB
import de.devin.monity.network.wsrouting.actions.*
import de.devin.monity.util.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


/**
 * This util class will handle any existing action and incoming request.
 * The main purpose is to handle incoming request, search the correct action and then execute the action.
 *
 * @see ActionHandler.handleIncomingActionRequest
 *
 */
object ActionHandler {

    private val registeredActionHandlers = ArrayList<Action>()


    /**
     * loads all default actions and registers them
     */
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
        registerAction(ProfileGetOtherAction())

        //Private chat actions
        registerAction(PrivateChatSendMessageAction())
        registerAction(PrivateChatDeleteMessageAction())
        registerAction(PrivateChatGetLatestMessagesAction())
        registerAction(PrivateChatGetMessagesAction())
        registerAction(PrivateChatEditMessageAction())
        registerAction(PrivateChatReactMessageAction())
        registerAction(PrivateChatMessageReadAction())

        //Group chat actions
        registerAction(GroupChatSendMessageAction())
        registerAction(GroupChatEditMessageAction())
        registerAction(GroupChatDeleteMessageAction())
        registerAction(GroupChatCreateAction())

        registerAction(GroupChatInviteUserAction())
        registerAction(GroupChatCancelInviteAction())
        registerAction(GroupChatAcceptInviteAction())
        registerAction(GroupChatDeclineInviteAction())

        registerAction(GroupChatRequestInviteAction())
        registerAction(GroupChatDeclineRequestAction())
        registerAction(GroupChatAcceptRequestAction())


        //User Settings
        registerAction(UserSettingsGetAction())
        registerAction(UserSettingsChangeAction())

        //User actions
        registerAction(UserTypingAction())

    }
    private fun registerAction(action: Action) {
        registeredActionHandlers += action
    }

    /**
     * Handles the incoming content and matches, if existing, an action to it
     *
     * Because every action consists of parameters, it will check if the request contains every parameter, if not an error will be returned.
     * After finding the correct action it will execute it and afterwards return the actions return to the sender.
     * @see Action
     *
     * @param sender the sender of the action
     * @param actionRequest the action
     * @param request the request body
     * @return either error or the return of the executed action
     */
    fun handleIncomingActionRequest(sender: UUID, actionRequest: String, request: JSONObject): JSONObject {
        if (!UserDB.has(sender))
            return Error.USER_NOT_FOUND.toJson()

        logInfo("Action ${ConsoleColors.RED_BRIGHT}$actionRequest call ${ConsoleColors.RESET}by ${ConsoleColors.GREEN_BRIGHT}${UserDB.get(sender).username}")

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
                UserActivityTimer.userExecutedAction(sender)
                logDebug("Executing action $actionRequest from $sender")
                logDebug(actionRequest)

                return returnJson
            }
        }
        return Error.ACTION_NOT_FOUND.toJson()
    }
}