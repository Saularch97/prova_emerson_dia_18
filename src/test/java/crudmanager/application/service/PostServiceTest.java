package crudmanager.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crudmanager.domain.Page;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Post;
import crudmanager.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostServiceTest {

    private final FakePostDAO posts = new FakePostDAO();
    private final PostService service = new PostService(posts);

    private Post valid() {
        Post p = new Post();
        p.setContent("Olá");
        p.setUser(new User(1));
        return p;
    }

    @Test
    @DisplayName("ARQ-06 post válido é criado")
    void createValid() {
        service.create(valid());
        assertEquals(1, posts.count());
    }

    @Test
    @DisplayName("ARQ-06 conteúdo vazio é rejeitado")
    void emptyContent() {
        for (String c : new String[] {"", "   ", null}) {
            Post p = valid();
            p.setContent(c);
            ValidationException e = assertThrows(ValidationException.class, () -> service.create(p));
            assertTrue(e.getErrors().containsKey("content"));
        }
        assertEquals(0, posts.count());
    }

    @Test
    @DisplayName("ARQ-06 usuário é obrigatório")
    void userRequired() {
        Post p = valid();
        p.setUser(null);
        ValidationException e = assertThrows(ValidationException.class, () -> service.create(p));
        assertTrue(e.getErrors().containsKey("user"));
        Post q = valid();
        q.setUser(new User(0));
        assertThrows(ValidationException.class, () -> service.create(q));
    }

    @Test
    @DisplayName("ARQ-06 data é obrigatória")
    void dateRequired() {
        Post p = valid();
        p.setPostDate(null);
        ValidationException e = assertThrows(ValidationException.class, () -> service.create(p));
        assertTrue(e.getErrors().containsKey("postDate"));
    }

    @Test
    @DisplayName("ARQ-06 find, update e delete de id inexistente lançam NotFoundException")
    void notFound() {
        assertThrows(NotFoundException.class, () -> service.find(5));
        assertThrows(NotFoundException.class, () -> service.delete(5));
        Post p = new Post(5);
        p.setUser(new User(1));
        assertThrows(NotFoundException.class, () -> service.update(p));
    }

    @Test
    @DisplayName("ARQ-06 update e delete de post existente")
    void updateAndDelete() {
        Post p = new Post(7);
        p.setContent("a");
        p.setUser(new User(1));
        posts.store.put(7, p);
        Post edited = new Post(7);
        edited.setContent("b");
        edited.setUser(new User(1));
        service.update(edited);
        assertEquals("b", posts.findById(7).getContent());
        service.delete(7);
        assertEquals(0, posts.count());
    }

    @Test
    @DisplayName("ARQ-06 list(page) pagina e clampa página acima da última")
    void listClamps() {
        for (int i = 1; i <= 25; i++) {
            posts.store.put(i, new Post(i));
        }
        Page<Post> p = service.list(99);
        assertEquals(3, p.getPage());
        assertEquals(5, p.getItems().size());
        assertEquals(25, p.getTotalItems());
        assertEquals(1, service.list(0).getPage());
    }
}
