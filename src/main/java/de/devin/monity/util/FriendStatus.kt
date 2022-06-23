package de.devin.monity.util


/**
 * The status between 2 users
 */
enum class FriendStatus {

    /**
     * The friend request is pending
     */
    PENDING,

    /**
     * the friend request was declined
     */
    DECLINED,

    /**
     * the user has blocked the other user
     */
    BLOCKED,

    /**
     * the friend request was accepted
     * both users are now in contact with each other
     */
    ACCEPTED

}