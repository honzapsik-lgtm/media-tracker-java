package com.mediatracker.controller;

import com.mediatracker.service.AdminDiagnosticsService;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminDiagnosticsControllerTest {
    AdminDiagnosticsService service = mock(AdminDiagnosticsService.class);
    AdminDiagnosticsController controller;

    @BeforeEach
    void setup() {
        var factory = new ProxyFactory(new AdminDiagnosticsController(service));
        factory.setProxyTargetClass(true);
        factory.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());
        controller = (AdminDiagnosticsController) factory.getProxy();
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void allDiagnosticsDenyNonAdminRolesBeforeDatabaseReads() {
        for (String role : new String[] {"USER", "SYSTEM", "GATEWAY"}) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "test", "unused", AuthorityUtils.createAuthorityList("ROLE_" + role)));
            for (Runnable call : calls()) assertThrows(org.springframework.security.access.AccessDeniedException.class, call::run);
        }
        verifyNoInteractions(service);
    }

    @Test
    void anonymousDeniedAndAdminAllowed() {
        for (Runnable call : calls()) assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class, call::run);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test", "unused", AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        controller.database();
        verify(service).databaseSummary();
    }

    private Runnable[] calls() {
        return new Runnable[] {() -> controller.database(), () -> controller.integrity(),
                () -> controller.logSummary(), () -> controller.logs(Map.of()),
                () -> controller.cacheSummary(), () -> controller.cache(Map.of()),
                () -> controller.performanceSummary(), () -> controller.performance(Map.of())};
    }
}
