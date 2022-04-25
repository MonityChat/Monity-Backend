package de.devin.monity.util

import filemanagment.filemanagers.ConfigFileManager
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.Email
import org.apache.commons.mail.HtmlEmail
import org.apache.commons.mail.SimpleEmail
import java.io.File


 fun htmlEmail(): HtmlEmail {
    val email = HtmlEmail()
    email.hostName = ConfigFileManager.getEmailHostName()
    email.setSmtpPort(ConfigFileManager.getEmailSMTPPort())
    email.setAuthenticator(DefaultAuthenticator(ConfigFileManager.getEmailUser(), ConfigFileManager.getEmailPassword()))
    email.isSSLOnConnect = true
    email.setFrom(ConfigFileManager.getEmailUser())
    return email
}