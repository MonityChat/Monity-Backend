package de.devin.monity.network.wsrouting

import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.util.Status
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.logInfo
import de.devin.monity.util.notifications.UserUpdatesProfileNotification
import java.util.*
import kotlin.collections.HashMap


/**
 * The user activity Timer will track the activity of a user on the network.
 * If the user is for too long not sending an action, he will be set to AWAY.
 *
 * To achieve this every online user is saved in a map with an integer.
 * The integer starts by a value of 10 (initial login). When he executes an action the integer will reset to 10.
 * Every minute the integer gets count down by 1. If the Integer reaches a value below 1 it will set the user to AWAY and
 * notify contacts that he is now away.
 *
 * If then another action is received the user is then reset again to a value of 10 and back to online.
 */
object UserActivityTimer: Runnable {

    private val activityMap = HashMap<UUID, Int>()
    override fun run() {
        for (uuid in activityMap.keys) {

            if (!UserHandler.isOnline(uuid)) {
                activityMap.remove(uuid)
                return
            }

            activityMap[uuid] = activityMap[uuid]!! - 1

            if (activityMap[uuid]!! <= 0) {
                val oldStatus = DetailedUserDB.get(uuid).status
                DetailedUserDB.setStatus(uuid, Status.AWAY)
                if (oldStatus != Status.AWAY) {
                    for (contact in UserHandler.getOfflineUser(uuid).contacts) {
                        UserHandler.sendNotificationIfOnline(contact, UserUpdatesProfileNotification(uuid))
                    }
                }
            }
        }
    }

    /**
     * Will reset the users internal timer and will notify all contacts when he goes back to being online
     * @param uuid the user
     */
    fun userExecutedAction(uuid: UUID) {
        activityMap[uuid] = 10

        val oldStatus = DetailedUserDB.get(uuid).status
        DetailedUserDB.setStatus(uuid, DetailedUserDB.get(uuid).preferredStatus)

        if (oldStatus != DetailedUserDB.get(uuid).preferredStatus) {
            for (contact in UserHandler.getOfflineUser(uuid).contacts) {
                UserHandler.sendNotificationIfOnline(contact, UserUpdatesProfileNotification(uuid))
            }
        }
    }
}