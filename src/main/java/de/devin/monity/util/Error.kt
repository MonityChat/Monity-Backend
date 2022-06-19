package de.devin.monity.util

import org.json.JSONObject

enum class Error {

    /**
     * Occurs when no error occurred
     */
    NONE,

    /**
     * Occurs when an email address is already in use.
     * For example on registration process
     */
    EMAIL_ALREADY_IN_USE,

    /**
     * Occurs when a username is already in use
     * For example on registration process
     */
    USERNAME_ALREADY_IN_USE,

    /**
     * Occurs when a verification url is opened and the given ID und UUID are not matching
     */
    INVALID_ID_UUID_COMBINATION,

    /**
     *  Occurs when a verification url is opened with a not existing ID
     */
    INVALID_CONFIRMATION,

    /**
     * Occurs when the given email address is not found
     */
    EMAIL_NOT_FOUND,

    /**
     * Occurs when the given user was not found
     */
    USER_NOT_FOUND,

    /**
     * Occurs when an invalid password is given
     */
    INVALID_PASSWORD,

    /**
     * Occurs when reset password url is opened with an invalid ID
     */
    INVALID_RESET_REQUEST,

    /**
     * Occurs when the email and username in the required login-json are missing
     */
    INVALID_LOGIN_REQUEST,

    /**
     * Occurs when the required UUID in websocket authorization is not matching the required format
     */
    INVALID_UUID_FORMAT,

    /**
     * Occurs when a message is sent via. ws and is not matching the standard JSON format
     */
    INVALID_JSON_FORMAT,

    /**
     * Occurs when an action is performed but the authorization is too low or is invalid
     */
    UNAUTHORIZED,

    /**
     * Occurs when the "action" key is missing in a ws JSON
     */
    INVALID_JSON_STRUCTURE,

    /**
     * Occurs when the registration window timed out and there was no successful registration
     */
    IDENTIFICATION_WINDOW_TIMEOUT,

    /**
     * Occurs when a required parameter is missing
     */
    INVALID_JSON_PARAMETER,

    /**
     * Occurs when the given action does not exist
     */
    ACTION_NOT_FOUND,

    /**
     * Occurs when a user tries to add a contact when already added
     */
    ALREADY_MADE_CONTACT,

    /**
     * Occurs when a user has already requested to join a group
     */
    ALREADY_MADE_REQUEST,

    /**
     * Occurs when a user tries to perform an action on a target but blocked the target
     */
    USER_BLOCKED_TARGET,

    /**
     * Occurs when a user
     */
    TARGET_BLOCKED_USER,

    /**
     * Occurs when a requested chat does not exist
     */
    CHAT_NOT_FOUND,

    /**
     * Occurs when a requested chat does nto exist
     */
    MESSAGE_NOT_FOUND,

    /**
     * Occurs when a users reacts to a message he has already reacted too
     */
    USER_ALREADY_REACTED,

    /**
     * Occurs when a user tries to update a non-existing setting
     */
    INVALID_SETTING,

    /**
     * Occurs when the given group does not exist
     */
    GROUP_NOT_FOUND,

    /**
     * Occurs when a user tries to join a group where he is already included
     */
    USER_ALREADY_IN_GROUP,

    /**
     * Occurs when a user tries to join a group where no request is needed
     */
    GROUP_DOES_NOT_REQUIRE_REQUEST,

    /**
     * Occurs when a user tries to join a group which is closed
     */
    GROUP_IS_CLOSED,

    /**
     * Occurs when a users tries to perform an action on a target but the target the blocked the user
     */
    INVALID_FRIEND_DECLINE_REQUEST,


    /**
     * Occurs when a moderator accepts/decline a non-existing request
     */
    USER_DID_NOT_REQUEST,

    /**
     * Occurs when a user is not permitted to invite a user to a group
     */
    CANT_INVITE_USER_DUE_TO_PRIVATE_SETTINGS,

    /**
     * Occurs when an invitation is canceled, but it doesn't exist
     */
    USER_NOT_INVITED,

    /**
     * Occurs when a users tries to send another request to a user
     */
    ALREADY_SEND_REQUEST,

    /**
     * Occurs when a users tries to accept a non-existing friend request
     */
    INVALID_FRIEND_ACCEPT_REQUEST;


    fun toJson(): JSONObject {
        return JSONObject("{\"error\":\"$this\"}")
    }

}