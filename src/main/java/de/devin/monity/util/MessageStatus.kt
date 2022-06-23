package de.devin.monity.util


/**
 * The message of a status
 */
enum class MessageStatus {

    /**
     * The message is pending
     * The receiver has not received it yet
     */
    PENDING,

    /**
     * The message has been received
     */
    RECEIVED,

    /**
     * The message was read
     */
    READ
}