package crudmanager.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import crudmanager.exception.ModelException;
import crudmanager.exception.ValidationException;
import crudmanager.model.User;
import crudmanager.service.AuthService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class LoginControllerTest {

    private AuthService auth;
    private LoginController controller;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private HttpSession session;
    private RequestDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        auth = mock(AuthService.class);
        controller = new LoginController(auth);
        req = mock(HttpServletRequest.class);
        resp = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);
        when(req.getContextPath()).thenReturn("/crud-manager");
        when(req.getRequestDispatcher("/login.jsp")).thenReturn(dispatcher);
    }

    @Test
    @DisplayName("AUTH-02 GET /login sem sessão exibe login.jsp")
    void showsLoginPage() throws Exception {
        when(req.getServletPath()).thenReturn("/login");
        when(req.getSession(false)).thenReturn(null);

        controller.doGet(req, resp);

        verify(dispatcher).forward(req, resp);
        verify(req, never()).getSession();
    }

    @Test
    @DisplayName("AUTH-03 login correto guarda o usuário na sessão e redireciona para a raiz")
    void loginSuccess() throws Exception {
        User user = new User(1);
        user.setName("Ana");
        when(req.getServletPath()).thenReturn("/login");
        when(req.getParameter("email")).thenReturn("ana@x.com");
        when(req.getParameter("password")).thenReturn("segredo");
        when(auth.login("ana@x.com", "segredo")).thenReturn(Optional.of(user));
        when(req.getSession()).thenReturn(session);

        controller.doPost(req, resp);

        verify(session).setAttribute(WebUtil.SESSION_USER, user);
        verify(resp).sendRedirect("/crud-manager/");
    }

    @Test
    @DisplayName("AUTH-11 login troca o identificador de sessão antes de gravar o usuário")
    void changesSessionId() throws Exception {
        User user = new User(1);
        when(req.getServletPath()).thenReturn("/login");
        when(req.getParameter("email")).thenReturn("ana@x.com");
        when(req.getParameter("password")).thenReturn("segredo");
        when(auth.login("ana@x.com", "segredo")).thenReturn(Optional.of(user));
        when(req.getSession()).thenReturn(session);

        controller.doPost(req, resp);

        InOrder order = inOrder(req, session);
        order.verify(req).changeSessionId();
        order.verify(session).setAttribute(WebUtil.SESSION_USER, user);
    }

    @Test
    @DisplayName("AUTH-04 credencial errada reexibe login com 'E-mail ou senha inválidos' sem criar sessão")
    void wrongCredentials() throws Exception {
        when(req.getServletPath()).thenReturn("/login");
        when(req.getParameter("email")).thenReturn("ana@x.com");
        when(req.getParameter("password")).thenReturn("errada");
        when(auth.login("ana@x.com", "errada")).thenReturn(Optional.empty());

        controller.doPost(req, resp);

        verify(req).setAttribute("message", "E-mail ou senha inválidos");
        verify(dispatcher).forward(req, resp);
        verify(req, never()).getSession();
        verify(req, never()).changeSessionId();
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("DOCK-06 banco indisponível no login reexibe login com a mensagem genérica, sem exceção nem sessão")
    void databaseDown() throws Exception {
        when(req.getServletPath()).thenReturn("/login");
        when(req.getParameter("email")).thenReturn("ana@x.com");
        when(req.getParameter("password")).thenReturn("segredo");
        when(auth.login("ana@x.com", "segredo")).thenThrow(new ModelException("detalhe interno"));

        controller.doPost(req, resp);

        verify(req).setAttribute("message", "Não foi possível concluir a operação. Tente novamente mais tarde.");
        verify(req).setAttribute("alertType", WebUtil.ALERT_ERROR);
        verify(dispatcher).forward(req, resp);
        verify(req, never()).getSession();
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("AUTH-05 campos vazios reexibem login com 'Informe e-mail e senha' sem criar sessão")
    void emptyFields() throws Exception {
        when(req.getServletPath()).thenReturn("/login");
        when(req.getParameter("email")).thenReturn("");
        when(req.getParameter("password")).thenReturn("");
        when(auth.login("", "")).thenThrow(new ValidationException(Map.of("login", "Informe e-mail e senha")));

        controller.doPost(req, resp);

        verify(req).setAttribute("message", "Informe e-mail e senha");
        verify(dispatcher).forward(req, resp);
        verify(req, never()).getSession();
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("AUTH-07 GET /logout invalida a sessão e redireciona para /login")
    void logout() throws Exception {
        when(req.getServletPath()).thenReturn("/logout");
        when(req.getSession(false)).thenReturn(session);

        controller.doGet(req, resp);

        verify(session).invalidate();
        verify(resp).sendRedirect("/crud-manager/login");
    }

    @Test
    @DisplayName("AUTH-07 GET /logout sem sessão apenas redireciona para /login")
    void logoutWithoutSession() throws Exception {
        when(req.getServletPath()).thenReturn("/logout");
        when(req.getSession(false)).thenReturn(null);

        controller.doGet(req, resp);

        verify(resp).sendRedirect("/crud-manager/login");
    }

    @Test
    @DisplayName("AUTH-08 GET /login com usuário autenticado redireciona para a raiz")
    void alreadyLoggedIn() throws Exception {
        when(req.getServletPath()).thenReturn("/login");
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebUtil.SESSION_USER)).thenReturn(new User(1));

        controller.doGet(req, resp);

        verify(resp).sendRedirect("/crud-manager/");
        verify(dispatcher, never()).forward(req, resp);
    }
}
