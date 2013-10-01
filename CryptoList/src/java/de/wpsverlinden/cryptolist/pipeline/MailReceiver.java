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
public class MailReceiver {

    @Inject
    private MailUtils mailSession;
    private MessageQueue outQueue;

    public void setOutQueue(MessageQueue queue) {
        this.outQueue = queue;
    }

    public void run() {
        try {
            Logger.getLogger(MailReceiver.class.getName()).log(Level.INFO, "Receiving new messages from mailbox");
            Store store = mailSession.getSession().getStore("pop3");
            store.connect();
            Folder folder = store.getFolder("Inbox");
            folder.open(Folder.READ_WRITE);

            Message[] msgs = folder.getMessages();

            for (Message message : msgs) {
                MimeMessage m = new MimeMessage((MimeMessage) message);
                outQueue.add(m);
                message.setFlag(Flags.Flag.DELETED, true);
            }
            folder.close(true);
            store.close();

        } catch (MessagingException ex) {
            Logger.getLogger(MailReceiver.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(MailReceiver.class.getName()).log(Level.INFO, "Outbound queue size: {0}", outQueue.size());
    }
}
