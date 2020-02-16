/*
 * Copyright (C) 2020 Glencoe Software, Inc. All rights reserved.
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

import java.util.List;

import javax.naming.Name;
import javax.naming.directory.SearchControls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;

public class LoggingLdapTemplate extends LdapTemplate {

    private static final Logger log =
            LoggerFactory.getLogger(LoggingLdapTemplate.class);

    /**
     * Constructor for bean usage.
     */
    public LoggingLdapTemplate() {
    }

    /**
     * Constructor to setup instance directly.
     * 
     * @param contextSource the ContextSource to use.
     */
    public LoggingLdapTemplate(ContextSource contextSource) {
        super();
        setContextSource(contextSource);
    }

    @Override
    public List search(String base, String filter, SearchControls controls,
            ContextMapper mapper) {
        log.info(
            "Base: '{}' Filter: '{}' SearchControls: '{}' ContextMapper: '{}'",
            base, filter, controls, mapper
        );
        return super.search(base, filter, controls, mapper);
    }

    @Override
    public List search(Name base, String filter, ContextMapper mapper) {
        log.info(
            "Name: '{}' Filter: '{}' ContextMapper: '{}'",
            base, filter, mapper
        );
        return super.search(base, filter, mapper);
    }

    @Override
    public Object lookup(Name dn, ContextMapper mapper) {
        log.info("DN: '{}' ContextMapper: '{}'", dn, mapper);
        return super.lookup(dn, mapper);
    }

    @Override
    public List search(String base, String filter, AttributesMapper mapper) {
        log.info(
            "Base: '{}' Filter: '{}' AttributesMapper: '{}'",
            base, filter, mapper
        );
        return super.search(base, filter, mapper);
    }

}
