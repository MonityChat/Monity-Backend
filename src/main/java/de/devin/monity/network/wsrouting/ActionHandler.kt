package de.devin.monity.network.wsrouting

import de.devin.monity.network.wsrouting.actions.Action
import de.devin.monity.network.wsrouting.actions.Parameter
import de.devin.monity.util.Error
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

object ActionHandler {

    private val registeredActionHandlers = ArrayList<Action>()


    fun registerAction(action: Action) {
        registeredActionHandlers += action
    }

    fun removeAction(action: Action) {
        registeredActionHandlers -= action
    }

    fun handleIncomingActionRequest(sender: UUID, actionRequest: String, request: JSONObject): JSONObject {
        for (action in registeredActionHandlers) {
            if (action.name == actionRequest) {

                val parameters = mutableListOf<Parameter>()
                for (parameter in action.parameters) {
                    if (!request.has(parameter.key)) return toJSON(Error.INVALID_JSON_PARAMETER)
                    parameters += parameter
                }


                return action.execute(sender, parameters)
            }
        }
        return toJSON(Error.ACTION_NOT_FOUND)
    }

}