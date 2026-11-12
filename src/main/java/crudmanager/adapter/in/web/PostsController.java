package crudmanager.adapter.in.web;

import crudmanager.application.port.in.PostUseCase;
import crudmanager.application.port.in.UserUseCase;
import crudmanager.application.service.Pagination;
import crudmanager.domain.Page;
import crudmanager.domain.error.ModelException;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Post;
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

/** Posts; a raiz do contexto ("") é a home e lista os posts. */
@WebServlet(urlPatterns = {"", "/posts", "/post/form", "/post/insert", "/post/update", "/post/delete"})
public class PostsController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(PostsController.class.getName());
    static final String LIST_JSP = "/index.jsp";
    static final String FORM_JSP = "/form-post.jsp";
    private static final String LIST_URL = "/posts";
    private static final String NOT_FOUND = "Post não encontrado.";

    private final transient PostUseCase injectedPosts;
    private final transient UserUseCase injectedUsers;

    public PostsController() {
        this(null, null);
    }

    PostsController(PostUseCase posts, UserUseCase users) {
        this.injectedPosts = posts;
        this.injectedUsers = users;
    }

    private PostUseCase posts() {
        return injectedPosts != null ? injectedPosts : UseCases.instance().posts();
    }

    private UserUseCase users() {
        return injectedUsers != null ? injectedUsers : UseCases.instance().users();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (WebUtil.route(req)) {
                case "":
                case "/posts":
                    list(req, resp);
                    break;
                case "/post/form":
                    showForm(req, resp, null, "insert");
                    break;
                case "/post/update":
                    showForm(req, resp, posts().find(WebUtil.parseId(req.getParameter("postId"))), "update");
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
                case "/post/insert":
                    if (!save(req, resp, false)) {
                        return;
                    }
                    break;
                case "/post/update":
                    if (!save(req, resp, true)) {
                        return;
                    }
                    break;
                case "/post/delete":
                    redirectTo = LIST_URL + "?page=" + WebUtil.parsePage(req);
                    delete(req);
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
            }
        } catch (NotFoundException e) {
            WebUtil.error(req, NOT_FOUND);
        } catch (ModelException e) {
            LOG.log(Level.SEVERE, "Falha de dados em " + route, e);
            WebUtil.error(req, WebUtil.DB_ERROR_MESSAGE);
        }
        WebUtil.redirect(req, resp, redirectTo);
    }

    private void list(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        WebUtil.transferFlash(req);
        Page<Post> page;
        try {
            page = posts().list(WebUtil.parsePage(req));
        } catch (ModelException e) {
            LOG.log(Level.SEVERE, "Falha ao listar posts", e);
            req.setAttribute(WebUtil.MESSAGE_KEY, WebUtil.DB_ERROR_MESSAGE);
            req.setAttribute(WebUtil.ALERT_TYPE_KEY, WebUtil.ALERT_ERROR);
            page = Page.of(List.of(), 1, Pagination.PAGE_SIZE, 0);
        }
        WebUtil.pageData(req, page, LIST_URL);
        WebUtil.forward(req, resp, LIST_JSP);
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Post post, String action)
            throws ServletException, IOException {
        req.setAttribute("users", users().listAll());
        if (post != null) {
            req.setAttribute("post", post);
        }
        req.setAttribute("action", action);
        WebUtil.forward(req, resp, FORM_JSP);
    }

    /** Devolve false quando já respondeu (formulário reexibido). A data do post é definida no servidor ao criar. */
    private boolean save(HttpServletRequest req, HttpServletResponse resp, boolean update)
            throws ServletException, IOException {
        Post post = update ? posts().find(WebUtil.parseId(req.getParameter("postId"))) : new Post();
        post.setContent(req.getParameter("content"));
        post.setUser(userFrom(req.getParameter("user")));
        try {
            if (update) {
                posts().update(post);
            } else {
                posts().create(post);
            }
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
            showForm(req, resp, post, update ? "update" : "insert");
            return false;
        }
        WebUtil.success(req, "Post '" + post.getContent() + "' " + (update ? "atualizado" : "salvo") + " com sucesso.");
        return true;
    }

    private void delete(HttpServletRequest req) {
        Post post = posts().find(WebUtil.parseId(req.getParameter("id")));
        posts().delete(post.getId());
        WebUtil.success(req, "Post '" + post.getContent() + "' deletado com sucesso.");
    }

    /** Usuário só com id; ausente ou não numérico vira null e o serviço rejeita. */
    private static User userFrom(String raw) {
        try {
            return new User(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}
