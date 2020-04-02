package com.glencoesoftware.ldaptool;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ome.model.internal.Permissions;
import ome.security.SecuritySystem;
import ome.security.auth.SimpleRoleProvider;
import ome.tools.hibernate.SessionFactory;

public class MockSimpleRoleProvider extends SimpleRoleProvider {

    private AtomicLong nextExperimenterGroupId = new AtomicLong(1L);

    private static final Logger log =
            LoggerFactory.getLogger(MockSimpleRoleProvider.class);

    public MockSimpleRoleProvider(SecuritySystem sec, SessionFactory sf) {
        super(sec, sf);
    }

    public MockSimpleRoleProvider(SecuritySystem sec, SessionFactory sf,
            AtomicBoolean ignoreCaseLookup) {
        super(sec, sf, ignoreCaseLookup);
    }

    @Override
    public long createGroup(
            String name, Permissions perms, boolean strict, boolean isLdap) {
        long id = nextExperimenterGroupId.getAndIncrement();
        log.info(
            "Would have created ExperimenterGroup id={} name={} perms={} " +
            "strict={} isLdap={}", name, perms, strict, isLdap
        );
        return id;
    }

}
