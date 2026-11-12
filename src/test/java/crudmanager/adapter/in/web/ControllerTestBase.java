package crudmanager.adapter.in.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;

/** Mocks de request, response, sessão e dispatcher compartilhados pelos testes de controller. */
abstract class ControllerTestBase {

    protected HttpServletRequest req;
    protected HttpServletResponse resp;
    protected HttpSession session;
    protected RequestDispatcher dispatcher;

    @BeforeEach
    void setUpWeb() {
        req = mock(HttpServletRequest.class);
        resp = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);
        when(req.getContextPath()).thenReturn("/crud-manager");
        when(req.getSession()).thenReturn(session);
        when(req.getSession(false)).thenReturn(session);
    }

    protected void route(String servletPath, Map<String, String> params) {
        when(req.getServletPath()).thenReturn(servletPath);
        params.forEach((k, v) -> when(req.getParameter(k)).thenReturn(v));
    }

    protected void jsp(String path) {
        when(req.getRequestDispatcher(path)).thenReturn(dispatcher);
    }

    protected void verifyFlash(String message, int type) {
        verify(session).setAttribute("message", message);
        verify(session).setAttribute("alertType", type);
    }
}
