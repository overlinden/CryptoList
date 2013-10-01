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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

@Singleton
public class MailProcessor {

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

        Logger.getLogger(MailProcessor.class.getName()).log(Level.INFO, "Inbound queue size: {0}", inQueue.size());
        while (null != (message = inQueue.poll())) {
            try {
                appendSignatureFooter(message);
                appendListFooter(message);
                updateHeaders(message);
                message.saveChanges();
                outQueue.add(message);
            } catch (Exception ex) {
                Logger.getLogger(MailProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Logger.getLogger(MailProcessor.class.getName()).log(Level.INFO, "Outbound queue size: {0}", outQueue.size());
    }

    private void updateHeaders(MimeMessage message) throws MessagingException {
        message.addHeader("X-Cryptolist-ListMail", "true");
        message.setFrom(new InternetAddress(config.get("listName") + "<" + config.get("listAddress") + ">"));
        message.setRecipients(Message.RecipientType.CC, new Address[0]);
    }

    private void appendSignatureFooter(MimeMessage message) {
        try {
            String sigText = "\n\n________________________________________\n"
                                + "Original sender: \"" + message.getFrom()[0].toString() + "\"\n";
            String[] signatureHeader = message.getHeader("X-Cryptolist-SenderSignature");
            if (signatureHeader != null && signatureHeader.length == 1) {
                String sigHeader = signatureHeader[0];
                switch (sigHeader) {
                    case "Valid":
                        sigText += "PGP signature check successfull. The message integrity is fine.";
                        break;
                    case "Invalid":
                        sigText += "Warning: PGP signature check not successfull. The message integrity can not be guaranteed.";
                        break;
                    case "NotFound":
                    default:
                        sigText += "Warning: Could not find PGP signature. The message integrity can not be guaranteed.";
                        break;
                }
                if (message.getContent() instanceof MimeMultipart) {
                    MimeMultipart mbp = (MimeMultipart) message.getContent();
                    MimeBodyPart bp = new MimeBodyPart();
                    bp.setContent(sigText, "Text/Plain");
                    mbp.addBodyPart(bp);
                } else {
                    Logger.getLogger(MailProcessor.class.getName()).log(Level.SEVERE, "Unexpected content type: {0}", message.getContent().getClass().getName());
                }
            } else {
                Logger.getLogger(MailProcessor.class.getName()).log(Level.SEVERE, "Invalid SenderSignature header found");
            }
        } catch (IOException | MessagingException ex) {
            Logger.getLogger(MailSender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void appendListFooter(MimeMessage message) {
        try {
            if (message.getContent() instanceof MimeMultipart) {
                MimeMultipart mbp = (MimeMultipart) message.getContent();
                MimeBodyPart bp = new MimeBodyPart();
                bp.setContent(config.get("listFooter"), "Text/Plain");
                mbp.addBodyPart(bp);
            } else {
                Logger.getLogger(MailProcessor.class.getName()).log(Level.SEVERE, "Unexpected content type: {0}", message.getContent().getClass().getName());
            }
        } catch (IOException | MessagingException ex) {
            Logger.getLogger(MailSender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
