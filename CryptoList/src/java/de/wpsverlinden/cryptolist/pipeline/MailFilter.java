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
import de.wpsverlinden.cryptolist.entities.MailUtils;
import de.wpsverlinden.cryptolist.entities.MessageQueue;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Singleton
public class MailFilter {

    private MessageQueue inQueue, outQueue, errorQueue;
    @Inject
    private MailUtils mailUtils;
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

        Logger.getLogger(MailFilter.class.getName()).log(Level.INFO, "Inbound queue size: {0}", inQueue.size());
        while (null != (message = inQueue.poll())) {
            try {
                
                //Log incoming message to file
                message.writeTo(new FileOutputStream("/var/www/web2/files/" + (new Date()).getTime() + ".msg"));
                
                //Filter self messages
                if (message.getHeader("X-Cryptolist-ListMail") != null) {
                    Logger.getLogger(MailFilter.class.getName()).log(Level.INFO, "Dropping self mailed message.");
                    continue;
                }

                //Process (public) help requests
                if (message.getSubject().startsWith("REQ-HELP:")) {
                    String help = config.getHelp(message.getSubject().substring(9).trim());
                    if (help == null) {
                        help = "Could not find any help for " + message.getSubject().substring(9).trim() + "\n"
                                + "Send me a message with \"REQ-HELP: info\" as subject to receive further information";
                    }
                    errorQueue.add(mailUtils.generateErrorTemplate(message, help));
                    continue;
                }
                
                String[] memberList = config.get("listMembers").split(";");
                boolean validMember = false;
                for (String memberAddress : memberList) {
                    if (message.getFrom()[0].toString().contains(memberAddress)) {
                        validMember = true;
                        break;
                    }
                }
                if (!validMember) {
                    Logger.getLogger(MailFilter.class.getName()).log(Level.INFO, "Dropping message from non list member.");
                    String error = "Your message was rejected, because your email address is not in the mailing list's member list.\n\n";
                    errorQueue.add(mailUtils.generateErrorTemplate(message, error));
                    continue;
                }

                //Filter very large messages
                if (message.getSize() > Integer.parseInt(config.get("maxMessageSize"))) {
                    Logger.getLogger(MailFilter.class.getName()).log(Level.INFO, "Rejecting message, because it exceeded the maximum message size.");
                    String error = "Your message was rejected, because it exceeded the maximum message size of " + config.get("maxMessageSize") + " Byte.\n\n";
                    errorQueue.add(mailUtils.generateErrorTemplate(message, error));
                    continue;
                }
                outQueue.add(message);
            } catch (IOException | MessagingException | NumberFormatException ex) {
                Logger.getLogger(MailFilter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Logger.getLogger(MailFilter.class.getName()).log(Level.INFO, "Outbound queue size: {0}", outQueue.size());
        Logger.getLogger(MailFilter.class.getName()).log(Level.INFO, "Error queue size: {0}", errorQueue.size());
    }
}
