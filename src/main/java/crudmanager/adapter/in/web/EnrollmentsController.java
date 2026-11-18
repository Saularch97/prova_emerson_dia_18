package crudmanager.adapter.in.web;

import crudmanager.application.port.in.CourseUseCase;
import crudmanager.application.port.in.EnrollmentUseCase;
import crudmanager.application.port.in.UserUseCase;
import crudmanager.application.service.Pagination;
import crudmanager.domain.Page;
import crudmanager.domain.error.DuplicateException;
import crudmanager.domain.error.ModelException;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.RelatedDataException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Course;
import crudmanager.domain.model.Enrollment;
import crudmanager.domain.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(urlPatterns = {"/enrollments", "/enrollment/form", "/enrollment/insert", "/enrollment/update",
        "/enrollment/delete"})
public class EnrollmentsController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(EnrollmentsController.class.getName());
    static final String LIST_JSP = "/enrollments.jsp";
    static final String FORM_JSP = "/form-enrollment.jsp";
    private static final String LIST_URL = "/enrollments";
    private static final String NOT_FOUND = "Matrícula não encontrada.";
    static final String INVALID_DATE = "Informe uma data de matrícula válida.";
    private static final int INVALID_PROGRESS_VALUE = -1;
    static final String INVALID_PROGRESS = "O progresso deve estar entre 0 e 100.";

    private final EnrollmentUseCase injectedEnrollments;
    private final UserUseCase injectedUsers;
    private final CourseUseCase injectedCourses;

    public EnrollmentsController() {
        this(null, null, null);
    }

    EnrollmentsController(EnrollmentUseCase enrollments, UserUseCase users, CourseUseCase courses) {
        this.injectedEnrollments = enrollments;
        this.injectedUsers = users;
        this.injectedCourses = courses;
    }

    private EnrollmentUseCase enrollments() {
        return injectedEnrollments != null ? injectedEnrollments : UseCases.instance().enrollments();
    }

    private UserUseCase users() {
        return injectedUsers != null ? injectedUsers : UseCases.instance().users();
    }

    private CourseUseCase courses() {
        return injectedCourses != null ? injectedCourses : UseCases.instance().courses();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (WebUtil.route(req)) {
                case "/enrollments":
                    list(req, resp);
                    break;
                case "/enrollment/form":
                    Enrollment fresh = new Enrollment();
                    fresh.setEnrolledOn(LocalDate.now());
                    showForm(req, resp, fresh, "insert");
                    break;
                case "/enrollment/update":
                    showForm(req, resp,
                            enrollments().find(WebUtil.parseId(req.getParameter("enrollmentId"))), "update");
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
                case "/enrollment/insert":
                    if (!save(req, resp, false)) {
                        return;
                    }
                    break;
                case "/enrollment/update":
                    if (!save(req, resp, true)) {
                        return;
                    }
                    break;
                case "/enrollment/delete":
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
        Page<Enrollment> page;
        try {
            page = enrollments().list(WebUtil.parsePage(req));
        } catch (ModelException e) {
            LOG.log(Level.SEVERE, "Falha ao listar matrículas", e);
            req.setAttribute(WebUtil.MESSAGE_KEY, WebUtil.DB_ERROR_MESSAGE);
            req.setAttribute(WebUtil.ALERT_TYPE_KEY, WebUtil.ALERT_ERROR);
            page = Page.of(List.of(), 1, Pagination.PAGE_SIZE, 0);
        }
        WebUtil.pageData(req, page, LIST_URL);
        WebUtil.forward(req, resp, LIST_JSP);
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Enrollment enrollment, String action)
            throws ServletException, IOException {
        req.setAttribute("users", users().listAll());
        req.setAttribute("courses", courses().listAll());
        req.setAttribute("statuses", Enrollment.Status.values());
        req.setAttribute("enrollment", enrollment);
        req.setAttribute("action", action);
        WebUtil.forward(req, resp, FORM_JSP);
    }

    /**
     * Devolve false quando já respondeu (formulário reexibido). Data ou progresso não interpretáveis viram valor
     * inválido (null e {@code -1}) e o serviço sempre roda, rejeitando-os, de modo que nada é gravado; a mensagem de
     * parse prevalece na mesma chave e as demais mensagens do serviço aparecem juntas; matrícula duplicada vira erro no campo {@code course}.
     */
    private boolean save(HttpServletRequest req, HttpServletResponse resp, boolean update)
            throws ServletException, IOException {
        Enrollment enrollment = update
                ? enrollments().find(WebUtil.parseId(req.getParameter("enrollmentId"))) : new Enrollment();
        enrollment.setStudent(userFrom(req.getParameter("student")));
        enrollment.setCourse(courseFrom(req.getParameter("course")));
        enrollment.setStatus(statusFrom(req.getParameter("status")));
        enrollment.setPaid(req.getParameter("paid") != null);
        Map<String, String> errors = new LinkedHashMap<>();
        try {
            enrollment.setEnrolledOn(WebUtil.parseDate(req.getParameter("enrolledOn")));
        } catch (DateTimeParseException e) {
            enrollment.setEnrolledOn(null);
            errors.put("enrolledOn", INVALID_DATE);
        }
        String rawProgress = req.getParameter("progress");
        try {
            enrollment.setProgress(Integer.parseInt(rawProgress.trim()));
        } catch (NumberFormatException | NullPointerException e) {
            enrollment.setProgress(INVALID_PROGRESS_VALUE);
            errors.put("progress", INVALID_PROGRESS);
            req.setAttribute("progressInput", rawProgress);
        }
        try {
            if (update) {
                enrollments().update(enrollment);
            } else {
                enrollments().create(enrollment);
            }
        } catch (ValidationException e) {
            e.getErrors().forEach(errors::putIfAbsent);
        } catch (DuplicateException e) {
            errors.put("course", e.getMessage());
        }
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            showForm(req, resp, enrollment, update ? "update" : "insert");
            return false;
        }
        WebUtil.success(req, "Matrícula " + (update ? "atualizada" : "salva") + " com sucesso.");
        return true;
    }

    private void delete(HttpServletRequest req) {
        Enrollment enrollment = enrollments().find(WebUtil.parseId(req.getParameter("id")));
        enrollments().delete(enrollment.getId());
        WebUtil.success(req, "Matrícula deletada com sucesso.");
    }

    /** Situação desconhecida ou ausente vira null e o serviço rejeita. */
    private static Enrollment.Status statusFrom(String raw) {
        try {
            return Enrollment.Status.valueOf(raw.trim());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    /** Aluno só com id; ausente ou não numérico vira null e o serviço rejeita. */
    private static User userFrom(String raw) {
        Integer id = idFrom(raw);
        return id == null ? null : new User(id);
    }

    /** Curso só com id; ausente ou não numérico vira null e o serviço rejeita. */
    private static Course courseFrom(String raw) {
        Integer id = idFrom(raw);
        return id == null ? null : new Course(id);
    }

    private static Integer idFrom(String raw) {
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}
