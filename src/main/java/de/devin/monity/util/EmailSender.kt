package de.devin.monity.util

import filemanagment.filemanagers.ConfigFileManager
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail


/**
 * Builder to build a default HTML Email with the settings in the config
 * @return HTML Email ready to be sent
 */
fun htmlEmail(): HtmlEmail {
    val email = HtmlEmail()
    email.hostName = ConfigFileManager.getEmailHostName()
    email.setSmtpPort(ConfigFileManager.getEmailSMTPPort())
    email.setAuthenticator(DefaultAuthenticator(ConfigFileManager.getEmailUser(), ConfigFileManager.getEmailPassword()))
    email.isSSLOnConnect = true
    email.setFrom(ConfigFileManager.getEmailUser())
    return email
}