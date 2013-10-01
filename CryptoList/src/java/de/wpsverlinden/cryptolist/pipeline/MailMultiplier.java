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
package de.wpsverlinden.cryptolist.pipeline;

import de.wpsverlinden.cryptolist.Configuration;
import de.wpsverlinden.cryptolist.entities.MessageQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@Singleton
public class MailMultiplier {

    private MessageQueue inQueue, outQueue;
    @Inject
    private Configuration config;

    public void setInQueue(MessageQueue inQueue) {
        this.inQueue = inQueue;
    }

    public void setOutQueue(MessageQueue outQueue) {
        this.outQueue = outQueue;
    }

    public void run() {
        MimeMessage message;

        Logger.getLogger(MailMultiplier.class.getName()).log(Level.INFO, "Inbound queue size: {0}", inQueue.size());
        while (null != (message = inQueue.poll())) {
            String[] recipients = config.get("listMembers").split(";");
            Logger.getLogger(MailMultiplier.class.getName()).log(Level.INFO, "Multiplying message for {0} recipients.", recipients.length);
            for (String m : recipients) {
                MimeMessage copy;
                try {
                    copy = new MimeMessage(message);
                    copy.setRecipient(Message.RecipientType.TO, new InternetAddress(m));
                    outQueue.add(copy);
                } catch (Exception ex) {
                    Logger.getLogger(MailMultiplier.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        Logger.getLogger(MailMultiplier.class.getName()).log(Level.INFO, "Outbound queue size: {0}", outQueue.size());
    }
}
