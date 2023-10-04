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
import ome.system.OmeroContext;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.ScopeType;

/**
 * @author Chris Allan <callan@glencoesoftware.com>
 */
@Command(
    name = "omero-ldaptool",
    mixinStandardHelpOptions = true,
    subcommands = {Password.class, Search.class},
    exitCodeOnExecutionException = 100,
    versionProvider = VersionProvider.class
)
public class Main
{
    private static final Logger log =
            LoggerFactory.getLogger(Main.class);

    @Option(
        names = "--log-level",
        description = "Change logging level; valid values are " +
            "OFF, ERROR, WARN, INFO, DEBUG, TRACE and ALL. " +
            "(default: ${DEFAULT-VALUE})",
        scope = ScopeType.INHERIT
    )
    String logLevel = "WARN";

    @Option(
        names = "--config",
        description = "LDAP configuration properties file",
        required = true,
        scope = ScopeType.INHERIT
    )
    File config;

    // Non-CLI fields
    OmeroContext context;
    LdapImpl ldapImpl;
    LdapTemplate ldapTemplate;

    Main() { }

    public static void main(String[] args)
    {
        Main main = new Main();
        int exitCode = new CommandLine(main)
                .setExecutionStrategy(main::executionStrategy)
                .execute(args);
        System.exit(exitCode);
    }

    private int executionStrategy(ParseResult parseResult) {
        // Check if any subcommands have usage help or version help requested
        AtomicBoolean doInit = new AtomicBoolean(true);
        parseResult.subcommands().forEach(v -> {
            if (v.isUsageHelpRequested() || v.isVersionHelpRequested()) {
                doInit.set(false);
            }
        });

        if (doInit.get()
                && !parseResult.isUsageHelpRequested()
                && !parseResult.isVersionHelpRequested()) {
            init();
        }
        return new CommandLine.RunLast().execute(parseResult);
    }

    public void init() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.toLevel(logLevel));

        log.info("Loading LDAP configuration from: {}",
                config.getAbsolutePath());
        try (FileInputStream v = new FileInputStream(config)) {
            Properties properties = System.getProperties();
            properties.load(v);
            log.info("Properties: {}", properties);
            System.setProperties(properties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        context = new OmeroContext(new String[]{
                "classpath:ome/config.xml",
                "classpath:ome/services/datalayer.xml",
                "classpath*:beanRefContext.xml"});
        ldapImpl = (LdapImpl) context.getBean("internal-ome.api.ILdap");
        ldapTemplate = (LdapTemplate) context.getBean("ldapTemplate");
    }
}
