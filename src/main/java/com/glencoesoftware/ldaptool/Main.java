/*
 * Copyright (C) 2019 Glencoe Software, Inc. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.glencoesoftware.ldaptool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.LdapTemplate;

import ch.qos.logback.classic.Level;
import ome.logic.LdapImpl;
import ome.model.meta.Experimenter;
import ome.system.OmeroContext;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * @author Chris Allan <callan@glencoesoftware.com>
 */
public class Main implements Callable<Integer>
{
    private static final Logger log =
            LoggerFactory.getLogger(Main.class);

    @Option(
        names = "--help",
        usageHelp = true,
        description = "Display this help and exit"
    )
    boolean help;

    @Option(names = "--debug", description = "Set logging level to DEBUG")
    boolean debug;

    @Parameters(
        index = "0",
        description = "LDAP configuration properties file"
    )
    File config;

    @Parameters(
        index = "1",
        description = "Username to search for"
    )
    String username;

    Main()
    {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (debug)
        {
            root.setLevel(Level.DEBUG);
        }
        else
        {
            root.setLevel(Level.INFO);
        }
    }

    public static void main(String[] args)
    {
        Integer returnCode;
        try {
            returnCode = CommandLine.call(new Main(), args);
        } catch (Exception e) {
            log.error("Error when calling command", e);
            returnCode = 1;
        }
        System.exit(returnCode == null? 100 : returnCode);
    }

    @Override
    public Integer call() throws Exception {
        log.info("Loading LDAP configuration from: {}",
                 config.getAbsolutePath());
        try (FileInputStream v = new FileInputStream(config)) {
            Properties properties = System.getProperties();
            properties.load(v);
            log.info("Properties: {}", properties);
            System.setProperties(properties);
        }

        OmeroContext context = new OmeroContext(new String [] {
                "classpath:ome/config.xml",
                "classpath:ome/services/datalayer.xml",
                "classpath*:beanRefContext.xml"});

        LdapImpl ldapImpl =
                (LdapImpl) context.getBean("internal-ome.api.ILdap");
        LdapTemplate ldapTemplate =
                (LdapTemplate) context.getBean("ldapTemplate");
        String referral = context.getProperty("omero.ldap.referral");
        Field ignorePartialResultException =
            LdapTemplate.class.getDeclaredField("ignorePartialResultException");
        ignorePartialResultException.setAccessible(true);
        log.info("Ignoring partial result exceptions? {}",
                ignorePartialResultException.get(ldapTemplate));
        log.info("Referral set to: '{}'", referral);
        String dn = ldapImpl.findDN(username);
        log.info("Found DN: {}", dn);
        Experimenter experimenter = ldapImpl.findExperimenter(username);
        log.info(
            "Experimenter field mappings id={} email={} firstName={} " +
            "lastName={} institution={} ldap={} middleName={} omeName={}",
            experimenter.getId(), experimenter.getEmail(),
            experimenter.getFirstName(), experimenter.getLastName(),
            experimenter.getInstitution(), experimenter.getLdap(),
            experimenter.getMiddleName(), experimenter.getOmeName()
        );

        return 0;
    }

}
