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

import de.buelowssiege.mail.pgp_mime.MimeMultipartSigned;
import de.wpsverlinden.cryptolist.entities.MailUtils;
import de.wpsverlinden.cryptolist.entities.MessageQueue;
import de.wpsverlinden.cryptolist.entities.PGPUtils;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

@Singleton
public class MailSigner {

    @Inject
    private MailUtils mailUtils;
    @Inject
    private PGPUtils pgpUtils;
    private MessageQueue inQueue, outQueue;

    public void setInQueue(MessageQueue inQueue) {
        this.inQueue = inQueue;
    }

    public void setOutQueue(MessageQueue outQueue) {
        this.outQueue = outQueue;
    }

    public void run() {
        MimeMessage message;

        Logger.getLogger(MailSigner.class.getName()).log(Level.INFO, "Inbound queue size: {0}", inQueue.size());
        while (null != (message = inQueue.poll())) {
            try {
                Logger.getLogger(MailSigner.class.getName()).log(Level.INFO, "Signing outbound message");
                if (message.getContent() instanceof MimeMultipart) {
                    MimeMultipart mmp = (MimeMultipart) message.getContent();
                    MimeMultipartSigned mms = pgpUtils.sign(mmp);
                    message.setContent(mms);
                    message.saveChanges();
                    outQueue.add(message);
                } else {
                    Logger.getLogger(MailSigner.class.getName()).log(Level.SEVERE, "Unexpected content type: {0}", message.getContent().getClass().getName());
                }
            } catch (IOException | MessagingException ex) {
                Logger.getLogger(MailSigner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Logger.getLogger(MailSigner.class.getName()).log(Level.INFO, "Outbound queue size: {0}", outQueue.size());
    }
}
