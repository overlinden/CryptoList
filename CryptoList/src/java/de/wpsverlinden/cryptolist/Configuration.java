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
package de.wpsverlinden.cryptolist;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import org.apache.log4j.PropertyConfigurator;

@Singleton
public class Configuration {

    private Properties config;
    private Properties help;
    private Properties mail;

    @PostConstruct
    private void loadConfig() {
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("cryptolist.conf")) {
            config = new Properties();
            config.load(in);
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(this.get("helpTextFile"))) {
            help = new Properties();
            help.load(in);
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(this.get("mailConfigFile"))) {
            mail = new Properties();
            mail.load(in);
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        PropertyConfigurator.configure(this.getClass().getClassLoader().getResource("log4j.conf"));
    }

    public String get(String key) {
        return config.getProperty(key);
    }

    public String getHelp(String key) {
        return help.getProperty(key);
    }

    public String getMail(String key) {
        return mail.getProperty(key);
    }

    public Properties getMail() {
        return mail;
    }
}
