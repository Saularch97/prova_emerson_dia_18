package crudmanager.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import crudmanager.domain.Page;
import crudmanager.domain.error.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WebUtilTest {

    private final Map<String, Object> sessionAttrs = new HashMap<>();
    private final Map<String, Object> requestAttrs = new HashMap<>();
    private HttpServletRequest req;

    @BeforeEach
    void setUp() {
        req = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(req.getSession()).thenReturn(session);
        when(req.getSession(false)).thenReturn(session);
        Mockito.doAnswer(i -> sessionAttrs.put(i.getArgument(0), i.getArgument(1)))
                .when(session).setAttribute(Mockito.anyString(), Mockito.any());
        when(session.getAttribute(Mockito.anyString())).thenAnswer(i -> sessionAttrs.get(i.<String>getArgument(0)));
        Mockito.doAnswer(i -> sessionAttrs.remove(i.<String>getArgument(0)))
                .when(session).removeAttribute(Mockito.anyString());
        Mockito.doAnswer(i -> requestAttrs.put(i.getArgument(0), i.getArgument(1)))
                .when(req).setAttribute(Mockito.anyString(), Mockito.any());
    }

    @Test
    @DisplayName("EDGE-2 id não numérico vira NotFoundException")
    void nonNumericId() {
        assertThrows(NotFoundException.class, () -> WebUtil.parseId("abc"));
    }

    @Test
    @DisplayName("EDGE-2 id vazio ou ausente vira NotFoundException")
    void emptyId() {
        assertThrows(NotFoundException.class, () -> WebUtil.parseId(""));
        assertThrows(NotFoundException.class, () -> WebUtil.parseId("  "));
        assertThrows(NotFoundException.class, () -> WebUtil.parseId(null));
    }

    @Test
    @DisplayName("EDGE-2 id numérico é convertido")
    void validId() {
        assertEquals(7, WebUtil.parseId("7"));
    }

    @Test
    @DisplayName("EDGE-2 flash de sucesso passa da sessão para a request uma única vez")
    void flashOnce() {
        WebUtil.success(req, "Salvo");
        WebUtil.transferFlash(req);

        assertEquals("Salvo", requestAttrs.get("message"));
        assertEquals(1, requestAttrs.get("alertType"));
        assertNull(sessionAttrs.get("message"));
        assertNull(sessionAttrs.get("alertType"));

        requestAttrs.clear();
        WebUtil.transferFlash(req);
        assertNull(requestAttrs.get("message"));
    }

    @Test
    @DisplayName("EDGE-2 flash de erro usa alertType 0")
    void errorFlash() {
        WebUtil.error(req, "Falhou");
        WebUtil.transferFlash(req);

        assertEquals("Falhou", requestAttrs.get("message"));
        assertEquals(0, requestAttrs.get("alertType"));
    }

    @Test
    @DisplayName("EDGE-2 transferFlash sem sessão não cria sessão")
    void noSession() {
        when(req.getSession(false)).thenReturn(null);
        WebUtil.transferFlash(req);
        Mockito.verify(req, Mockito.never()).getSession();
    }

    @Test
    @DisplayName("PAG-01 parsePage usa Pagination.parse e pageData expõe pageData e pageBaseUrl")
    void pageHelpers() {
        when(req.getParameter("page")).thenReturn("3");
        assertEquals(3, WebUtil.parsePage(req));
        when(req.getParameter("page")).thenReturn("xyz");
        assertEquals(1, WebUtil.parsePage(req));

        Page<String> page = Page.of(List.of("a"), 1, 10, 1);
        WebUtil.pageData(req, page, "/courses");
        assertSame(page, requestAttrs.get("pageData"));
        assertEquals("/courses", requestAttrs.get("pageBaseUrl"));
    }

    @Test
    @DisplayName("NAV-01 route devolve o servletPath")
    void route() {
        when(req.getServletPath()).thenReturn("/courses");
        assertEquals("/courses", WebUtil.route(req));
    }
}
