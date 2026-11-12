package crudmanager.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crudmanager.domain.Page;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Company;
import crudmanager.domain.model.User;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompanyServiceTest {

    private final FakeCompanyDAO companies = new FakeCompanyDAO();
    private final CompanyService service = new CompanyService(companies);

    private Company valid() {
        Company c = new Company();
        c.setName("Acme");
        c.setRole("Dev");
        c.setStart(LocalDate.of(2020, 1, 1));
        c.setEnd(LocalDate.of(2021, 1, 1));
        c.setUser(new User(1));
        return c;
    }

    @Test
    @DisplayName("ARQ-06 empresa válida é criada, com saída opcional")
    void createValid() {
        service.create(valid());
        Company open = valid();
        open.setEnd(null);
        service.create(open);
        assertEquals(2, companies.count());
    }

    @Test
    @DisplayName("ARQ-06 nome, cargo, entrada e usuário são obrigatórios")
    void requiredFields() {
        Company c = new Company();
        ValidationException e = assertThrows(ValidationException.class, () -> service.create(c));
        assertTrue(e.getErrors().keySet().containsAll(java.util.List.of("name", "role", "start", "user")));
        assertEquals(0, companies.count());
    }

    @Test
    @DisplayName("ARQ-06 saída anterior à entrada é rejeitada")
    void endBeforeStart() {
        Company c = valid();
        c.setEnd(LocalDate.of(2019, 12, 31));
        ValidationException e = assertThrows(ValidationException.class, () -> service.create(c));
        assertTrue(e.getErrors().containsKey("end"));
    }

    @Test
    @DisplayName("ARQ-06 saída igual à entrada é aceita")
    void endEqualsStart() {
        Company c = valid();
        c.setEnd(c.getStart());
        service.create(c);
        assertEquals(1, companies.count());
    }

    @Test
    @DisplayName("ARQ-06 nome e cargo acima de 128 caracteres são rejeitados")
    void tooLong() {
        Company c = valid();
        c.setName("x".repeat(129));
        c.setRole("y".repeat(129));
        ValidationException e = assertThrows(ValidationException.class, () -> service.create(c));
        assertTrue(e.getErrors().containsKey("name"));
        assertTrue(e.getErrors().containsKey("role"));
    }

    @Test
    @DisplayName("ARQ-06 find, update e delete de id inexistente lançam NotFoundException")
    void notFound() {
        assertThrows(NotFoundException.class, () -> service.find(5));
        assertThrows(NotFoundException.class, () -> service.delete(5));
        Company c = new Company(5);
        assertThrows(NotFoundException.class, () -> service.update(c));
    }

    @Test
    @DisplayName("ARQ-06 update e delete de empresa existente")
    void updateAndDelete() {
        Company stored = new Company(3);
        companies.store.put(3, stored);
        Company edited = new Company(3);
        edited.setName("Nova");
        edited.setRole("Dev");
        edited.setStart(LocalDate.of(2020, 1, 1));
        edited.setUser(new User(1));
        service.update(edited);
        assertEquals("Nova", companies.findById(3).getName());
        service.delete(3);
        assertEquals(0, companies.count());
    }

    @Test
    @DisplayName("ARQ-06 list(page) pagina e clampa")
    void list() {
        for (int i = 1; i <= 11; i++) {
            companies.store.put(i, new Company(i));
        }
        Page<Company> p = service.list(2);
        assertEquals(1, p.getItems().size());
        assertEquals(2, service.list(50).getPage());
    }
}
