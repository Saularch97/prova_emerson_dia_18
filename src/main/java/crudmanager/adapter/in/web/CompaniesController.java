package crudmanager.adapter.in.web;

import crudmanager.application.port.in.CompanyUseCase;
import crudmanager.application.port.in.UserUseCase;
import crudmanager.application.service.Pagination;
import crudmanager.domain.Page;
import crudmanager.domain.error.ModelException;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.RelatedDataException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Company;
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

@WebServlet(urlPatterns = {"/companies", "/company/form", "/company/insert", "/company/update", "/company/delete"})
public class CompaniesController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(CompaniesController.class.getName());
    static final String LIST_JSP = "/companies.jsp";
    static final String FORM_JSP = "/form-company.jsp";
    private static final String LIST_URL = "/companies";
    private static final String NOT_FOUND = "Empresa não encontrada.";
    private static final String INVALID_DATE = "Informe uma data válida.";

    private final transient CompanyUseCase injectedCompanies;
    private final transient UserUseCase injectedUsers;

    public CompaniesController() {
        this(null, null);
    }

    CompaniesController(CompanyUseCase companies, UserUseCase users) {
        this.injectedCompanies = companies;
        this.injectedUsers = users;
    }

    private CompanyUseCase companies() {
        return injectedCompanies != null ? injectedCompanies : UseCases.instance().companies();
    }

    private UserUseCase users() {
        return injectedUsers != null ? injectedUsers : UseCases.instance().users();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (WebUtil.route(req)) {
                case "/companies":
                    list(req, resp);
                    break;
                case "/company/form":
                    showForm(req, resp, null, "insert");
                    break;
                case "/company/update":
                    showForm(req, resp, companies().find(WebUtil.parseId(req.getParameter("companyId"))), "update");
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
                case "/company/insert":
                    if (!save(req, resp, false)) {
                        return;
                    }
                    break;
                case "/company/update":
                    if (!save(req, resp, true)) {
                        return;
                    }
                    break;
                case "/company/delete":
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
        Page<Company> page;
        try {
            page = companies().list(WebUtil.parsePage(req));
        } catch (ModelException e) {
            LOG.log(Level.SEVERE, "Falha ao listar empresas", e);
            req.setAttribute(WebUtil.MESSAGE_KEY, WebUtil.DB_ERROR_MESSAGE);
            req.setAttribute(WebUtil.ALERT_TYPE_KEY, WebUtil.ALERT_ERROR);
            page = Page.of(List.of(), 1, Pagination.PAGE_SIZE, 0);
        }
        WebUtil.pageData(req, page, LIST_URL);
        WebUtil.forward(req, resp, LIST_JSP);
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Company company, String action)
            throws ServletException, IOException {
        req.setAttribute("users", users().listAll());
        if (company != null) {
            req.setAttribute("company", company);
        }
        req.setAttribute("action", action);
        WebUtil.forward(req, resp, FORM_JSP);
    }

    /** Devolve false quando já respondeu (formulário reexibido). */
    private boolean save(HttpServletRequest req, HttpServletResponse resp, boolean update)
            throws ServletException, IOException {
        Company company = update ? companies().find(WebUtil.parseId(req.getParameter("companyId"))) : new Company();
        company.setName(req.getParameter("name"));
        company.setRole(req.getParameter("role"));
        company.setUser(userFrom(req.getParameter("user")));
        Map<String, String> errors = new LinkedHashMap<>();
        company.setStart(date(req.getParameter("start"), "start", errors));
        company.setEnd(date(req.getParameter("end"), "end", errors));
        try {
            if (errors.isEmpty()) {
                if (update) {
                    companies().update(company);
                } else {
                    companies().create(company);
                }
            }
        } catch (ValidationException e) {
            errors.putAll(e.getErrors());
        }
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            showForm(req, resp, company, update ? "update" : "insert");
            return false;
        }
        WebUtil.success(req, "Empresa '" + company.getName() + "' " + (update ? "atualizada" : "salva")
                + " com sucesso.");
        return true;
    }

    private void delete(HttpServletRequest req) {
        Company company = companies().find(WebUtil.parseId(req.getParameter("id")));
        try {
            companies().delete(company.getId());
        } catch (RelatedDataException e) {
            throw new RelatedDataException("Empresa '" + company.getName()
                    + "' não pode ser deletado. Há dados relacionados à empresa.");
        }
        WebUtil.success(req, "Empresa '" + company.getName() + "' deletada com sucesso.");
    }

    /** Data inválida vira erro de campo; vazia vira null e o serviço decide se é obrigatória. */
    private static LocalDate date(String raw, String field, Map<String, String> errors) {
        try {
            return WebUtil.parseDate(raw);
        } catch (DateTimeParseException e) {
            errors.put(field, INVALID_DATE);
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
