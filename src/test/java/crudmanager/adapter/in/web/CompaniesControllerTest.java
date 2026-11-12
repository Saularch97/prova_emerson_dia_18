package crudmanager.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import crudmanager.application.port.in.CompanyUseCase;
import crudmanager.application.port.in.UserUseCase;
import crudmanager.application.port.out.CompanyDAO;
import crudmanager.application.service.CompanyService;
import crudmanager.domain.Page;
import crudmanager.domain.error.ModelException;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.RelatedDataException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Company;
import crudmanager.domain.model.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CompaniesControllerTest extends ControllerTestBase {

    private CompanyUseCase companies;
    private UserUseCase users;
    private CompaniesController controller;
    private CompanyDAO dao;
    private CompaniesController realController;
    private final List<User> allUsers = List.of(new User(1), new User(2));

    @BeforeEach
    void setUp() {
        companies = mock(CompanyUseCase.class);
        users = mock(UserUseCase.class);
        controller = new CompaniesController(companies, users);
        dao = mock(CompanyDAO.class);
        realController = new CompaniesController(new CompanyService(dao), users);
        when(users.listAll()).thenReturn(allUsers);
        jsp("/companies.jsp");
        jsp("/form-company.jsp");
    }

    private Company company(int id) {
        Company c = new Company(id);
        c.setName("Acme");
        c.setRole("Dev");
        c.setStart(LocalDate.of(2020, 1, 2));
        c.setUser(new User(2));
        return c;
    }

    @Test
    @DisplayName("ARQ-06 GET /companies repassa page, expõe pageData e pageBaseUrl /companies e exibe companies.jsp")
    void list() throws Exception {
        Page<Company> page = Page.of(List.of(company(1)), 2, 10, 11);
        when(companies.list(2)).thenReturn(page);
        route("/companies", Map.of("page", "2"));

        controller.doGet(req, resp);

        verify(companies).list(2);
        verify(req).setAttribute("pageData", page);
        verify(req).setAttribute("pageBaseUrl", "/companies");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-05 falha de dados ao listar exibe mensagem genérica na própria lista, sem stack trace")
    void listModelException() throws Exception {
        when(companies.list(1)).thenThrow(new ModelException("detalhe interno"));
        route("/companies", Map.of());

        controller.doGet(req, resp);

        verify(req).setAttribute("message", WebUtil.DB_ERROR_MESSAGE);
        verify(req).setAttribute("alertType", 0);
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-06 GET /company/form exibe o formulário com action insert e a lista de usuários")
    void formInsert() throws Exception {
        route("/company/form", Map.of());

        controller.doGet(req, resp);

        verify(req).setAttribute("users", allUsers);
        verify(req).setAttribute("action", "insert");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-06 GET /company/update?companyId=4 exibe o formulário preenchido com action update")
    void formUpdate() throws Exception {
        Company c = company(4);
        when(companies.find(4)).thenReturn(c);
        route("/company/update", Map.of("companyId", "4"));

        controller.doGet(req, resp);

        verify(req).setAttribute("company", c);
        verify(req).setAttribute("users", allUsers);
        verify(req).setAttribute("action", "update");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("EDGE-2 GET /company/update com id não numérico redireciona para /companies com 'Empresa não encontrada.'")
    void formUpdateNonNumericId() throws Exception {
        route("/company/update", Map.of("companyId", "abc"));

        controller.doGet(req, resp);

        verifyFlash("Empresa não encontrada.", 0);
        verify(resp).sendRedirect("/crud-manager/companies");
        verify(dispatcher, never()).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-06 POST /company/insert chama create com os campos, avisa e redireciona; saída vazia vira null")
    void insert() throws Exception {
        route("/company/insert", Map.of("name", "Acme", "role", "Dev", "start", "2020-01-02", "end", "", "user", "2"));

        controller.doPost(req, resp);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companies).create(captor.capture());
        Company c = captor.getValue();
        assertEquals("Acme", c.getName());
        assertEquals("Dev", c.getRole());
        assertEquals(LocalDate.of(2020, 1, 2), c.getStart());
        assertNull(c.getEnd());
        assertEquals(2, c.getUser().getId());
        verifyFlash("Empresa 'Acme' salva com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/companies");
    }

    @Test
    @DisplayName("ARQ-06 POST /company/insert inválido reexibe o formulário com valores digitados, errors, users e action")
    void insertInvalid() throws Exception {
        Map<String, String> errors = Map.of("name", "Informe o nome.");
        doThrow(new ValidationException(errors)).when(companies).create(any(Company.class));
        route("/company/insert", Map.of("name", "", "role", "Dev", "start", "2020-01-02", "user", "2"));

        controller.doPost(req, resp);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(req).setAttribute(org.mockito.ArgumentMatchers.eq("company"), captor.capture());
        assertEquals("Dev", captor.getValue().getRole());
        verify(req).setAttribute("errors", errors);
        verify(req).setAttribute("users", allUsers);
        verify(req).setAttribute("action", "insert");
        verify(dispatcher).forward(req, resp);
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("ARQ-06 POST /company/insert com data inválida reexibe o formulário com erro no campo e nada é gravado")
    void insertInvalidDate() throws Exception {
        route("/company/insert", Map.of("name", "Acme", "role", "Dev", "start", "32/13/2020", "user", "2"));

        realController.doPost(req, resp);

        verify(dao, never()).save(any());
        verify(req).setAttribute("errors", Map.of("start", "Informe uma data válida."));
        verify(req).setAttribute("action", "insert");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-06 POST /company/insert com saída ilegível e nome vazio mostra as duas mensagens e nada é gravado")
    void insertInvalidEndAndEmptyName() throws Exception {
        route("/company/insert", Map.of("name", "", "role", "Dev", "start", "2020-01-02", "end", "xx", "user", "2"));

        realController.doPost(req, resp);

        verify(dao, never()).save(any());
        Map<String, String> expected = new java.util.LinkedHashMap<>();
        expected.put("end", "Informe uma data válida.");
        expected.put("name", "Informe o nome.");
        verify(req).setAttribute("errors", expected);
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-06 POST /company/insert só com saída ilegível reexibe o formulário e nada é gravado")
    void insertOnlyInvalidEnd() throws Exception {
        route("/company/insert", Map.of("name", "Acme", "role", "Dev", "start", "2020-01-02", "end", "xx", "user", "2"));

        realController.doPost(req, resp);

        verify(dao, never()).save(any());
        verify(req).setAttribute("errors", Map.of("end", "Informe uma data válida."));
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-06 POST /company/update altera os campos da empresa existente, avisa e redireciona")
    void update() throws Exception {
        when(companies.find(4)).thenReturn(company(4));
        route("/company/update", Map.of("companyId", "4", "name", "Novo", "role", "Lead",
                "start", "2021-05-06", "end", "2022-01-01", "user", "1"));

        controller.doPost(req, resp);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companies).update(captor.capture());
        Company c = captor.getValue();
        assertEquals(4, c.getId());
        assertEquals("Novo", c.getName());
        assertEquals(LocalDate.of(2022, 1, 1), c.getEnd());
        assertEquals(1, c.getUser().getId());
        verifyFlash("Empresa 'Novo' atualizada com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/companies");
    }

    @Test
    @DisplayName("ARQ-06 POST /company/update inválido reexibe o formulário com action update, sem redirect")
    void updateInvalid() throws Exception {
        Map<String, String> errors = Map.of("role", "Informe o cargo.");
        when(companies.find(4)).thenReturn(company(4));
        doThrow(new ValidationException(errors)).when(companies).update(any(Company.class));
        route("/company/update", Map.of("companyId", "4", "name", "Acme", "role", "", "start", "2020-01-02", "user", "1"));

        controller.doPost(req, resp);

        verify(req).setAttribute("errors", errors);
        verify(req).setAttribute("action", "update");
        verify(dispatcher).forward(req, resp);
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("EDGE-2 POST /company/update com id não numérico redireciona com 'Empresa não encontrada.' sem alterar")
    void updateNonNumericId() throws Exception {
        route("/company/update", Map.of("companyId", "x", "name", "a"));

        controller.doPost(req, resp);

        verifyFlash("Empresa não encontrada.", 0);
        verify(resp).sendRedirect("/crud-manager/companies");
        verify(companies, never()).update(any());
    }

    @Test
    @DisplayName("ARQ-06 POST /company/delete remove a empresa e redireciona para /companies?page=N")
    void deleteKeepsPage() throws Exception {
        when(companies.find(7)).thenReturn(company(7));
        route("/company/delete", Map.of("id", "7", "page", "3"));

        controller.doPost(req, resp);

        verify(companies).delete(7);
        verifyFlash("Empresa 'Acme' deletada com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/companies?page=3");
    }

    @Test
    @DisplayName("ARQ-06 POST /company/delete com dados relacionados mantém a empresa e avisa")
    void deleteRelated() throws Exception {
        when(companies.find(7)).thenReturn(company(7));
        doThrow(new RelatedDataException("fk")).when(companies).delete(7);
        route("/company/delete", Map.of("id", "7", "page", "2"));

        controller.doPost(req, resp);

        verifyFlash("Empresa 'Acme' não pode ser deletado. Há dados relacionados à empresa.", 0);
        verify(resp).sendRedirect("/crud-manager/companies?page=2");
    }

    @Test
    @DisplayName("EDGE-2 POST /company/delete com id não numérico redireciona com 'Empresa não encontrada.' sem excluir")
    void deleteNonNumericId() throws Exception {
        route("/company/delete", Map.of("id", "x", "page", "2"));

        controller.doPost(req, resp);

        verifyFlash("Empresa não encontrada.", 0);
        verify(resp).sendRedirect("/crud-manager/companies?page=2");
        verify(companies, never()).delete(anyInt());
    }

    @Test
    @DisplayName("EDGE-2 POST /company/delete de empresa inexistente redireciona com 'Empresa não encontrada.'")
    void deleteUnknown() throws Exception {
        when(companies.find(9)).thenThrow(new NotFoundException("x"));
        route("/company/delete", Map.of("id", "9"));

        controller.doPost(req, resp);

        verifyFlash("Empresa não encontrada.", 0);
        verify(resp).sendRedirect("/crud-manager/companies?page=1");
    }

    @Test
    @DisplayName("ARQ-05 falha de dados ao salvar mostra mensagem genérica sem stack trace e redireciona")
    void insertModelException() throws Exception {
        doThrow(new ModelException("PSQLException")).when(companies).create(any(Company.class));
        route("/company/insert", Map.of("name", "Acme", "role", "Dev", "start", "2020-01-02", "user", "2"));

        controller.doPost(req, resp);

        verifyFlash(WebUtil.DB_ERROR_MESSAGE, 0);
        verify(resp).sendRedirect("/crud-manager/companies");
    }

    @Test
    @DisplayName("ARQ-05 POST em rota desconhecida responde 405")
    void unknownPost() throws Exception {
        route("/companies", Map.of());

        controller.doPost(req, resp);

        verify(resp).sendError(405);
    }
}
