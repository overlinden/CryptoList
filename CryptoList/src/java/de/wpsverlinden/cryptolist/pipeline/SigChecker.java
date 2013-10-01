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
import de.wpsverlinden.cryptolist.Configuration;
import de.wpsverlinden.cryptolist.entities.MessageQueue;
import de.wpsverlinden.cryptolist.entities.PGPUtils;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

@Singleton
public class SigChecker {

    private MessageQueue inQueue, outQueue;
    @Inject
    private Configuration config;
    @Inject
    private PGPUtils pgpUtils;

    public void setInQueue(MessageQueue inQueue) {
        this.inQueue = inQueue;
    }

    public void setOutQueue(MessageQueue outQueue) {
        this.outQueue = outQueue;
    }

    public void run() {
        MimeMessage message;

        Logger.getLogger(SigChecker.class.getName()).log(Level.INFO, "Inbound queue size: {0}", inQueue.size());
        while (null != (message = inQueue.poll())) {
            try {
                Logger.getLogger(SigChecker.class.getName()).log(Level.INFO, "Checking inbound message signature");
                if (message.getContent() instanceof MimeMultipartSigned) {
                    MimeMultipartSigned mms = (MimeMultipartSigned) message.getContent();
                    verifySignatureOf(message, mms);
                    Multipart mp = removeSenderSignature(mms);
                    message.setContent(mp);
                    message.saveChanges();
                } else if (message.getContent() instanceof MimeMultipart) {
                    MimeMultipart mmp = (MimeMultipart) message.getContent();
                    if (mmp.getCount() == 1 && mmp.getBodyPart(0).getContent() instanceof MimeMultipartSigned) {
                        MimeMultipartSigned mms = (MimeMultipartSigned) mmp.getBodyPart(0).getContent();
                        verifySignatureOf(message, mms);
                        Multipart mp = removeSenderSignature(mms);
                        message.setContent(mp);
                        message.saveChanges();
                    } else {
                        message.setHeader("X-Cryptolist-SenderSignature", "NotFound");
                        Logger.getLogger(SigChecker.class.getName()).log(Level.INFO, "Unexpected content type: {0}", message.getContent().getClass().getName());
                    }

                } else {
                    message.setHeader("X-Cryptolist-SenderSignature", "NotFound");
                    Logger.getLogger(SigChecker.class.getName()).log(Level.INFO, "Unexpected content type: {0}", message.getContent().getClass().getName());
                }
                outQueue.add(message);
            } catch (IOException | MessagingException ex) {
                Logger.getLogger(SigChecker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Logger.getLogger(SigChecker.class.getName()).log(Level.INFO, "Outbound queue size: {0}", outQueue.size());
    }

    private void verifySignatureOf(MimeMessage message, MimeMultipartSigned mms) throws MessagingException {
        if (pgpUtils.isValidSignature(mms)) {
            message.setHeader("X-Cryptolist-SenderSignature", "Valid");
        } else {
            message.setHeader("X-Cryptolist-SenderSignature", "Invalid");
        }
    }

    private Multipart removeSenderSignature(MimeMultipartSigned mms) throws MessagingException {
        Multipart mp = new MimeMultipart();
        for (int i = 0; i < mms.getCount(); i++) {
            BodyPart bp = mms.getBodyPart(i);
            if (!bp.isMimeType("application/pgp-signature")) {
                mp.addBodyPart(bp);
            }
        }
        return mp;
    }
}
