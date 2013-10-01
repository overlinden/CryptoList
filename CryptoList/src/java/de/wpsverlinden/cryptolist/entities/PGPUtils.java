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

import de.buelowssiege.mail.pgp_mime.BodyPartDecrypter;
import de.buelowssiege.mail.pgp_mime.BodyPartEncrypter;
import de.buelowssiege.mail.pgp_mime.BodyPartSigner;
import de.buelowssiege.mail.pgp_mime.BodyPartVerifier;
import de.buelowssiege.mail.pgp_mime.MimeMultipartEncrypted;
import de.buelowssiege.mail.pgp_mime.MimeMultipartSigned;
import de.buelowssiege.mail.pgp_mime.PGPAuthenticator;
import de.buelowssiege.mail.pgp_mime.PGPMimeException;
import de.buelowssiege.mail.pgp_mime.gpg.GnuPGBodyPartEncrypter;
import de.buelowssiege.mail.pgp_mime.gpg.GnuPGBodyPartSigner;
import de.wpsverlinden.cryptolist.Configuration;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

@Singleton
public class PGPUtils {

    @Inject
    private Configuration config;
    private BodyPartDecrypter decrypter;
    private BodyPartSigner signer;
    private BodyPartVerifier verifier;

    @PostConstruct
    private void initPGP() {
        PGPAuthenticator authenticator = new PGPAuthenticator() {
            @Override
            public String getLocalUser() throws PGPMimeException {
                return config.get("PGPLocalUser");
            }

            @Override
            public boolean useDefaultLocalUser() throws PGPMimeException {
                return false;
            }

            @Override
            public char[] getPassphrase() throws PGPMimeException {
                return config.get("PGPPassphrase").toCharArray();
            }
        };

        decrypter = new GnuPGBodyPartEncrypter(config.get("gpgPath"), authenticator);
        signer = new GnuPGBodyPartSigner(config.get("gpgPath"), authenticator);
        verifier = new GnuPGBodyPartSigner(config.get("gpgPath"), authenticator);
    }

    public BodyPart decrypt(MimeMultipartEncrypted mme) throws MessagingException {
        return mme.decrypt(decrypter);
    }

    public MimeMultipartEncrypted encrypt(MimeMultipart mmp, Address[] recipients) throws MessagingException {
        String[] rec = new String[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            rec[i] = recipients[i].toString();
        }
        return encrypt(mmp, rec);
    }

    public MimeMultipartEncrypted encrypt(MimeMultipart mmp, String[] recipients) throws MessagingException {
        BodyPartEncrypter bpe = new GnuPGBodyPartEncrypter(config.get("gpgPath"), recipients);
        return MimeMultipartEncrypted.createInstance(mmp, bpe);
    }

    public MimeMultipartSigned sign(MimeMultipart mmp) throws MessagingException {
        return MimeMultipartSigned.createInstance(mmp, signer);
    }

    public boolean isValidSignature(MimeMultipartSigned mms) {
        try {
            mms.verify(verifier);
            return true;
        } catch (MessagingException ex) {
            return false;
        }
    }
}
