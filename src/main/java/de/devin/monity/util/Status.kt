package de.devin.monity.util


/**
 * The online status of a user
 */
enum class Status {

    /**
     * The user online
     */
    ONLINE,

    /**
     * The user is away
     */
    AWAY,

    /**
     * The user does not want to be disturbed
     * This will stop it from getting notifications in the frontend
     */
    DO_NOT_DISTURB,

    /**
     * The user is offline
     */
    OFFLINE

}