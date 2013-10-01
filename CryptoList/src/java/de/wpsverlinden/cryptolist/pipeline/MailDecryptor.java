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

import de.buelowssiege.mail.pgp_mime.MimeMultipartEncrypted;
import de.wpsverlinden.cryptolist.Configuration;
import de.wpsverlinden.cryptolist.entities.MailUtils;
import de.wpsverlinden.cryptolist.entities.MessageQueue;
import de.wpsverlinden.cryptolist.entities.PGPUtils;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

@Singleton
public class MailDecryptor {

    private MessageQueue inQueue, outQueue, errorQueue;
    @Inject
    private MailUtils mailUtils;
    @Inject
    private PGPUtils pgpUtils;
    @Inject
    private Configuration config;

    public void setInQueue(MessageQueue inQueue) {
        this.inQueue = inQueue;
    }

    public void setOutQueue(MessageQueue outQueue) {
        this.outQueue = outQueue;
    }

    public void setErrorQueue(MessageQueue errorQueue) {
        this.errorQueue = errorQueue;
    }

    public void run() {
        MimeMessage message;

        Logger.getLogger(MailDecryptor.class.getName()).log(Level.INFO, "Inbound queue size: {0}", inQueue.size());
        while (null != (message = inQueue.poll())) {
            try {
                Logger.getLogger(MailDecryptor.class.getName()).log(Level.INFO, "Decrypting inbound message");
                if (message.getContent() instanceof MimeMultipartEncrypted) {
                    BodyPart decryptedBP = pgpUtils.decrypt((MimeMultipartEncrypted) message.getContent());
                    MimeMultipart mmp = new MimeMultipart();
                    mmp.addBodyPart(decryptedBP);
                    message.setContent(mmp);
                    message.saveChanges();
                    outQueue.add(message);
                } else {
                    Logger.getLogger(MailDecryptor.class.getName()).log(Level.INFO, "Unexpected content type: {0}", message.getContent().getClass().getName());
                    String error = "Your message was rejected, because it does not contain a valid PGP MIME content type.\n"
                            + "This mailing list only accepts MIME PGP encrypted and signed messages. Please make\n"
                            + "sure that your messages are correctly formatted as defined in\n"
                            + "\"RFC 3156 MIME Security with OpenPGP\" (http://tools.ietf.org/html/rfc3156) and\n"
                            + "\"RFC 1847 Security Multiparts for MIME: Multipart/Signed and Multipart/Encrypted\" (http://tools.ietf.org/html/rfc1847)";
                    errorQueue.add(mailUtils.generateErrorTemplate(message, error));
                    continue;
                }
            } catch (IOException | MessagingException ex) {
                Logger.getLogger(MailDecryptor.class.getName()).log(Level.SEVERE, null, ex);
                String error = "Your message could not be decrypted with the private key of this mailing list.\n"
                        + "Please verify that you encrypt your message with the correct list's public key.\n"
                        + "Send a message with \"REQ-HELP: getKey\" as subject to receive the key.";
                errorQueue.add(mailUtils.generateErrorTemplate(message, error));
            }
        }
        Logger.getLogger(MailDecryptor.class.getName()).log(Level.INFO, "Outbound queue size: {0}", outQueue.size());
    }
}