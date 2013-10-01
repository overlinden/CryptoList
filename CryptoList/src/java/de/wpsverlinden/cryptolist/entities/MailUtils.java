/**
 *
 * Cryptolist - A GnuPG encrypted mailing list Copyright (C) 2013 Oliver Verlinden (http://wps-verlinden.de)
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses/>.
 */
package de.wpsverlinden.cryptolist.entities;

import de.wpsverlinden.cryptolist.Configuration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@Singleton
public class MailUtils {

    private Session session;
    @Inject
    private Configuration config;

    @PostConstruct
    private void initMailing() {

        session = Session.getInstance(config.getMail(), new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getMail("mail.pop3.user"), config.getMail(("mail.pop3.password")));
            }
        });
    }

    public Session getSession() {
        return session;
    }

    public MimeMessage generateErrorTemplate(MimeMessage message, String text) {
        MimeMessage msg = null;
        try {
            msg = (MimeMessage) message.reply(false);
            msg.setFrom(new InternetAddress(config.get("listName") + "<" + config.get("listAddress") + ">"));
            msg.setContent(text + config.get("listFooter"), "Text/Plain");
            msg.saveChanges();
        } catch (MessagingException ex) {
            Logger.getLogger(MailUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return msg;
    }
}
