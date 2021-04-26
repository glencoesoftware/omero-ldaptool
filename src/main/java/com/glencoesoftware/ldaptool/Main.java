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
import ome.logic.LdapImpl.GroupLoader;
import ome.model.meta.Experimenter;
import ome.system.OmeroContext;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
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

    @ArgGroup(exclusive = true, multiplicity = "1")
    SearchFor searchFor;

    static class SearchFor {
        @Option(names = "--all", description = "Print all users")
        boolean all;

        @Option(names = "--user", description = "Username to search")
        String username;
    }

    @Parameters(
        index = "0",
        description = "LDAP configuration properties file"
    )
    File config;

    // Non-CLI fields
    LdapImpl ldapImpl;
    LdapTemplate ldapTemplate;

    Main() { }

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

    public GroupLoader newGroupLoader(
            LdapImpl ldapImpl, String username, DistinguishedName dn)
                    throws Exception {
        Class<?> clazz = Class.forName("ome.logic.LdapImpl$GroupLoader");
        Constructor<?> constructor =
            clazz.getDeclaredConstructor(
                    LdapImpl.class, String.class, DistinguishedName.class);
        constructor.setAccessible(true);
        return (GroupLoader) constructor.newInstance(ldapImpl, username, dn);
    }

    @Override
    public Integer call() throws Exception {
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

        System.out.println("---");
        if (searchFor.all) {
            lookupAllUsers(ldapImpl, ldapTemplate);
        } else {
            try {
                Experimenter experimenter = ldapImpl.findExperimenter(searchFor.username);
                lookupUser(ldapImpl, ldapTemplate, experimenter);
            } catch (ome.conditions.ApiUsageException api) {
                System.err.println("no such user: " + searchFor.username);
                return 1;
            }
        }
        return 0;
    }

    public void lookupAllUsers(LdapImpl ldapImpl, LdapTemplate ldapTemplate) throws Exception {
        List<Experimenter> users = ldapImpl.searchAll();
        for (Experimenter user : users) {
            lookupUser(ldapImpl, ldapTemplate, user);
        }
    }

    public void lookupUser(LdapImpl ldapImpl, LdapTemplate template, Experimenter user) throws Exception {

        String dn = (String) user.retrieve("LDAP_DN");

        // This class needs updating in omero-server to make it also return strings
        GroupLoader groupLoader = newGroupLoader(
                ldapImpl, user.getOmeName(), new DistinguishedName(dn));
        Field groups = LdapImpl.GroupLoader.class.getDeclaredField("groups");
        groups.setAccessible(true);
        printString("- dn", dn);
        printString("  omeName", user.getOmeName());
        printString("  firstName", user.getFirstName());
        printString("  middleName", user.getMiddleName());
        printString("  lastName", user.getLastName());
        printString("  email", user.getEmail());
        printString("  institution", user.getInstitution());
        printGroup("owner", groupLoader.getOwnedGroups());
        printGroup("member", (List<Long>) groups.get(groupLoader));

    }

    private void printString(String key, String value) {
        if (value == null) {
            return;
        }
        value = '"' + value + '"';
        System.out.println(String.format("%s: %s", key, value));
    }

    private void printGroup(String key, List<Long> groups) {
        if (groups == null || groups.size() == 0) {
            return;
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (Long id : groups) {
            joiner.add(id.toString());
        }
        System.out.println(String.format("  %s: [%s]", key, joiner.toString()));
    }

}
