package crudmanager.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import crudmanager.exception.ModelException;
import crudmanager.exception.NotFoundException;
import crudmanager.exception.RelatedDataException;
import crudmanager.exception.ValidationException;
import crudmanager.model.Page;
import crudmanager.model.User;
import crudmanager.service.UserService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UsersControllerTest extends ControllerTestBase {

    private UserService users;
    private UsersController controller;

    @BeforeEach
    void setUp() {
        users = mock(UserService.class);
        controller = new UsersController(users);
        jsp("/users.jsp");
        jsp("/form-user.jsp");
    }

    private User ana(int id) {
        User u = new User(id);
        u.setName("Ana");
        u.setGender("F");
        u.setEmail("ana@mail.com");
        return u;
    }

    private Map<String, String> form(String id) {
        return Map.of("userId", id, "name", "Ana", "gender", "F", "email", "ana@mail.com", "password", "segredo");
    }

    @Test
    @DisplayName("ARQ-05 GET /users repassa page ao caso de uso e expõe pageData, pageBaseUrl e a mensagem flash")
    void listPassesPage() throws Exception {
        Page<User> page = Page.of(List.of(ana(1)), 2, 10, 11);
        when(users.list(2)).thenReturn(page);
        route("/users", Map.of("page", "2"));

        controller.doGet(req, resp);

        verify(users).list(2);
        verify(req).setAttribute("pageData", page);
        verify(req).setAttribute("pageBaseUrl", "/users");
        verify(session).getAttribute("message");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-06 GET /users com page inválido lista a página 1")
    void listInvalidPage() throws Exception {
        when(users.list(1)).thenReturn(Page.of(List.of(), 1, 10, 0));
        route("/users", Map.of("page", "abc"));

        controller.doGet(req, resp);

        verify(users).list(1);
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-05 falha de dados ao listar exibe mensagem genérica na própria lista, sem stack trace")
    void listModelException() throws Exception {
        when(users.list(1)).thenThrow(new ModelException("conexão recusada em 10.0.0.1"));
        route("/users", Map.of());

        controller.doGet(req, resp);

        verify(req).setAttribute("message", WebUtil.DB_ERROR_MESSAGE);
        verify(req).setAttribute("alertType", 0);
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-05 GET /user/form exibe o formulário com action insert")
    void formInsert() throws Exception {
        route("/user/form", Map.of());

        controller.doGet(req, resp);

        verify(req).setAttribute("action", "insert");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-06 GET /user/update?userId=3 exibe o formulário preenchido com action update")
    void formUpdate() throws Exception {
        User u = ana(3);
        when(users.find(3)).thenReturn(u);
        route("/user/update", Map.of("userId", "3"));

        controller.doGet(req, resp);

        verify(req).setAttribute("user", u);
        verify(req).setAttribute("action", "update");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("EDGE-2 GET /user/update com id não numérico redireciona para a lista com 'Usuário não encontrado.'")
    void formUpdateNonNumericId() throws Exception {
        route("/user/update", Map.of("userId", "abc"));

        controller.doGet(req, resp);

        verifyFlash("Usuário não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/users");
        verify(users, never()).find(anyInt());
        verify(dispatcher, never()).forward(req, resp);
    }

    @Test
    @DisplayName("EDGE-2 GET /user/update com id inexistente redireciona para a lista com 'Usuário não encontrado.'")
    void formUpdateUnknownId() throws Exception {
        when(users.find(99)).thenThrow(new NotFoundException("x"));
        route("/user/update", Map.of("userId", "99"));

        controller.doGet(req, resp);

        verifyFlash("Usuário não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/users");
    }

    @Test
    @DisplayName("AUTH-12 POST /user/insert chama create com os campos e a senha, avisa e redireciona para a lista")
    void insert() throws Exception {
        route("/user/insert", form("0"));

        controller.doPost(req, resp);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).create(captor.capture(), org.mockito.ArgumentMatchers.eq("segredo"));
        assertEquals("Ana", captor.getValue().getName());
        assertEquals("F", captor.getValue().getGender());
        assertEquals("ana@mail.com", captor.getValue().getEmail());
        verifyFlash("Usuário 'Ana' salvo com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/users");
    }

    @Test
    @DisplayName("AUTH-12 POST /user/insert inválido reexibe o formulário com valores digitados, errors e action, sem redirect")
    void insertInvalid() throws Exception {
        Map<String, String> errors = Map.of("password", "Informe a senha.");
        doThrow(new ValidationException(errors)).when(users).create(any(User.class), any());
        route("/user/insert", form("0"));

        controller.doPost(req, resp);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(req).setAttribute(org.mockito.ArgumentMatchers.eq("user"), captor.capture());
        assertEquals("Ana", captor.getValue().getName());
        assertEquals("ana@mail.com", captor.getValue().getEmail());
        assertEquals(null, captor.getValue().getPasswordHash());
        verify(req).setAttribute("errors", errors);
        verify(req).setAttribute("action", "insert");
        verify(dispatcher).forward(req, resp);
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("AUTH-13 POST /user/update com senha em branco chama update com a senha vazia e avisa 'atualizado'")
    void updateBlankPassword() throws Exception {
        Map<String, String> params = new java.util.HashMap<>(form("5"));
        params.put("password", "");
        route("/user/update", params);

        controller.doPost(req, resp);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).update(captor.capture(), org.mockito.ArgumentMatchers.eq(""));
        assertEquals(5, captor.getValue().getId());
        verifyFlash("Usuário 'Ana' atualizado com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/users");
    }

    @Test
    @DisplayName("AUTH-13 POST /user/update inválido reexibe o formulário com action update, sem redirect")
    void updateInvalid() throws Exception {
        Map<String, String> errors = Map.of("name", "Informe o nome.");
        doThrow(new ValidationException(errors)).when(users).update(any(User.class), any());
        route("/user/update", form("5"));

        controller.doPost(req, resp);

        verify(req).setAttribute("errors", errors);
        verify(req).setAttribute("action", "update");
        verify(dispatcher).forward(req, resp);
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("EDGE-2 POST /user/update com id não numérico redireciona para a lista com 'Usuário não encontrado.'")
    void updateNonNumericId() throws Exception {
        route("/user/update", form("abc"));

        controller.doPost(req, resp);

        verifyFlash("Usuário não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/users");
        verify(users, never()).update(any(), any());
    }

    @Test
    @DisplayName("ARQ-06 POST /user/update de usuário removido redireciona para a lista com 'Usuário não encontrado.'")
    void updateNotFound() throws Exception {
        doThrow(new NotFoundException("x")).when(users).update(any(User.class), any());
        route("/user/update", form("5"));

        controller.doPost(req, resp);

        verifyFlash("Usuário não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/users");
    }

    @Test
    @DisplayName("PAG-07 POST /user/delete remove o usuário e redireciona para /users?page=N")
    void deleteKeepsPage() throws Exception {
        when(users.find(7)).thenReturn(ana(7));
        route("/user/delete", Map.of("id", "7", "page", "3"));

        controller.doPost(req, resp);

        verify(users).delete(7);
        verifyFlash("Usuário 'Ana' deletado com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/users?page=3");
    }

    @Test
    @DisplayName("EDGE-2 POST /user/delete com id não numérico redireciona com 'Usuário não encontrado.' sem excluir")
    void deleteNonNumericId() throws Exception {
        route("/user/delete", Map.of("id", "x", "page", "2"));

        controller.doPost(req, resp);

        verifyFlash("Usuário não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/users?page=2");
        verify(users, never()).delete(anyInt());
    }

    @Test
    @DisplayName("EDGE-1 POST /user/delete com dados relacionados mantém o usuário e avisa")
    void deleteRelated() throws Exception {
        when(users.find(7)).thenReturn(ana(7));
        doThrow(new RelatedDataException("fk")).when(users).delete(7);
        route("/user/delete", Map.of("id", "7", "page", "1"));

        controller.doPost(req, resp);

        verifyFlash("Usuário 'Ana' não pode ser deletado. Há dados relacionados ao usuário.", 0);
        verify(resp).sendRedirect("/crud-manager/users?page=1");
    }

    @Test
    @DisplayName("ARQ-05 falha de dados ao salvar mostra mensagem genérica sem stack trace e redireciona")
    void insertModelException() throws Exception {
        doThrow(new ModelException("org.postgresql.util.PSQLException: refused")).when(users).create(any(User.class), any());
        route("/user/insert", form("0"));

        controller.doPost(req, resp);

        verifyFlash(WebUtil.DB_ERROR_MESSAGE, 0);
        verify(resp).sendRedirect("/crud-manager/users");
    }

    @Test
    @DisplayName("ARQ-05 POST em rota desconhecida responde 405")
    void unknownPost() throws Exception {
        route("/users", Map.of());

        controller.doPost(req, resp);

        verify(resp).sendError(405);
    }
}
