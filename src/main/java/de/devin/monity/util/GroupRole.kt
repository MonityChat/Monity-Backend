package de.devin.monity.util


/**
 * Roles which exist in a group
 *
 * @param weight hierarchy integer
 */
enum class GroupRole(val weight: Int) {


    /**
     * Member in the group
     */
    MEMBER(0),

    /**
     * Moderator in the group
     * can accept/decline incoming request
     * can kick other members
     */
    MODERATOR(1),

    /**
     * Admin in the group
     * can make other user moderator
     * can also kick moderators
     */
    ADMIN(2),

    /**
     * The owner of the group
     * Has all permissions
     */
    OWNER(3)

}