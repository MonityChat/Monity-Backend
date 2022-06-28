package de.devin.monity.util

import de.devin.monity.filemanagment.filemanagers.ConfigFileManager
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail


/**
 * Builder to build a default HTML Email with the settings in the config
 * @return HTML Email ready to be sent
 */
fun htmlEmail(): HtmlEmail {
    val email = HtmlEmail()
    email.hostName = ConfigFileManager.getEmailHostName()
    email.isStartTLSEnabled = true
    email.setSmtpPort(ConfigFileManager.getEmailSMTPPort())
    logInfo(email.smtpPort)
    email.setAuthenticator(DefaultAuthenticator(ConfigFileManager.getEmailUser(), ConfigFileManager.getEmailPassword()))
    email.setFrom(ConfigFileManager.getEmailUser())
    email.setDebug(true)
    return email
}