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

import java.util.LinkedList;
import java.util.Queue;
import javax.mail.internet.MimeMessage;

public class MessageQueue {

    private Queue<MimeMessage> msgs;

    public MessageQueue() {
        this.msgs = new LinkedList<>();
    }

    public void add(MimeMessage m) {
        msgs.add(m);
    }

    public MimeMessage poll() {
        return msgs.poll();
    }

    public int size() {
        return msgs.size();
    }
}
