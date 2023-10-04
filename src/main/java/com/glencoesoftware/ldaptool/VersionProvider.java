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

import java.util.Optional;

import ome.services.blitz.Entry;
import picocli.CommandLine.IVersionProvider;

class VersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        String version = Optional.ofNullable(
            this.getClass().getPackage().getImplementationVersion()
        ).orElse("development");
        String blitzVersion = Optional.ofNullable(
            Entry.class.getPackage().getImplementationVersion()
        ).orElse("development");
        return new String[] {
            "${COMMAND-FULL-NAME} version " + version,
            "omero-blitz version " + blitzVersion
        };
    }

}