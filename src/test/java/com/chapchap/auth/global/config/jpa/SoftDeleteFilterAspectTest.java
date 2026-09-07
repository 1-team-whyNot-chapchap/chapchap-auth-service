package com.chapchap.auth.global.config.jpa;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.Mockito.*;

class SoftDeleteFilterAspectTest {
    @Test
    void undefinedFilterDoesNotBreakEveryController() {
        Session session = sessionWithFilters(Set.of());
        verify(session, never()).enableFilter(anyString());
    }

    @Test
    void definedFilterStillApplies() {
        Session session = sessionWithFilters(Set.of("softDelete"));
        verify(session).enableFilter("softDelete");
    }

    private Session sessionWithFilters(Set<String> names) {
        EntityManager entityManager = mock(EntityManager.class);
        Session session = mock(Session.class);
        SessionFactory factory = mock(SessionFactory.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.getSessionFactory()).thenReturn(factory);
        when(factory.getDefinedFilterNames()).thenReturn(names);
        new SoftDeleteFilterAspect(entityManager).endableSoftDeleteFilter();
        return session;
    }
}
