package crudmanager.application.service;

import crudmanager.application.port.in.PostUseCase;
import crudmanager.application.port.out.PostDAO;
import crudmanager.domain.Page;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Post;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PostService implements PostUseCase {

    private final PostDAO posts;

    public PostService(PostDAO posts) {
        this.posts = posts;
    }

    @Override
    public Page<Post> list(int page) {
        long total = posts.count();
        int current = Pagination.clamp(page, total);
        List<Post> items = posts.findPage(Pagination.offset(current), Pagination.PAGE_SIZE);
        return Page.of(items, current, Pagination.PAGE_SIZE, total);
    }

    @Override
    public Post find(int id) {
        Post post = posts.findById(id);
        if (post == null) {
            throw new NotFoundException("Post não encontrado.");
        }
        return post;
    }

    @Override
    public void create(Post post) {
        validate(post);
        posts.save(post);
    }

    @Override
    public void update(Post post) {
        find(post.getId());
        validate(post);
        posts.update(post);
    }

    @Override
    public void delete(int id) {
        posts.delete(find(id));
    }

    private static void validate(Post post) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (post.getContent() == null || post.getContent().isBlank()) {
            errors.put("content", "Informe o conteúdo.");
        }
        if (post.getPostDate() == null) {
            errors.put("postDate", "Informe a data.");
        }
        if (post.getUser() == null || post.getUser().getId() <= 0) {
            errors.put("user", "Informe o usuário.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
