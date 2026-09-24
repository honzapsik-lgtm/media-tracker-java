package com.mediatracker.controller;

import com.mediatracker.service.AdminMediaActionsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminMediaActionsControllerTest {
    private final AdminMediaActionsService service = mock(AdminMediaActionsService.class);
    private final AdminMediaActionsController controller = new AdminMediaActionsController(service);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test
    void clearCacheReturnsActualDeletedCount() throws Exception {
        when(service.clearCache("tmdb-tv-12")).thenReturn(4);
        mvc.perform(post("/api/admin/media/tmdb-tv-12/actions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"clear-cache\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedCount").value(4));
    }

    @Test
    void refreshStatsExecutesMaintenance() throws Exception {
        mvc.perform(post("/api/admin/media/igdb-game-12/actions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"refresh-stats\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(service).refreshStats("igdb-game-12");
    }

    @Test
    void malformedRequestsDoNotMutateData() throws Exception {
        for (String body : new String[] { "{}", "{\"action\":\"other\"}", "null", "not json" }) {
            mvc.perform(post("/api/admin/media/tmdb-tv-12/actions").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/admin/media/tmdb-tv/actions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"clear-cache\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    private AdminMediaActionsController secured() {
        var factory = new ProxyFactory(controller);
        factory.setProxyTargetClass(true);
        factory.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());
        return (AdminMediaActionsController) factory.getProxy();
    }

    @Test
    void nonAdminsCannotInvokeEitherAction() {
        var guarded = secured();
        for (String role : new String[] { "USER", "GATEWAY", "SYSTEM" }) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "test", "unused", AuthorityUtils.createAuthorityList("ROLE_" + role)));
            for (String action : new String[] { "clear-cache", "refresh-stats" }) {
                assertThrows(org.springframework.security.access.AccessDeniedException.class,
                        () -> guarded.act("tmdb-tv-12", new AdminMediaActionsController.ActionRequest(action)));
            }
        }
        SecurityContextHolder.clearContext();
        assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> guarded.act("tmdb-tv-12", new AdminMediaActionsController.ActionRequest("clear-cache")));
        verifyNoInteractions(service);
    }

    @Test
    void adminCanInvokeMaintenance() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test", "unused", AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        assertEquals(200, secured().act("tmdb-tv-12",
                new AdminMediaActionsController.ActionRequest("refresh-stats")).getStatusCode().value());
        verify(service).refreshStats("tmdb-tv-12");
    }
}
