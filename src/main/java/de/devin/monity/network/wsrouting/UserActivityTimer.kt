package de.devin.monity.network.wsrouting

import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.util.Status
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.logInfo
import de.devin.monity.util.notifications.UserUpdatesProfileNotification
import java.util.*
import kotlin.collections.HashMap

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