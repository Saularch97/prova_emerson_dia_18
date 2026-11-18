package crudmanager.service.impl;

import crudmanager.dao.CourseDAO;
import crudmanager.exception.NotFoundException;
import crudmanager.exception.ValidationException;
import crudmanager.model.Course;
import crudmanager.model.Page;
import crudmanager.service.CourseService;
import crudmanager.service.Pagination;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CourseServiceImpl implements CourseService {

    private static final int TITLE_MAX = 150;
    private static final BigDecimal PRICE_LIMIT = new BigDecimal("100000000");

    private final CourseDAO courses;

    public CourseServiceImpl(CourseDAO courses) {
        this.courses = courses;
    }

    @Override
    public Page<Course> list(int page) {
        long total = courses.count();
        int current = Pagination.clamp(page, total);
        List<Course> items = courses.findPage(Pagination.offset(current), Pagination.PAGE_SIZE);
        return Page.of(items, current, Pagination.PAGE_SIZE, total);
    }

    @Override
    public List<Course> listAll() {
        return courses.listAll();
    }

    @Override
    public Course find(int id) {
        Course course = courses.findById(id);
        if (course == null) {
            throw new NotFoundException("Curso não encontrado.");
        }
        return course;
    }

    @Override
    public void create(Course course) {
        validate(course);
        courses.save(course);
    }

    @Override
    public void update(Course course) {
        find(course.getId());
        validate(course);
        courses.update(course);
    }

    @Override
    public void delete(int id) {
        courses.delete(find(id));
    }

    private static void validate(Course course) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (course.getTitle() == null || course.getTitle().isBlank()) {
            errors.put("title", "Informe o título.");
        } else if (course.getTitle().codePointCount(0, course.getTitle().length()) > TITLE_MAX) {
            errors.put("title", "O título deve ter no máximo " + TITLE_MAX + " caracteres.");
        }
        if (course.getDescription() == null || course.getDescription().isBlank()) {
            errors.put("description", "Informe a descrição.");
        }
        BigDecimal price = course.getPrice();
        if (price == null || price.signum() < 0 || price.stripTrailingZeros().scale() > 2) {
            errors.put("price", "Informe um preço válido (número maior ou igual a zero, com até 2 casas decimais).");
        } else if (price.compareTo(PRICE_LIMIT) >= 0) {
            errors.put("price", "O preço deve ser no máximo 99999999,99.");
        }
        if (course.getStartDate() == null) {
            errors.put("startDate", "Informe a data de início.");
        }
        if (course.getLevel() == null) {
            errors.put("level", "Informe o nível.");
        }
        if (course.getInstructor() == null || course.getInstructor().getId() <= 0) {
            errors.put("instructor", "Informe o instrutor.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
