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

import crudmanager.application.port.in.PostUseCase;
import crudmanager.application.port.in.UserUseCase;
import crudmanager.domain.Page;
import crudmanager.domain.error.ModelException;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Post;
import crudmanager.domain.model.User;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PostsControllerTest extends ControllerTestBase {

    private PostUseCase posts;
    private UserUseCase users;
    private PostsController controller;
    private final List<User> allUsers = List.of(new User(1), new User(2));

    @BeforeEach
    void setUp() {
        posts = mock(PostUseCase.class);
        users = mock(UserUseCase.class);
        controller = new PostsController(posts, users);
        when(users.listAll()).thenReturn(allUsers);
        jsp("/index.jsp");
        jsp("/form-post.jsp");
    }

    private Post post(int id) {
        Post p = new Post(id);
        p.setContent("Olá");
        p.setUser(new User(2));
        return p;
    }

    @Test
    @DisplayName("NAV-03 GET /posts repassa page, expõe pageData e pageBaseUrl /posts e exibe index.jsp")
    void listPosts() throws Exception {
        Page<Post> page = Page.of(List.of(post(1)), 2, 10, 11);
        when(posts.list(2)).thenReturn(page);
        route("/posts", Map.of("page", "2"));

        controller.doGet(req, resp);

        verify(posts).list(2);
        verify(req).setAttribute("pageData", page);
        verify(req).setAttribute("pageBaseUrl", "/posts");
        verify(session).getAttribute("message");
        verify(req).getRequestDispatcher("/index.jsp");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-06 GET na raiz (home) lista os posts da página 1")
    void homeListsPosts() throws Exception {
        when(posts.list(1)).thenReturn(Page.of(List.of(), 1, 10, 0));
        route("", Map.of());

        controller.doGet(req, resp);

        verify(posts).list(1);
        verify(req).setAttribute("pageBaseUrl", "/posts");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-05 falha de dados ao listar exibe mensagem genérica na própria lista, sem stack trace")
    void listModelException() throws Exception {
        when(posts.list(1)).thenThrow(new ModelException("detalhe interno"));
        route("/posts", Map.of());

        controller.doGet(req, resp);

        verify(req).setAttribute("message", WebUtil.DB_ERROR_MESSAGE);
        verify(req).setAttribute("alertType", 0);
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-06 GET /post/form exibe o formulário com action insert e a lista de usuários")
    void formInsert() throws Exception {
        route("/post/form", Map.of());

        controller.doGet(req, resp);

        verify(req).setAttribute("users", allUsers);
        verify(req).setAttribute("action", "insert");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-06 GET /post/update?postId=4 exibe o formulário preenchido com action update")
    void formUpdate() throws Exception {
        Post p = post(4);
        when(posts.find(4)).thenReturn(p);
        route("/post/update", Map.of("postId", "4"));

        controller.doGet(req, resp);

        verify(req).setAttribute("post", p);
        verify(req).setAttribute("users", allUsers);
        verify(req).setAttribute("action", "update");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("EDGE-2 GET /post/update com id não numérico redireciona para /posts com 'Post não encontrado.'")
    void formUpdateNonNumericId() throws Exception {
        route("/post/update", Map.of("postId", "abc"));

        controller.doGet(req, resp);

        verifyFlash("Post não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/posts");
        verify(dispatcher, never()).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-06 POST /post/insert chama create com conteúdo, usuário e data de hoje, avisa e redireciona")
    void insert() throws Exception {
        route("/post/insert", Map.of("content", "Olá", "user", "2"));

        controller.doPost(req, resp);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(posts).create(captor.capture());
        assertEquals("Olá", captor.getValue().getContent());
        assertEquals(2, captor.getValue().getUser().getId());
        assertEquals(LocalDate.now(), captor.getValue().getPostDate());
        verifyFlash("Post 'Olá' salvo com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/posts");
    }

    @Test
    @DisplayName("ARQ-06 POST /post/insert inválido reexibe o formulário com valores digitados, errors, users e action")
    void insertInvalid() throws Exception {
        Map<String, String> errors = Map.of("content", "Informe o conteúdo.");
        doThrow(new ValidationException(errors)).when(posts).create(any(Post.class));
        route("/post/insert", Map.of("content", "", "user", "2"));

        controller.doPost(req, resp);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(req).setAttribute(org.mockito.ArgumentMatchers.eq("post"), captor.capture());
        assertEquals(2, captor.getValue().getUser().getId());
        verify(req).setAttribute("errors", errors);
        verify(req).setAttribute("users", allUsers);
        verify(req).setAttribute("action", "insert");
        verify(dispatcher).forward(req, resp);
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("ARQ-06 POST /post/insert sem usuário repassa post sem usuário ao serviço, que o rejeita")
    void insertWithoutUser() throws Exception {
        route("/post/insert", Map.of("content", "Olá"));

        controller.doPost(req, resp);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(posts).create(captor.capture());
        assertNull(captor.getValue().getUser());
    }

    @Test
    @DisplayName("ARQ-06 POST /post/update altera conteúdo e usuário do post existente, mantendo a data")
    void update() throws Exception {
        Post existing = post(4);
        LocalDate originalDate = LocalDate.of(2020, 1, 2);
        existing.setPostDate(originalDate);
        when(posts.find(4)).thenReturn(existing);
        route("/post/update", Map.of("postId", "4", "content", "Novo", "user", "1"));

        controller.doPost(req, resp);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(posts).update(captor.capture());
        assertEquals(4, captor.getValue().getId());
        assertEquals("Novo", captor.getValue().getContent());
        assertEquals(1, captor.getValue().getUser().getId());
        assertEquals(originalDate, captor.getValue().getPostDate());
        verifyFlash("Post 'Novo' atualizado com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/posts");
    }

    @Test
    @DisplayName("ARQ-06 POST /post/update inválido reexibe o formulário com action update, sem redirect")
    void updateInvalid() throws Exception {
        Map<String, String> errors = new HashMap<>(Map.of("content", "Informe o conteúdo."));
        when(posts.find(4)).thenReturn(post(4));
        doThrow(new ValidationException(errors)).when(posts).update(any(Post.class));
        route("/post/update", Map.of("postId", "4", "content", "", "user", "1"));

        controller.doPost(req, resp);

        verify(req).setAttribute("errors", errors);
        verify(req).setAttribute("action", "update");
        verify(dispatcher).forward(req, resp);
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("EDGE-2 POST /post/update com id não numérico redireciona com 'Post não encontrado.' sem alterar")
    void updateNonNumericId() throws Exception {
        route("/post/update", Map.of("postId", "x", "content", "a", "user", "1"));

        controller.doPost(req, resp);

        verifyFlash("Post não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/posts");
        verify(posts, never()).update(any());
    }

    @Test
    @DisplayName("PAG-07 POST /post/delete remove o post e redireciona para /posts?page=N")
    void deleteKeepsPage() throws Exception {
        when(posts.find(7)).thenReturn(post(7));
        route("/post/delete", Map.of("id", "7", "page", "3"));

        controller.doPost(req, resp);

        verify(posts).delete(7);
        verifyFlash("Post 'Olá' deletado com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/posts?page=3");
    }

    @Test
    @DisplayName("EDGE-2 POST /post/delete com id não numérico redireciona com 'Post não encontrado.' sem excluir")
    void deleteNonNumericId() throws Exception {
        route("/post/delete", Map.of("id", "x", "page", "2"));

        controller.doPost(req, resp);

        verifyFlash("Post não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/posts?page=2");
        verify(posts, never()).delete(anyInt());
    }

    @Test
    @DisplayName("EDGE-2 POST /post/delete de post inexistente redireciona com 'Post não encontrado.'")
    void deleteUnknown() throws Exception {
        when(posts.find(9)).thenThrow(new NotFoundException("x"));
        route("/post/delete", Map.of("id", "9"));

        controller.doPost(req, resp);

        verifyFlash("Post não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/posts?page=1");
    }

    @Test
    @DisplayName("ARQ-05 falha de dados ao salvar mostra mensagem genérica sem stack trace e redireciona")
    void insertModelException() throws Exception {
        doThrow(new ModelException("PSQLException")).when(posts).create(any(Post.class));
        route("/post/insert", Map.of("content", "Olá", "user", "2"));

        controller.doPost(req, resp);

        verifyFlash(WebUtil.DB_ERROR_MESSAGE, 0);
        verify(resp).sendRedirect("/crud-manager/posts");
    }

    @Test
    @DisplayName("ARQ-05 POST em rota desconhecida responde 405")
    void unknownPost() throws Exception {
        route("/posts", Map.of());

        controller.doPost(req, resp);

        verify(resp).sendError(405);
    }
}
