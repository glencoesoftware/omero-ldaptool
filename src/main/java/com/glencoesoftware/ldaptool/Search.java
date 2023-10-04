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

import ome.logic.LdapImpl;
import ome.logic.LdapImpl.GroupLoader;
import ome.model.meta.Experimenter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.Callable;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * @author Chris Allan <callan@glencoesoftware.com>
 */
@Command(
    name = "search",
    mixinStandardHelpOptions = true,
    exitCodeOnExecutionException = 100,
    versionProvider = VersionProvider.class
)
public class Search implements Callable<Integer>
{
    private static final Logger log =
            LoggerFactory.getLogger(Search.class);

    @ArgGroup(exclusive = true, multiplicity = "1")
    SearchFor searchFor;

    static class SearchFor {
        @Option(names = "--all", description = "Print all users")
        boolean all;

        @Option(names = "--user", description = "Username to search")
        String[] username;
    }

    @ParentCommand
    private Main main;

    Search() { }

    @Override
    public Integer call() throws Exception {
        String referral = main.context.getProperty("omero.ldap.referral");
        Field ignorePartialResultException =
                LdapTemplate.class.getDeclaredField("ignorePartialResultException");
        ignorePartialResultException.setAccessible(true);
        log.info("Ignoring partial result exceptions? {}",
                ignorePartialResultException.get(main.ldapTemplate));
        log.info("Referral set to: '{}'", referral);

        System.out.println("---");
        if (searchFor.all) {
            lookupAllUsers(main.ldapImpl, main.ldapTemplate);
        } else {
            for (String username : searchFor.username) {
                try {
                    Experimenter experimenter =
                            main.ldapImpl.findExperimenter(username);
                    lookupUser(main.ldapImpl, main.ldapTemplate, experimenter);
                } catch (ome.conditions.ApiUsageException api) {
                    System.err.println("no such user: " + username);
                    return 1;
                }
            }
        }
        return 0;
    }

    private GroupLoader newGroupLoader(
            LdapImpl ldapImpl, String username, DistinguishedName dn)
                    throws Exception {
        Class<?> clazz = Class.forName("ome.logic.LdapImpl$GroupLoader");
        Constructor<?> constructor =
            clazz.getDeclaredConstructor(
                    LdapImpl.class, String.class, DistinguishedName.class);
        constructor.setAccessible(true);
        return (GroupLoader) constructor.newInstance(ldapImpl, username, dn);
    }

    private void lookupAllUsers(LdapImpl ldapImpl, LdapTemplate ldapTemplate) throws Exception {
        List<Experimenter> users = ldapImpl.searchAll();
        for (Experimenter user : users) {
            lookupUser(ldapImpl, ldapTemplate, user);
        }
    }

    private void lookupUser(LdapImpl ldapImpl, LdapTemplate template, Experimenter user) throws Exception {
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
