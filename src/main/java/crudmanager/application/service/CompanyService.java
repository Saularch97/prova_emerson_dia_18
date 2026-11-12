package crudmanager.application.service;

import crudmanager.application.port.in.CompanyUseCase;
import crudmanager.application.port.out.CompanyDAO;
import crudmanager.domain.Page;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Company;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CompanyService implements CompanyUseCase {

    private static final int MAX_LENGTH = 128;

    private final CompanyDAO companies;

    public CompanyService(CompanyDAO companies) {
        this.companies = companies;
    }

    @Override
    public Page<Company> list(int page) {
        long total = companies.count();
        int current = Pagination.clamp(page, total);
        List<Company> items = companies.findPage(Pagination.offset(current), Pagination.PAGE_SIZE);
        return Page.of(items, current, Pagination.PAGE_SIZE, total);
    }

    @Override
    public Company find(int id) {
        Company company = companies.findById(id);
        if (company == null) {
            throw new NotFoundException("Empresa não encontrada.");
        }
        return company;
    }

    @Override
    public void create(Company company) {
        validate(company);
        companies.save(company);
    }

    @Override
    public void update(Company company) {
        find(company.getId());
        validate(company);
        companies.update(company);
    }

    @Override
    public void delete(int id) {
        companies.delete(find(id));
    }

    private static void validate(Company company) {
        Map<String, String> errors = new LinkedHashMap<>();
        text(errors, "name", "o nome", company.getName());
        text(errors, "role", "o cargo", company.getRole());
        if (company.getStart() == null) {
            errors.put("start", "Informe a data de entrada.");
        }
        if (company.getStart() != null && company.getEnd() != null && company.getEnd().isBefore(company.getStart())) {
            errors.put("end", "A data de saída não pode ser anterior à entrada.");
        }
        if (company.getUser() == null || company.getUser().getId() <= 0) {
            errors.put("user", "Informe o usuário.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static void text(Map<String, String> errors, String field, String label, String value) {
        if (value == null || value.isBlank()) {
            errors.put(field, "Informe " + label + ".");
        } else if (value.length() > MAX_LENGTH) {
            errors.put(field, "Máximo de " + MAX_LENGTH + " caracteres.");
        }
    }
}
