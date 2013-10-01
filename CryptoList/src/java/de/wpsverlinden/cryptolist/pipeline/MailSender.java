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

import de.wpsverlinden.cryptolist.entities.MailUtils;
import de.wpsverlinden.cryptolist.entities.MessageQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.*;
import javax.mail.internet.MimeMessage;

@Singleton
public class MailSender {

    @Inject
    private MailUtils mailUtils;
    private MessageQueue inQueue;

    public void setInQueue(MessageQueue inQueue) {
        this.inQueue = inQueue;
    }

    public void run() {

        try {
            Logger.getLogger(MailSender.class.getName()).log(Level.INFO, "Inbound queue size: {0}", inQueue.size());
            Logger.getLogger(MailSender.class.getName()).log(Level.INFO, "Sending {0} outbound messages", inQueue.size());
            MimeMessage message;
            while (null != (message = inQueue.poll())) {
                Transport.send(message);
            }

        } catch (Exception ex) {
            Logger.getLogger(MailSender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
