package crudmanager.adapter.in.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import crudmanager.domain.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthFilterTest {

    private final AuthFilter filter = new AuthFilter();
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        req = mock(HttpServletRequest.class);
        resp = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        when(req.getContextPath()).thenReturn("/crud-manager");
    }

    @Test
    @DisplayName("AUTH-01 sem sessão, rota protegida redireciona para /login sem seguir a cadeia")
    void redirectsWithoutSession() throws Exception {
        when(req.getServletPath()).thenReturn("/courses");
        when(req.getSession(false)).thenReturn(null);

        filter.doFilter(req, resp, chain);

        verify(resp).sendRedirect("/crud-manager/login");
        verify(chain, never()).doFilter(req, resp);
        verify(req, never()).getSession();
    }

    @Test
    @DisplayName("AUTH-01 a raiz também exige autenticação")
    void rootProtected() throws Exception {
        when(req.getServletPath()).thenReturn("");
        when(req.getSession(false)).thenReturn(null);

        filter.doFilter(req, resp, chain);

        verify(resp).sendRedirect("/crud-manager/login");
        verify(chain, never()).doFilter(req, resp);
    }

    @Test
    @DisplayName("AUTH-01 sessão autenticada segue a cadeia")
    void authenticatedPasses() throws Exception {
        HttpSession session = mock(HttpSession.class);
        when(req.getServletPath()).thenReturn("/courses");
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebUtil.SESSION_USER)).thenReturn(new User(1));

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(req, resp);
        verify(resp, never()).sendRedirect(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("AUTH-09 /login, /css/, /js/, /fonts/ e /images/ passam sem autenticação")
    void publicPathsPass() throws Exception {
        for (String path : new String[] {"/login", "/css/style.css", "/js/jquery.min.js",
                "/fonts/glyphicons-halflings-regular.woff2", "/images/logo.png"}) {
            HttpServletRequest r = mock(HttpServletRequest.class);
            FilterChain c = mock(FilterChain.class);
            when(r.getServletPath()).thenReturn(path);

            filter.doFilter(r, resp, c);

            verify(c).doFilter(r, resp);
        }
        verifyNoInteractions(resp);
    }

    @Test
    @DisplayName("EDGE-4 sessão expirada (existe sem usuário ou já nula) redireciona para /login")
    void expiredSession() throws Exception {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(WebUtil.SESSION_USER)).thenReturn(null);
        when(req.getServletPath()).thenReturn("/users");
        when(req.getSession(false)).thenReturn(session);

        filter.doFilter(req, resp, chain);

        verify(resp).sendRedirect("/crud-manager/login");
        verify(chain, never()).doFilter(req, resp);
    }

    @Test
    @DisplayName("EDGE-5 POST sem sessão redireciona para /login e não alcança o controller")
    void postWithoutSession() throws Exception {
        when(req.getMethod()).thenReturn("POST");
        when(req.getServletPath()).thenReturn("/course/insert");
        when(req.getSession(false)).thenReturn(null);

        filter.doFilter(req, resp, chain);

        verify(resp).sendRedirect("/crud-manager/login");
        verifyNoInteractions(chain);
    }
}
