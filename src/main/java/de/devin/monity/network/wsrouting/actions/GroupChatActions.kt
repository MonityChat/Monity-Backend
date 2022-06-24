package de.devin.monity.network.wsrouting.actions

import com.google.gson.Gson
import de.devin.monity.network.db.chat.*
import de.devin.monity.network.db.user.FriendRequestLevel
import de.devin.monity.network.db.user.UserContactDB
import de.devin.monity.network.db.user.UserSettingsDB
import de.devin.monity.util.Error
import de.devin.monity.util.GroupRole
import de.devin.monity.util.MessageStatus
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.notifications.*
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*


/**
 * Action when a user sends a message
 */
class GroupChatSendMessageAction: Action {

    override val name: String
        get() = "chat:group:send:message"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("chatID"), Parameter("embedID"), Parameter("content"), Parameter("sent"), Parameter("related"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val chatIDRaw = request.getString("chatID")
        val chatID = UUID.fromString(chatIDRaw)

        val content = request.getString("content")
        val sent = request.getLong("sent")
        val embedID = request.getString("embedID")
        val relatedID = request.getString("related")

        val user = UserHandler.getOnlineUser(sender)
        val chats = user.groupChats

        if (chats.none {it.groupID == chatID}) return Error.CHAT_NOT_FOUND.toJson()
        val chat = GroupDB.get(chatID)

        if (GroupSettingDB.get(chatID).rolesOnly) {
            if (GroupMemberDB.getGroupMemberFor(chatID, sender).role.weight <= GroupRole.MEMBER.weight) return Error.UNAUTHORIZED.toJson()
        }

        val messageID = MessageDB.getFreeUUID()
        val index = MessageDB.getNextIndex(chat.groupID)
        val embeds = if (embedID.isNotEmpty()) MediaDB.get(UUID.fromString(embedID)) else listOf()
        val related = if (relatedID.isNotEmpty()) MessageDB.get(UUID.fromString(relatedID)) else null


        val status = if (chat.members.all { UserHandler.isOnline(it.id) }) MessageStatus.RECEIVED else MessageStatus.PENDING

        val message = MessageData(sender, messageID, chat.groupID, content, related, embeds, listOf(), index, sent, false, status)

        MessageDB.insert(message)


        chat.members.forEach{ UserHandler.sendNotificationIfOnline(it.id, GroupChatMessageReceivedNotification(sender, chatID, message))}
        chat.members.forEach {
            GroupMessageStatusDB.insert(GroupMessageStatus(messageID, chatID, it.id, if (UserHandler.isOnline(it.id)) MessageStatus.RECEIVED else MessageStatus.PENDING))
        }
        return toJSON(message).put("author", user.getUserName())
    }
}


/**
 * Action when a user deletes a message
 */
class GroupChatDeleteMessageAction: Action {
    override val name: String
        get() = "chat:group:delete:message"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("chatID"), Parameter("messageID"))
    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val chatIDRaw = request.getString("chatID")
        val chatID = UUID.fromString(chatIDRaw)
        val messageIDRaw = request.getString("messageID")
        val messageID = UUID.fromString(messageIDRaw)


        if (!ChatDB.has(chatID)) return Error.CHAT_NOT_FOUND.toJson()
        if (!MessageDB.has(messageID)) return Error.MESSAGE_NOT_FOUND.toJson()

        MessageDB.removeMessage(messageID)
        GroupMessageStatusDB.deleteAllMessages(messageID)

        val chat = GroupDB.get(chatID)

        chat.members.forEach {UserHandler.sendNotificationIfOnline(it.id, GroupChatMessageDeletedNotification(sender, chatID, messageID)) }

        return Error.NONE.toJson()
    }
}

/**
 * Action when a user edits a message
 */
class GroupChatEditMessageAction: Action {
    override val name: String
        get() = "chat:group:message:edit"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("chatID"), Parameter("messageID"), Parameter("newContent"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val chatIDRaw = request.getString("chatID")
        val chatID = UUID.fromString(chatIDRaw)
        val messageIDRaw = request.getString("messageID")
        val messageID = UUID.fromString(messageIDRaw)


        if (!ChatDB.has(chatID)) return Error.CHAT_NOT_FOUND.toJson()
        if (!MessageDB.has(messageID)) return Error.MESSAGE_NOT_FOUND.toJson()

        MessageDB.editMessageContent(messageID, request.getString("newContent"))
        val chat = GroupDB.get(chatID)

        chat.members.forEach {UserHandler.sendNotificationIfOnline(it.id, GroupChatMessageEditNotification(sender, chatID, MessageDB.get(messageID))) }

        return Error.NONE.toJson()
    }
}


/**
 * Action when a user creates a new group
 */
class GroupChatCreateAction: Action {

    override val name: String
        get() = "chat:group:create"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("invites"), Parameter("profile"), Parameter("settings"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {

        val invites = request.getJSONArray("invites").map { it.toString() }
        val profileObject = request.getJSONObject("profile")
        val profile = Gson().fromJson(profileObject.toString(), GroupProfile::class.java)

        val settingsObject = request.getJSONObject("settings")
        val settings = Gson().fromJson(settingsObject.toString(), GroupSettings::class.java)


        val groupID = GroupDB.newUUID()
        val groupInvites = invites.map { GroupInvite(UUID.fromString(it), groupID) }
        val groupChat = GroupChatData(sender, listOf(GroupMemberData(sender, groupID, GroupRole.OWNER)), listOf(), groupID, System.currentTimeMillis(), settings, groupInvites, profile, emptyList())

        GroupDB.insert(groupChat)
        GroupSettingDB.insert(settings)
        GroupProfileDB.insert(profile)
        groupInvites.forEach { GroupMemberInvitesDB.insert(listOf(it)) }

        return toJSON(groupChat)
    }
}

/**
 * Action when a user requests to join a group
 */
class GroupChatRequestInviteAction: Action {
    override val name: String
        get() = "chat:group:request"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("groupID"), Parameter("content"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val groupID = UUID.fromString(request.getString("groupID"))
        if (!GroupDB.has(groupID)) return Error.GROUP_NOT_FOUND.toJson()

        val settings = GroupSettingDB.get(groupID)

        if (GroupMemberDB.isInGroup(sender, groupID)) return Error.USER_ALREADY_IN_GROUP.toJson()
        if (!settings.requiresRequest) return Error.GROUP_DOES_NOT_REQUIRE_REQUEST.toJson()
        if (!settings.opened) return Error.GROUP_IS_CLOSED.toJson()
        if (GroupRequestDB.hasRequested(sender, groupID)) return Error.ALREADY_MADE_REQUEST.toJson()

        val requestContent = request.getString("content")
        val groupRequest = GroupRequest(sender, groupID, requestContent)
        GroupRequestDB.insert(listOf(groupRequest))

        for (moderator in GroupMemberDB.get(groupID).filter { it.role == GroupRole.MODERATOR }) {
            UserHandler.sendNotificationIfOnline(moderator.id, GroupChatUserRequestedJoinNotification(sender, groupID, requestContent))
        }

        return Error.NONE.toJson()
    }
}

/**
 * Action when a user declines a join request from another user
 */
class GroupChatDeclineRequestAction: Action {

    override val name: String
        get() = "chat:group:request:decline"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("groupID"), Parameter("target"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val targetUUID = UUID.fromString(request.getString("target"))
        val groupID = UUID.fromString(request.getString("groupID"))
        if (!GroupDB.has(groupID)) return Error.GROUP_NOT_FOUND.toJson()

        if (!GroupRequestDB.hasRequested(targetUUID, groupID)) return Error.USER_DID_NOT_REQUEST.toJson()

        GroupRequestDB.removeRequest(targetUUID, groupID)
        UserHandler.sendNotificationIfOnline(targetUUID, GroupChatRequestDeclinedNotification(sender, targetUUID, groupID))

        for (moderator in GroupMemberDB.get(groupID).filter { it.role == GroupRole.MODERATOR }) {
            UserHandler.sendNotificationIfOnline(moderator.id, GroupChatRequestDeclinedNotification(sender, targetUUID, groupID))
        }

        return Error.NONE.toJson()
    }
}

/**
 * Action when a user accepts the join request of another user
 */
class GroupChatAcceptRequestAction: Action {

    override val name: String
        get() = "chat:group:request:accept"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("groupID"), Parameter("target"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val targetUUID = UUID.fromString(request.getString("target"))
        val groupID = UUID.fromString(request.getString("groupID"))
        if (!GroupDB.has(groupID)) return Error.GROUP_NOT_FOUND.toJson()

        if (!GroupRequestDB.hasRequested(targetUUID, groupID)) return Error.USER_DID_NOT_REQUEST.toJson()

        GroupRequestDB.removeRequest(targetUUID, groupID)
        val member = GroupMemberData(targetUUID, groupID, GroupRole.MEMBER)
        GroupMemberDB.insert(listOf(member))

        UserHandler.sendNotificationIfOnline(targetUUID, GroupChatRequestAcceptedNotification(sender, targetUUID, groupID))

        for (moderator in GroupMemberDB.get(groupID).filter { it.role == GroupRole.MODERATOR }) {
            UserHandler.sendNotificationIfOnline(moderator.id, GroupChatRequestAcceptedNotification(sender, targetUUID, groupID))
        }

        for (groupMember in GroupMemberDB.get(groupID)) {
            UserHandler.sendNotificationIfOnline(groupMember.id, GroupChatUserJoinedNotification(sender, groupID))
        }

        return Error.NONE.toJson()
    }
}

/**
 * Action when a user invites another user
 */
class GroupChatInviteUserAction: Action {

    override val name: String
        get() = "chat:group:invite:user"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("target"), Parameter("groupID"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val targetUUID = UUID.fromString(request.getString("target"))
        val groupID = UUID.fromString(request.getString("groupID"))

        if (!GroupDB.has(groupID)) return Error.GROUP_NOT_FOUND.toJson()

        val settings = GroupSettingDB.get(groupID)
        val role = GroupMemberDB.getGroupMemberFor(groupID, sender)

        if (UserContactDB.hasBlocked(targetUUID, sender)) return Error.TARGET_BLOCKED_USER.toJson()
        if (settings.whoCanInvite.weight > role.role.weight) return Error.UNAUTHORIZED.toJson()

        val userSettingsOfTarget = UserSettingsDB.get(targetUUID)

        if (userSettingsOfTarget.friendRequestLevel == FriendRequestLevel.NONE) return Error.CANT_INVITE_USER_DUE_TO_PRIVATE_SETTINGS.toJson()

        val invite = GroupInvite(targetUUID, groupID)
        GroupMemberInvitesDB.insert(listOf(invite))

        for (member in GroupMemberDB.get(groupID)) {
            UserHandler.sendNotificationIfOnline(member.id, GroupChatUserInvitedUserNotification(sender, targetUUID, groupID))
        }
        UserHandler.sendNotificationIfOnline(targetUUID, GroupChatUserInvitedYouNotification(sender, groupID))

        return Error.NONE.toJson()
    }
}

/**
 * Action when a user kicks another user
 */
class GroupChatKickMember: Action {


    override val name: String
        get() = "chat:group:kick"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("groupID"), Parameter("target"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val targetUUID = UUID.fromString(request.getString("target"))
        val groupID = UUID.fromString(request.getString("groupID"))

        if (!GroupDB.has(groupID)) return Error.GROUP_NOT_FOUND.toJson()

        //if (GroupMemberDB.get())

        return Error.NONE.toJson()
    }
}

/**
 * Action when a user accepts an invitation
 */
class GroupChatAcceptInviteAction: Action {

    override val name: String
        get() = "chat:group:invite:accepted"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("groupID"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val groupID = UUID.fromString(request.getString("groupID"))
        if (!GroupDB.has(groupID)) return Error.GROUP_NOT_FOUND.toJson()

        if (!GroupMemberInvitesDB.userInvitedToGroup(groupID, sender)) return Error.USER_NOT_INVITED.toJson()
        GroupMemberInvitesDB.deleteInvitation(groupID, sender)
        val member = GroupMemberData(sender, groupID, GroupRole.MEMBER)
        GroupMemberDB.insert(listOf(member))

        for (groupMember in GroupMemberDB.get(groupID)) {
            UserHandler.sendNotificationIfOnline(groupMember.id, GroupChatUserJoinedNotification(sender, groupID))
        }

        return toJSON(GroupProfileDB.get(groupID))
    }
}

/**
 * Action when a user declines an invitation
 */
class GroupChatDeclineInviteAction: Action {

    override val name: String
        get() = "chat:group:invite:decline"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("groupID"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val groupID = UUID.fromString(request.getString("groupID"))
        if (!GroupDB.has(groupID)) return Error.GROUP_NOT_FOUND.toJson()

        if (!GroupMemberInvitesDB.userInvitedToGroup(groupID, sender)) return Error.USER_NOT_INVITED.toJson()
        GroupMemberInvitesDB.deleteInvitation(groupID, sender)
        val member = GroupMemberData(sender, groupID, GroupRole.MEMBER)
        GroupMemberDB.insert(listOf(member))

        return toJSON(GroupProfileDB.get(groupID))
    }
}

/**
 * Action when a user cancels an invitation
 */
class GroupChatCancelInviteAction: Action {

    override val name: String
        get() = "chat:group:invite:cancel"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("target"), Parameter("groupID"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val targetUUID = UUID.fromString(request.getString("target"))
        val groupID = UUID.fromString(request.getString("groupID"))

        if (!GroupDB.has(groupID)) return Error.GROUP_NOT_FOUND.toJson()

        val role = GroupMemberDB.getGroupMemberFor(groupID, sender)

        if (role.role.weight < GroupRole.MODERATOR.weight) return Error.UNAUTHORIZED.toJson()
        if (!GroupMemberInvitesDB.userInvitedToGroup(groupID, targetUUID)) return Error.USER_NOT_INVITED.toJson()

        GroupMemberInvitesDB.deleteInvitation(groupID, targetUUID)

        for (member in GroupMemberDB.get(groupID)) {
            UserHandler.sendNotificationIfOnline(member.id, GroupChatInvitationCanceledNotification(sender, targetUUID, groupID))
        }

        UserHandler.sendNotificationIfOnline(targetUUID, GroupChatUserCanceledYourInvitationNotification(sender, groupID))

        return Error.NONE.toJson()
    }
}

