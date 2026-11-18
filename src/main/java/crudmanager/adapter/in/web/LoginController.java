package crudmanager.adapter.in.web;

import crudmanager.application.port.in.AuthUseCase;
import crudmanager.domain.error.ModelException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(urlPatterns = {"/login", "/logout"})
public class LoginController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(LoginController.class.getName());
    static final String LOGIN_JSP = "/login.jsp";

    private final AuthUseCase injected;

    public LoginController() {
        this(null);
    }

    LoginController(AuthUseCase auth) {
        this.injected = auth;
    }

    private AuthUseCase auth() {
        return injected != null ? injected : UseCases.instance().auth();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/logout".equals(WebUtil.route(req))) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            WebUtil.redirect(req, resp, "/login");
            return;
        }
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute(WebUtil.SESSION_USER) != null) {
            WebUtil.redirect(req, resp, "/");
            return;
        }
        WebUtil.forward(req, resp, LOGIN_JSP);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/login".equals(WebUtil.route(req))) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        Optional<User> user;
        try {
            user = auth().login(req.getParameter("email"), req.getParameter("password"));
        } catch (ValidationException e) {
            showLogin(req, resp, e.getErrors().getOrDefault("login", "Informe e-mail e senha"));
            return;
        } catch (ModelException e) {
            LOG.log(Level.SEVERE, "Falha de dados em /login", e);
            showLogin(req, resp, WebUtil.DB_ERROR_MESSAGE);
            return;
        }
        if (user.isEmpty()) {
            showLogin(req, resp, "E-mail ou senha inválidos");
            return;
        }
        HttpSession session = req.getSession();
        req.changeSessionId();
        session.setAttribute(WebUtil.SESSION_USER, user.get());
        WebUtil.redirect(req, resp, "/");
    }

    private void showLogin(HttpServletRequest req, HttpServletResponse resp, String message)
            throws ServletException, IOException {
        req.setAttribute(WebUtil.MESSAGE_KEY, message);
        req.setAttribute(WebUtil.ALERT_TYPE_KEY, WebUtil.ALERT_ERROR);
        WebUtil.forward(req, resp, LOGIN_JSP);
    }
}
