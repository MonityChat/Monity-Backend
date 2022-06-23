package de.devin.monity.network.wsrouting.actions

import de.devin.monity.network.wsrouting.ActionHandler
import org.json.JSONObject
import java.util.*

data class Parameter(val key: String)

/**
 * Models an executable class which will perform functions on the database based on the input given by the frontend.
 * A class which implements this interface might be filled with parameters, so that in the execute function the parameters can be reused again.
 * To make use of parameters a parameter class is provided
 * @see Parameter
 *
 * All parameters are checked before executing the action and therefore are all provided when the function is called.
 * But there is not checking if the Parameter is in the correct format so checking for that must be done in the execute function.
 *
 */
interface Action {

    /**
     * The unique name of the action
     */
    val name: String

    /**
     * List of parameters
     */
    val parameters: List<Parameter>

    /**
     * Is executed via. the ActionHandler when all requirements are met
     * @see ActionHandler.handleIncomingActionRequest
     *
     * @param sender of the message
     * @param request the request packed as a JSON
     * @return the result of the action as JSON
     */
    fun execute(sender: UUID, request: JSONObject): JSONObject

}