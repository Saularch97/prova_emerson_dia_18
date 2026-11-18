package crudmanager.adapter.in.web;

import crudmanager.application.port.in.UserUseCase;
import crudmanager.application.service.Pagination;
import crudmanager.domain.Page;
import crudmanager.domain.error.ModelException;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.RelatedDataException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(urlPatterns = {"/users", "/user/form", "/user/insert", "/user/update", "/user/delete"})
public class UsersController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(UsersController.class.getName());
    static final String LIST_JSP = "/users.jsp";
    static final String FORM_JSP = "/form-user.jsp";
    private static final String LIST_URL = "/users";
    private static final String NOT_FOUND = "Usuário não encontrado.";

    private final UserUseCase injected;

    public UsersController() {
        this(null);
    }

    UsersController(UserUseCase users) {
        this.injected = users;
    }

    private UserUseCase users() {
        return injected != null ? injected : UseCases.instance().users();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (WebUtil.route(req)) {
                case "/users":
                    list(req, resp);
                    break;
                case "/user/form":
                    req.setAttribute("action", "insert");
                    WebUtil.forward(req, resp, FORM_JSP);
                    break;
                case "/user/update":
                    req.setAttribute("user", users().find(WebUtil.parseId(req.getParameter("userId"))));
                    req.setAttribute("action", "update");
                    WebUtil.forward(req, resp, FORM_JSP);
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (NotFoundException e) {
            WebUtil.error(req, NOT_FOUND);
            WebUtil.redirect(req, resp, LIST_URL);
        } catch (ModelException e) {
            LOG.log(Level.SEVERE, "Falha de dados em " + WebUtil.route(req), e);
            WebUtil.error(req, WebUtil.DB_ERROR_MESSAGE);
            WebUtil.redirect(req, resp, LIST_URL);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String route = WebUtil.route(req);
        String redirectTo = LIST_URL;
        try {
            switch (route) {
                case "/user/insert":
                    save(req, resp, false);
                    return;
                case "/user/update":
                    save(req, resp, true);
                    return;
                case "/user/delete":
                    redirectTo = LIST_URL + "?page=" + WebUtil.parsePage(req);
                    delete(req);
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
            }
        } catch (NotFoundException e) {
            WebUtil.error(req, NOT_FOUND);
        } catch (RelatedDataException e) {
            WebUtil.error(req, e.getMessage());
        } catch (ModelException e) {
            LOG.log(Level.SEVERE, "Falha de dados em " + route, e);
            WebUtil.error(req, WebUtil.DB_ERROR_MESSAGE);
        }
        WebUtil.redirect(req, resp, redirectTo);
    }

    private void list(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        WebUtil.transferFlash(req);
        Page<User> page;
        try {
            page = users().list(WebUtil.parsePage(req));
        } catch (ModelException e) {
            LOG.log(Level.SEVERE, "Falha ao listar usuários", e);
            req.setAttribute(WebUtil.MESSAGE_KEY, WebUtil.DB_ERROR_MESSAGE);
            req.setAttribute(WebUtil.ALERT_TYPE_KEY, WebUtil.ALERT_ERROR);
            page = Page.of(List.of(), 1, Pagination.PAGE_SIZE, 0);
        }
        WebUtil.pageData(req, page, LIST_URL);
        WebUtil.forward(req, resp, LIST_JSP);
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, boolean update)
            throws ServletException, IOException {
        User user = new User(update ? WebUtil.parseId(req.getParameter("userId")) : 0);
        user.setName(req.getParameter("name"));
        user.setGender(req.getParameter("gender"));
        user.setEmail(req.getParameter("email"));
        String password = req.getParameter("password");
        try {
            if (update) {
                users().update(user, password);
            } else {
                users().create(user, password);
            }
        } catch (ValidationException e) {
            req.setAttribute("user", user);
            req.setAttribute("errors", e.getErrors());
            req.setAttribute("action", update ? "update" : "insert");
            WebUtil.forward(req, resp, FORM_JSP);
            return;
        } catch (NotFoundException e) {
            WebUtil.error(req, NOT_FOUND);
            WebUtil.redirect(req, resp, LIST_URL);
            return;
        } catch (ModelException e) {
            LOG.log(Level.SEVERE, "Falha ao salvar usuário", e);
            WebUtil.error(req, WebUtil.DB_ERROR_MESSAGE);
            WebUtil.redirect(req, resp, LIST_URL);
            return;
        }
        WebUtil.success(req, "Usuário '" + user.getName() + "' " + (update ? "atualizado" : "salvo") + " com sucesso.");
        WebUtil.redirect(req, resp, LIST_URL);
    }

    private void delete(HttpServletRequest req) {
        User user = users().find(WebUtil.parseId(req.getParameter("id")));
        try {
            users().delete(user.getId());
        } catch (RelatedDataException e) {
            throw new RelatedDataException("Usuário '" + user.getName()
                    + "' não pode ser deletado. Há dados relacionados ao usuário.");
        }
        WebUtil.success(req, "Usuário '" + user.getName() + "' deletado com sucesso.");
    }
}
