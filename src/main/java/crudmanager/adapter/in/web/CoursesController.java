package crudmanager.adapter.in.web;

import crudmanager.application.port.in.CourseUseCase;
import crudmanager.application.port.in.UserUseCase;
import crudmanager.application.service.Pagination;
import crudmanager.domain.Page;
import crudmanager.domain.error.ModelException;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.RelatedDataException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Course;
import crudmanager.domain.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(urlPatterns = {"/courses", "/course/form", "/course/insert", "/course/update", "/course/delete"})
public class CoursesController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(CoursesController.class.getName());
    static final String LIST_JSP = "/courses.jsp";
    static final String FORM_JSP = "/form-course.jsp";
    private static final String LIST_URL = "/courses";
    private static final String NOT_FOUND = "Curso não encontrado.";
    static final String INVALID_PRICE = "Informe um preço numérico válido (ex.: 99,90).";
    static final String INVALID_DATE = "Informe uma data de início válida.";

    private final CourseUseCase injectedCourses;
    private final UserUseCase injectedUsers;

    public CoursesController() {
        this(null, null);
    }

    CoursesController(CourseUseCase courses, UserUseCase users) {
        this.injectedCourses = courses;
        this.injectedUsers = users;
    }

    private CourseUseCase courses() {
        return injectedCourses != null ? injectedCourses : UseCases.instance().courses();
    }

    private UserUseCase users() {
        return injectedUsers != null ? injectedUsers : UseCases.instance().users();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (WebUtil.route(req)) {
                case "/courses":
                    list(req, resp);
                    break;
                case "/course/form":
                    showForm(req, resp, null, "insert");
                    break;
                case "/course/update":
                    showForm(req, resp, courses().find(WebUtil.parseId(req.getParameter("courseId"))), "update");
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
                case "/course/insert":
                    if (!save(req, resp, false)) {
                        return;
                    }
                    break;
                case "/course/update":
                    if (!save(req, resp, true)) {
                        return;
                    }
                    break;
                case "/course/delete":
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
        Page<Course> page;
        try {
            page = courses().list(WebUtil.parsePage(req));
        } catch (ModelException e) {
            LOG.log(Level.SEVERE, "Falha ao listar cursos", e);
            req.setAttribute(WebUtil.MESSAGE_KEY, WebUtil.DB_ERROR_MESSAGE);
            req.setAttribute(WebUtil.ALERT_TYPE_KEY, WebUtil.ALERT_ERROR);
            page = Page.of(List.of(), 1, Pagination.PAGE_SIZE, 0);
        }
        WebUtil.pageData(req, page, LIST_URL);
        WebUtil.forward(req, resp, LIST_JSP);
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Course course, String action)
            throws ServletException, IOException {
        req.setAttribute("users", users().listAll());
        req.setAttribute("levels", Course.Level.values());
        if (course != null) {
            req.setAttribute("course", course);
        }
        req.setAttribute("action", action);
        WebUtil.forward(req, resp, FORM_JSP);
    }

    /**
     * Devolve false quando já respondeu (formulário reexibido). Preço não numérico e data inválida viram null e
     * o serviço sempre roda (ele rejeita null, então nada é gravado); a mensagem de parse prevalece na mesma chave e
     * as demais mensagens do serviço aparecem juntas. O texto digitado do preço volta em {@code priceInput}.
     */
    private boolean save(HttpServletRequest req, HttpServletResponse resp, boolean update)
            throws ServletException, IOException {
        Course course = update ? courses().find(WebUtil.parseId(req.getParameter("courseId"))) : new Course();
        course.setTitle(req.getParameter("title"));
        course.setDescription(req.getParameter("description"));
        course.setLevel(levelFrom(req.getParameter("level")));
        course.setActive(req.getParameter("active") != null);
        course.setInstructor(userFrom(req.getParameter("instructor")));
        Map<String, String> errors = new LinkedHashMap<>();
        String rawPrice = req.getParameter("price");
        try {
            course.setPrice(parsePrice(rawPrice));
        } catch (NumberFormatException e) {
            course.setPrice(null);
            errors.put("price", INVALID_PRICE);
            req.setAttribute("priceInput", rawPrice);
        }
        try {
            course.setStartDate(WebUtil.parseDate(req.getParameter("startDate")));
        } catch (DateTimeParseException e) {
            course.setStartDate(null);
            errors.put("startDate", INVALID_DATE);
        }
        try {
            if (update) {
                courses().update(course);
            } else {
                courses().create(course);
            }
        } catch (ValidationException e) {
            e.getErrors().forEach(errors::putIfAbsent);
        }
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            showForm(req, resp, course, update ? "update" : "insert");
            return false;
        }
        WebUtil.success(req, "Curso '" + course.getTitle() + "' " + (update ? "atualizado" : "salvo")
                + " com sucesso.");
        return true;
    }

    private void delete(HttpServletRequest req) {
        Course course = courses().find(WebUtil.parseId(req.getParameter("id")));
        try {
            courses().delete(course.getId());
        } catch (RelatedDataException e) {
            throw new RelatedDataException("Curso '" + course.getTitle()
                    + "' não pode ser deletado. Há dados relacionados ao curso.");
        }
        WebUtil.success(req, "Curso '" + course.getTitle() + "' deletado com sucesso.");
    }

    /** Aceita vírgula ou ponto decimal; vazio vira null (o serviço exige); texto não numérico lança. */
    static BigDecimal parsePrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim().replace(',', '.');
        if (!text.matches("-?\\d+(\\.\\d+)?")) {
            throw new NumberFormatException(raw);
        }
        return new BigDecimal(text);
    }

    /** Nível desconhecido ou ausente vira null e o serviço rejeita. */
    private static Course.Level levelFrom(String raw) {
        try {
            return Course.Level.valueOf(raw.trim());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
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
