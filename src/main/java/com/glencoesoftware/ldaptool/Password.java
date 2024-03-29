/*
 * Copyright (C) 2023 Glencoe Software, Inc. All rights reserved.
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

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * @author Chris Allan <callan@glencoesoftware.com>
 */
@Command(
    name = "password",
    mixinStandardHelpOptions = true,
    exitCodeOnExecutionException = 100,
    versionProvider = VersionProvider.class
)
public class Password implements Callable<Integer>
{
    private static final Logger log =
            LoggerFactory.getLogger(Password.class);

    @Option(
        names = {"--tries"},
        description =
            "Number of times to retry the password check " +
            "(default: ${DEFAULT-VALUE})"
    )
    int tries = 1;

    @Parameters(
        index = "0",
        description = "DN to check password for"
    )
    String dn;

    @ParentCommand
    private Main main;

    Password() { }

    @Override
    public Integer call() throws Exception {
        String passphrase = String.valueOf(System.console().readPassword(
                "Enter passphrase for '" + dn + "': "));
        int exitCode = 0;
        for (int i = 0; i < tries; i++) {
            if (main.ldapImpl.validatePassword(dn, passphrase)) {
                System.out.println("Password check successful!");
            } else {
                System.out.println("Password check failed!");
                exitCode = 1;
            }
        }
        return exitCode;
    }

}
