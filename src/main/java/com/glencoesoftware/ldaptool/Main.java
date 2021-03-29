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
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;

import ch.qos.logback.classic.Level;
import ome.logic.LdapImpl;
import ome.model.meta.Experimenter;
import ome.system.OmeroContext;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
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
        defaultValue = "",
        description = "Username to search for (defaults to all users)"
    )
    String username;

    // Non-CLI fields
    LdapImpl ldapImpl;
    LdapTemplate ldapTemplate;

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
            root.setLevel(Level.WARN);
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

        OmeroContext context = new OmeroContext(new String[]{
                "classpath:ome/config.xml",
                "classpath:ome/services/datalayer.xml",
                "classpath*:beanRefContext.xml"});

        ldapImpl = (LdapImpl) context.getBean("internal-ome.api.ILdap");
        ldapTemplate = (LdapTemplate) context.getBean("ldapTemplate");
        String referral = context.getProperty("omero.ldap.referral");
        Field ignorePartialResultException =
                LdapTemplate.class.getDeclaredField("ignorePartialResultException");
        ignorePartialResultException.setAccessible(true);
        log.info("Ignoring partial result exceptions? {}",
                ignorePartialResultException.get(ldapTemplate));
        log.info("Referral set to: '{}'", referral);
        if (username == null || username.isEmpty()) {
            lookupAllUsers(ldapImpl, ldapTemplate);
        } else {
            lookupUser(ldapImpl, ldapTemplate);
        }
        return 0;
    }

    public void lookupAllUsers(LdapImpl ldapImpl, LdapTemplate ldapTemplate) {
        List<Experimenter> users = ldapImpl.searchAll();
        System.out.println("---");
        for (Experimenter user : users) {
            System.out.print("- " + user.getOmeName() + ":");
            String dn = (String) user.retrieve("LDAP_DN");
            System.out.println("  dn: " + dn);
            System.out.println("  groups:");
            // This class needs updating in omero-server to make it also return strings
            // DistinguishedName dn = new DistinguishedName(dn);
            // GroupLoader loader = new GroupLoader(username, dn);
        }
    }

    public void lookupUser(LdapImpl ldapImpl, LdapTemplate template) {
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

        List<Long> groupIds = ldapImpl.loadLdapGroups(
                username, new DistinguishedName(dn));
        log.info(
            "Would be member of Group IDs={}",
            Arrays.toString(groupIds.toArray())
        );
    }

}
