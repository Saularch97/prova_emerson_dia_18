# Plataforma de Cursos Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, the walk, check_proofs, the Verifier and the screens sub-agent).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/plataforma-cursos/design.md`
**Status**: Approved
**Approved**: 2026-11-12 - user: "aprovado"
**Build**: none - o projeto só ganha build na T1 (pom.xml) e T56 (docker-compose.yml); a T56 restaura `mvn -q -DskipTests package; docker compose config -q`
**Run all**: `mvn verify`
**Run smoke**: `bash scripts/smoke.sh`

---

## Test Coverage Matrix

> Generated from the codebase, the spec and the design - confirm before Execute. Guidelines found: none (the base project has no tests, README or CI) - strong defaults applied. Tests carry the criterion ID in `@DisplayName` (AD-003).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Services, `Pagination`, `Page` (`application`, `domain`) | unit | All branches; 1:1 to spec ACs; every listed edge case | `src/test/java/crudmanager/application/**/*Test.java` | `mvn -q test` |
| Web adapter (Servlets, filters, `WebUtil`) | unit (Mockito) | Every route a task adds: happy path + edge + error paths | `src/test/java/crudmanager/adapter/in/web/*Test.java` | `mvn -q test` |
| Security adapter | unit | Hash format, match, mismatch, malformed input | `src/test/java/crudmanager/adapter/out/security/*Test.java` | `mvn -q test` |
| Persistence adapter (`Jdbc*DAO`, `DAOFactory`) | integration (Testcontainers PostgreSQL 16) | Key query paths + FK/unique errors + paging | `src/test/java/crudmanager/adapter/out/persistence/*IT.java` | `mvn -q verify` |
| Architecture rules | unit | Package layout, forbidden imports, no MySQL | `src/test/java/crudmanager/ArchitectureTest.java` | `mvn -q test` |
| JSPs (views) | e2e smoke (curl against compose) | Rendered fields, messages, controls per criterion | `scripts/smoke.sh` | `bash scripts/smoke.sh` |
| Models, ports, `pom.xml`, SQL, Docker files | none | - (build gate only) | - | build gate |

## Gate Check Commands

> Generated from the codebase - confirm before Execute. The project has no linter configured.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | After tasks with unit tests only | `mvn -q test` |
| Full | After tasks with integration tests (needs Docker) | `mvn -q verify` |
| Smoke | After tasks that add smoke cases | `bash scripts/smoke.sh` |
| Build | After phase completion or config/model/view-only tasks | `mvn -q -DskipTests package; docker compose config -q` |

---

## Execution Plan

Phases run in order; tasks within a phase run in order.

### Phase 1: Build e banco

```
T1 → T2 → T3 → T4 → T5
```

### Phase 2: Domínio e portas

```
T6 → T7 → T8 → T9 → T10 → T11
```

### Phase 3: Serviços

```
T12 → T13 → T14 → T15 → T16 → T17 → T18
```

### Phase 4: Persistência

```
T19 → T20 → T21 → T22 → T23 → T24 → T25 → T26 → T27
```

### Phase 5: Base web

```
T28 → T29 → T30 → T31 → T32 → T33 → T34 → T35 → T36 → T37
```

### Phase 6: CRUD de Usuários e Posts

```
T38 → T39 → T40 → T41 → T42 → T43
```

### Phase 7: CRUD de Empresas, Cursos e Matrículas

```
T44 → T45 → T46 → T47 → T48 → T49 → T50 → T51 → T52
```

### Phase 8: Limpeza, Docker e smoke

```
T53 → T54 → T55 → T56 → T57 → T58 → T59 → T60 → T61
```

---

## Task Breakdown

### T1: Criar pom.xml e remover bibliotecas legadas

**What**: Maven WAR (JDK 21) com servlet/JSP provided, JSTL 3, driver PostgreSQL, JUnit 5, Mockito, Testcontainers, Surefire e Failsafe; remover `WEB-INF/lib` e o driver MySQL.
**Where**: `pom.xml`
**Depends on**: None
**Reuses**: `src/main/webapp/WEB-INF/lib/`
**Requirement**: DOCK-07

**Done when**:

- [x] `mvn -q -DskipTests package` gera `target/crud-manager.war`
- [x] `WEB-INF/lib/` removido do repositório
- [x] Nenhuma referência a MySQL no `pom.xml`
- [x] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `build: pom.xml Maven e WAR crud-manager`

---

### T2: Criar listener de relatório de testes

**What**: Listener JUnit (ServiceLoader) que imprime `✓ <displayName>` por teste aprovado (AD-003), com um teste que o prova.
**Where**: `src/test/java/crudmanager/TestReportListener.java`
**Depends on**: T1
**Reuses**: -
**Requirement**: AD-003

**Done when**:

- [x] `mvn -q test` imprime `✓ AD-003 listener imprime teste aprovado`
- [x] Registro em `META-INF/services/org.junit.platform.launcher.TestExecutionListener` criado junto
- [x] Teste falho não imprime a linha `✓`
- [x] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `test: listener que imprime checks aprovados`

---

### T3: Converter script-sql.sql para PostgreSQL

**What**: Script com `users` (+`password_hash`), `posts`, `companies`, `courses`, `enrollments`, constraints e seed (5 usuários com hash de `senha123`, posts, empresas, cursos, matrículas e volume suficiente para paginar).
**Where**: `script-sql.sql`
**Depends on**: T2
**Reuses**: `script-sql.sql`
**Requirement**: DB-01, DOCK-02

**Done when**:

- [x] Script roda limpo num `postgres:16` vazio
- [x] Seed tem mais de 10 registros em cada listagem
- [x] Hash do seed no formato `iterações:saltB64:hashB64` do `Pbkdf2PasswordHasher`
- [x] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(db): script PostgreSQL com tabelas courses e enrollments`

---

### T4: Criar Page do domínio

**What**: `Page<T>` imutável com `items`, `page`, `pageSize`, `totalItems`, `totalPages()`, `hasPrevious()`, `hasNext()`.
**Where**: `src/main/java/crudmanager/domain/Page.java`
**Depends on**: T3
**Reuses**: -
**Requirement**: PAG-03, PAG-04

**Done when**:

- [x] Testes cobrem 0 itens, 1 página, várias páginas, última página parcial
- [x] `mvn -q test` passa
- [x] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(domain): adiciona Page`

---

### T5: Criar Pagination do application

**What**: `Pagination` com `PAGE_SIZE=10`, `parse(String)` (ausente, não numérico ou menor que 1 → 1) e `clamp(page, totalItems)` (acima da última → última).
**Where**: `src/main/java/crudmanager/application/service/Pagination.java`
**Depends on**: T4
**Reuses**: -
**Requirement**: PAG-01, PAG-02, PAG-05, PAG-06

**Done when**:

- [x] Testes `PAG-01`, `PAG-02`, `PAG-05`, `PAG-06` passam
- [x] Offset calculado como `(page-1)*10`
- [x] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(app): regra de paginação`

---

### T6: Criar erros de domínio

**What**: `ValidationException` (mapa campo→mensagem), `NotFoundException`, `DuplicateException`, `RelatedDataException`, `ModelException`.
**Where**: `src/main/java/crudmanager/domain/error/`
**Depends on**: None (previous phase complete)
**Reuses**: -
**Requirement**: ARQ-01

**Done when**:

- [x] Cinco classes unchecked/checked conforme o design
- [x] Compila
- [x] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(domain): erros de negócio`

---

### T7: Migrar modelos User, Post e Company

**What**: POJOs em `domain.model` com `LocalDate`; `User.passwordHash`.
**Where**: `src/main/java/crudmanager/domain/model/`
**Depends on**: T6
**Reuses**: `src/main/java/model/*.java`
**Requirement**: ARQ-06, AUTH-10

**Done when**:

- [x] Getters/setters JavaBean compatíveis com EL
- [x] Sem import de `java.sql` nem `java.util.Date`
- [x] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `refactor(domain): migra User, Post e Company`

---

### T8: Criar modelo Course

**What**: `Course` e enum `Level` (`INICIANTE`, `INTERMEDIARIO`, `AVANCADO`) com rótulos em português.
**Where**: `src/main/java/crudmanager/domain/model/Course.java`
**Depends on**: T7
**Reuses**: -
**Requirement**: CURSO-02

**Done when**:

- [x] Campos do spec: título, descrição, preço `BigDecimal`, início `LocalDate`, nível, ativo, instrutor `User`
- [x] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(domain): adiciona Course`

---

### T9: Criar modelo Enrollment

**What**: `Enrollment` e enum `Status` (`ATIVA`, `CONCLUIDA`, `CANCELADA`) com rótulos.
**Where**: `src/main/java/crudmanager/domain/model/Enrollment.java`
**Depends on**: T8
**Reuses**: -
**Requirement**: MATR-02

**Done when**:

- [x] Campos do spec: aluno `User`, curso `Course`, data, situação, progresso, pago
- [x] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(domain): adiciona Enrollment`

---

### T10: Criar portas de saída

**What**: Interfaces `UserDAO`, `PostDAO`, `CompanyDAO`, `CourseDAO`, `EnrollmentDAO` (`save`, `update`, `delete`, `findById`, `listAll`, `findPage`, `count`, mais `findByEmail`, `existsByStudentAndCourse`) e `PasswordHasher`.
**Where**: `src/main/java/crudmanager/application/port/out/`
**Depends on**: T9
**Reuses**: -
**Requirement**: ARQ-04

**Done when**:

- [x] Assinaturas conforme design
- [x] Sem imports de `java.sql`
- [x] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(app): portas de saída`

---

### T11: Criar portas de entrada

**What**: Interfaces `UserUseCase`, `PostUseCase`, `CompanyUseCase`, `CourseUseCase`, `EnrollmentUseCase`, `AuthUseCase`.
**Where**: `src/main/java/crudmanager/application/port/in/`
**Depends on**: T10
**Reuses**: -
**Requirement**: ARQ-05

**Done when**:

- [x] `list(int page)`, `find`, `create`, `update`, `delete`; `AuthUseCase.login`
- [x] Sem imports de `jakarta.servlet`
- [x] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(app): portas de entrada`

---

### T12: Implementar Pbkdf2PasswordHasher

**What**: Hash PBKDF2WithHmacSHA256, 210000 iterações, salt de 16 bytes, formato `iterações:saltB64:hashB64`, comparação em tempo constante.
**Where**: `src/main/java/crudmanager/adapter/out/security/Pbkdf2PasswordHasher.java`
**Depends on**: None (previous phase complete)
**Reuses**: -
**Requirement**: AUTH-10

**Done when**:

- [x] Teste `AUTH-10`: dois hashes da mesma senha diferem, o hash não contém a senha, `matches` aceita a certa e recusa a errada, formato inválido recusa sem exceção
- [x] `mvn -q test` passa
- [x] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(security): hash PBKDF2 de senha`

---

### T13: Implementar AuthService

**What**: `login(email, password)`: vazio → `ValidationException` com "Informe e-mail e senha"; desconhecido ou senha errada → `Optional.empty()`; sucesso → usuário.
**Where**: `src/main/java/crudmanager/application/service/AuthService.java`
**Depends on**: T12
**Reuses**: -
**Requirement**: AUTH-03, AUTH-04, AUTH-05

**Done when**:

- [x] Testes com `UserDAO` e `PasswordHasher` falsos para os três casos
- [x] Mesmo resultado para e-mail inexistente e senha errada
- [x] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(app): adiciona AuthService`

---

### T14: Implementar UserService

**What**: CRUD de usuários com validação, senha obrigatória no create, em branco no update mantém o hash, delete com FK → `RelatedDataException`, `list(page)` paginado.
**Where**: `src/main/java/crudmanager/application/service/UserService.java`
**Depends on**: T13
**Reuses**: -
**Requirement**: AUTH-12, AUTH-13, ARQ-03, EDGE-1

**Done when**:

- [x] Testes `AUTH-12`, `AUTH-13`, `EDGE-1` passam
- [x] Hash gravado, nunca a senha
- [x] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(app): adiciona UserService`

---

### T15: Implementar PostService

**What**: CRUD de posts com validação e paginação.
**Where**: `src/main/java/crudmanager/application/service/PostService.java`
**Depends on**: T14
**Reuses**: -
**Requirement**: ARQ-06

**Done when**:

- [ ] Testes: conteúdo vazio rejeitado, usuário obrigatório, `list(page)` clampa
- [ ] `mvn -q test` passa
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(app): adiciona PostService`

---

### T16: Implementar CompanyService

**What**: CRUD de empresas com validação (saída não anterior à entrada) e paginação.
**Where**: `src/main/java/crudmanager/application/service/CompanyService.java`
**Depends on**: T15
**Reuses**: -
**Requirement**: ARQ-06

**Done when**:

- [ ] Testes: campos obrigatórios, datas inválidas, `list(page)`
- [ ] `mvn -q test` passa
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(app): adiciona CompanyService`

---

### T17: Implementar CourseService

**What**: Validação (título ≤150, descrição, preço ≥0, início, instrutor), CRUD, `NotFoundException`, FK → `RelatedDataException`, `list(page)`.
**Where**: `src/main/java/crudmanager/application/service/CourseService.java`
**Depends on**: T16
**Reuses**: -
**Requirement**: CURSO-03, CURSO-05, CURSO-06, CURSO-07, CURSO-08, CURSO-09, CURSO-12

**Done when**:

- [ ] Um teste por critério com o ID no `@DisplayName`
- [ ] Mensagem por campo inválido
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(app): adiciona CourseService`

---

### T18: Implementar EnrollmentService

**What**: Validação (aluno, curso, data, situação, progresso 0–100), concluída exige 100, par repetido → `DuplicateException`, CRUD, `list(page)`.
**Where**: `src/main/java/crudmanager/application/service/EnrollmentService.java`
**Depends on**: T17
**Reuses**: -
**Requirement**: MATR-03, MATR-05, MATR-06, MATR-07, MATR-08, MATR-09, MATR-12

**Done when**:

- [ ] Um teste por critério com o ID no `@DisplayName`
- [ ] Duplicidade ignora a própria matrícula na edição
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(app): adiciona EnrollmentService`

---

### T19: Criar JdbcConnectionFactory

**What**: Lê `DB_URL`, `DB_USER`, `DB_PASSWORD` de um mapa de ambiente injetável; variável ausente → `ModelException` clara; sem defaults de credencial.
**Where**: `src/main/java/crudmanager/adapter/out/persistence/JdbcConnectionFactory.java`
**Depends on**: None (previous phase complete)
**Reuses**: `MySQLConnectionFactory.java`
**Requirement**: DOCK-03

**Done when**:

- [ ] Teste `DOCK-03`: sem `DB_PASSWORD` falha com mensagem; com as três lê os valores
- [ ] Nenhuma credencial literal no código
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(db): conexão por variáveis de ambiente`

---

### T20: Migrar DBHandler

**What**: `DBHandler` em `adapter.out.persistence` com `setBoolean`, `setDecimal`, `setNull`, `getBoolean`, `getDecimal`, `getLong`, `close()` público e tradução de SQLState 23503/23505.
**Where**: `src/main/java/crudmanager/adapter/out/persistence/DBHandler.java`
**Depends on**: T19
**Reuses**: `DBHandler.java`
**Requirement**: ARQ-04

**Done when**:

- [ ] Usa `JdbcConnectionFactory`
- [ ] Compila; coberto pelos ITs seguintes
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `refactor(db): adiciona DBHandler para PostgreSQL`

---

### T21: Criar base de IT e SchemaIT

**What**: Base Testcontainers (PostgreSQL 16 carregando `script-sql.sql`) e `SchemaIT` que consulta `information_schema`.
**Where**: `src/test/java/crudmanager/adapter/out/persistence/SchemaIT.java`
**Depends on**: T20
**Reuses**: -
**Requirement**: DB-01

**Done when**:

- [ ] Teste `DB-01`: FK `courses.instructor_id`, `enrollments.student_id`, `enrollments.course_id`, unique `(student_id, course_id)`, coluna `users.password_hash`
- [ ] `mvn -q verify` passa
- [ ] Gate passes: `mvn -q verify`

**Tests**: integration
**Gate**: full
**Commit**: `test(db): schema do script SQL`

---

### T22: Implementar JdbcUserDAO

**What**: CRUD, `findByEmail`, `findPage`, `count`, violação de FK → `RelatedDataException`.
**Where**: `src/main/java/crudmanager/adapter/out/persistence/JdbcUserDAO.java`
**Depends on**: T21
**Reuses**: `MySQLUserDAO.java`
**Requirement**: AUTH-12, EDGE-1

**Done when**:

- [ ] IT: salvar com hash, achar por e-mail, paginar, FK vira `RelatedDataException`
- [ ] Gate passes: `mvn -q verify`

**Tests**: integration
**Gate**: full
**Commit**: `feat(db): adiciona JdbcUserDAO`

---

### T23: Implementar JdbcPostDAO

**What**: CRUD, `CURRENT_DATE`, `findPage`, `count`, JOIN com usuário.
**Where**: `src/main/java/crudmanager/adapter/out/persistence/JdbcPostDAO.java`
**Depends on**: T22
**Reuses**: `MySQLPostDAO.java`
**Requirement**: ARQ-06

**Done when**:

- [ ] IT cobre save, update, delete, findPage, count
- [ ] `mvn -q verify` passa
- [ ] Gate passes: `mvn -q verify`

**Tests**: integration
**Gate**: full
**Commit**: `feat(db): adiciona JdbcPostDAO`

---

### T24: Implementar JdbcCompanyDAO

**What**: CRUD com colunas `"start"`/`"end"`, `findPage`, `count`, JOIN com usuário.
**Where**: `src/main/java/crudmanager/adapter/out/persistence/JdbcCompanyDAO.java`
**Depends on**: T23
**Reuses**: `MySQLCompanyDAO.java`
**Requirement**: ARQ-06

**Done when**:

- [ ] IT cobre save (com e sem saída), update, delete, findPage, count
- [ ] `mvn -q verify` passa
- [ ] Gate passes: `mvn -q verify`

**Tests**: integration
**Gate**: full
**Commit**: `feat(db): adiciona JdbcCompanyDAO`

---

### T25: Implementar JdbcCourseDAO

**What**: CRUD, `BigDecimal`, boolean, JOIN com instrutor, `findPage`, `count`, FK → `RelatedDataException`.
**Where**: `src/main/java/crudmanager/adapter/out/persistence/JdbcCourseDAO.java`
**Depends on**: T24
**Reuses**: -
**Requirement**: CURSO-07, CURSO-10, EDGE-3

**Done when**:

- [ ] IT: excluir curso com matrícula → `RelatedDataException`; `active=false` persiste; descrição com acentos e emoji volta idêntica (`EDGE-3`)
- [ ] `mvn -q verify` passa
- [ ] Gate passes: `mvn -q verify`

**Tests**: integration
**Gate**: full
**Commit**: `feat(db): adiciona JdbcCourseDAO`

---

### T26: Implementar JdbcEnrollmentDAO

**What**: CRUD, JOIN com aluno e curso, `existsByStudentAndCourse(student, course, ignoreId)`, unique → `DuplicateException`, `findPage`, `count`.
**Where**: `src/main/java/crudmanager/adapter/out/persistence/JdbcEnrollmentDAO.java`
**Depends on**: T25
**Reuses**: -
**Requirement**: MATR-07, MATR-10

**Done when**:

- [ ] IT: par repetido rejeitado, edição da própria não conflita, `paid=false` persiste
- [ ] `mvn -q verify` passa
- [ ] Gate passes: `mvn -q verify`

**Tests**: integration
**Gate**: full
**Commit**: `feat(db): adiciona JdbcEnrollmentDAO`

---

### T27: Criar DAOFactory e UseCases

**What**: `DAOFactory.createDAO(Class)` com os cinco DAOs (criação sem conectar) e `UseCases` (composition root que monta os serviços).
**Where**: `src/main/java/crudmanager/adapter/out/persistence/DAOFactory.java`
**Depends on**: T26
**Reuses**: `src/main/java/model/dao/DAOFactory.java`
**Requirement**: ARQ-04

**Done when**:

- [ ] Teste `ARQ-04`: `createDAO` devolve implementação JDBC para cada interface, sem abrir conexão
- [ ] `UseCases` criado em `adapter/in/web/UseCases.java` junto
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(app): adiciona DAOFactory e composition root`

---

### T28: Criar WebUtil

**What**: Mensagens flash por sessão, parse de `page`, leitura segura de id (não numérico → `NotFoundException`), ações de rota por `getServletPath()`.
**Where**: `src/main/java/crudmanager/adapter/in/web/WebUtil.java`
**Depends on**: None (previous phase complete)
**Reuses**: `ControllerUtil.java`
**Requirement**: EDGE-2

**Done when**:

- [ ] Testes: id `abc` e id vazio viram `NotFoundException`; flash vai da sessão para a request uma vez
- [ ] `mvn -q test` passa
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(web): adiciona WebUtil`

---

### T29: Migrar CharacterEncodingFilter

**What**: Filtro UTF-8 em `adapter.in.web`, sem `@WebFilter` (ordem no `web.xml`).
**Where**: `src/main/java/crudmanager/adapter/in/web/CharacterEncodingFilter.java`
**Depends on**: T28
**Reuses**: `controller/CharacterEncodingFilter.java`
**Requirement**: EDGE-3

**Done when**:

- [ ] Teste: define encoding UTF-8 na request e na response
- [ ] `mvn -q test` passa
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `refactor(web): move CharacterEncodingFilter`

---

### T30: Configurar web.xml

**What**: Ordem dos filtros (encoding, depois auth), `error-page` 500 para `/error.jsp`, welcome file.
**Where**: `src/main/webapp/WEB-INF/web.xml`
**Depends on**: T29
**Reuses**: `WEB-INF/web.xml`
**Requirement**: DOCK-06

**Done when**:

- [ ] Filtros mapeados em `/*` na ordem correta
- [ ] `error-page` para `java.lang.Throwable` e 500
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): web.xml com filtros e página de erro`

---

### T31: Criar error.jsp

**What**: Página genérica de erro, sem stack trace.
**Where**: `src/main/webapp/error.jsp`
**Depends on**: T30
**Reuses**: -
**Requirement**: DOCK-06

**Done when**:

- [ ] Mostra mensagem genérica e link para a home
- [ ] Não imprime `exception` nem `stackTrace`
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): error.jsp`

---

### T32: Implementar AuthFilter

**What**: Redireciona para `/login` sem sessão autenticada; libera `/login`, `/css/`, `/js/`, `/fonts/`, `/images/`; POST sem sessão não chega ao controller.
**Where**: `src/main/java/crudmanager/adapter/in/web/AuthFilter.java`
**Depends on**: T31
**Reuses**: -
**Requirement**: AUTH-01, AUTH-09, EDGE-4, EDGE-5

**Done when**:

- [ ] Testes `AUTH-01`, `AUTH-09`, `EDGE-4`, `EDGE-5` com Mockito
- [ ] `mvn -q test` passa
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(auth): adiciona AuthFilter`

---

### T33: Implementar LoginController

**What**: `GET /login` (forward, ou redireciona para `/` se logado), `POST /login` (sucesso: `changeSessionId`, guarda usuário, redirect `/`; falha: reexibe com mensagem), `GET /logout` (invalida e redireciona).
**Where**: `src/main/java/crudmanager/adapter/in/web/LoginController.java`
**Depends on**: T32
**Reuses**: -
**Requirement**: AUTH-03, AUTH-04, AUTH-05, AUTH-07, AUTH-08, AUTH-11

**Done when**:

- [ ] Testes `AUTH-03`, `AUTH-04`, `AUTH-05`, `AUTH-07`, `AUTH-08`, `AUTH-11` com Mockito e `AuthUseCase` falso
- [ ] `mvn -q test` passa
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(auth): adiciona LoginController`

---

### T34: Criar login.jsp

**What**: Página de login com e-mail, senha, botão "Entrar" e área de mensagem.
**Where**: `src/main/webapp/login.jsp`
**Depends on**: T33
**Reuses**: -
**Requirement**: AUTH-02

**Done when**:

- [ ] Campos `email` e `password`, botão "Entrar"
- [ ] Mostra `${message}`; sem menu de navegação
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(auth): tela de login`

---

### T35: Atualizar nav-menu.jsp

**What**: Links Início, Usuários, Posts (`/posts`), Empresas, Cursos, Matrículas; Perfil mostra o nome logado e "Sair" para `/logout`.
**Where**: `src/main/webapp/nav-menu.jsp`
**Depends on**: T34
**Reuses**: `nav-menu.jsp`
**Requirement**: NAV-01, NAV-02, NAV-03, AUTH-06

**Done when**:

- [ ] Cinco links de cadastro com `contextPath`
- [ ] Nome do usuário e "Sair" funcionais
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): cabeçalho com Cursos, Matrículas e logout`

---

### T36: Criar pagination.jsp

**What**: Fragmento incluído nas listagens: Anterior, números, Próximo, atual destacada, desabilitados nas pontas, oculto com uma página.
**Where**: `src/main/webapp/pagination.jsp`
**Depends on**: T35
**Reuses**: -
**Requirement**: PAG-03, PAG-04, PAG-08

**Done when**:

- [ ] Usa os atributos `pageData` e `pageBaseUrl`
- [ ] Compila no Tomcat (verificado pelo smoke)
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): componente de paginação`

---

### T37: Atualizar modal.html

**What**: Modal de exclusão envia também o campo oculto `page` da listagem.
**Where**: `src/main/webapp/modal.html`
**Depends on**: T36
**Reuses**: `modal.html`
**Requirement**: PAG-07

**Done when**:

- [ ] Campo `<input type="hidden" name="page">` no form
- [ ] JS das listagens preenche `page`
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): modal de exclusão preserva a página`

---

### T38: Migrar UsersController

**What**: Servlet sobre `UserUseCase`, rotas por `getServletPath()`, senha no formulário, página preservada ao excluir, erros mapeados em mensagens.
**Where**: `src/main/java/crudmanager/adapter/in/web/UsersController.java`
**Depends on**: None (previous phase complete)
**Reuses**: `controller/UsersController.java`
**Requirement**: ARQ-05, ARQ-06, AUTH-12, AUTH-13, EDGE-2

**Done when**:

- [ ] Testes Mockito: list repassa `page`, insert chama `create`, delete redireciona para `/users?page=N`
- [ ] `mvn -q test` passa
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `refactor(web): adiciona UsersController hexagonal`

---

### T39: Atualizar users.jsp

**What**: Listagem paginada de usuários com `pagination.jsp` e `page` no modal.
**Where**: `src/main/webapp/users.jsp`
**Depends on**: T38
**Reuses**: `users.jsp`
**Requirement**: PAG-03, ARQ-06

**Done when**:

- [ ] Itera `page.items`; inclui `pagination.jsp`
- [ ] Mensagem de lista vazia
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): users.jsp paginado`

---

### T40: Atualizar form-user.jsp

**What**: Campo senha (obrigatório no cadastro, opcional na edição).
**Where**: `src/main/webapp/form-user.jsp`
**Depends on**: T39
**Reuses**: `form-user.jsp`
**Requirement**: AUTH-12, AUTH-13

**Done when**:

- [ ] `<input type="password" name="password">` com `required` só no insert
- [ ] Erros por campo exibidos
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): campo senha no formulário de usuário`

---

### T41: Migrar PostsController

**What**: Servlet sobre `PostUseCase`, mapeado em `""` e `/posts`.
**Where**: `src/main/java/crudmanager/adapter/in/web/PostsController.java`
**Depends on**: T40
**Reuses**: `controller/PostsController.java`
**Requirement**: ARQ-05, ARQ-06, NAV-03, EDGE-2

**Done when**:

- [ ] Testes Mockito para list, insert, update, delete com `page`
- [ ] `mvn -q test` passa
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `refactor(web): adiciona PostsController hexagonal`

---

### T42: Atualizar index.jsp

**What**: Listagem paginada de posts (home) com `pagination.jsp`.
**Where**: `src/main/webapp/index.jsp`
**Depends on**: T41
**Reuses**: `index.jsp`
**Requirement**: PAG-03, ARQ-06

**Done when**:

- [ ] Itera `page.items`; paginação incluída
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): posts paginados`

---

### T43: Atualizar form-post.jsp

**What**: Formulário de post com erros por campo.
**Where**: `src/main/webapp/form-post.jsp`
**Depends on**: T42
**Reuses**: `form-post.jsp`
**Requirement**: ARQ-06

**Done when**:

- [ ] Select de usuários preenchido pelo controller
- [ ] Erros por campo exibidos
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): form-post com validação`

---

### T44: Migrar CompaniesController

**What**: Servlet sobre `CompanyUseCase`.
**Where**: `src/main/java/crudmanager/adapter/in/web/CompaniesController.java`
**Depends on**: None (previous phase complete)
**Reuses**: `controller/CompaniesController.java`
**Requirement**: ARQ-05, ARQ-06, EDGE-2

**Done when**:

- [ ] Testes Mockito para list, insert, update, delete com `page`
- [ ] `mvn -q test` passa
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `refactor(web): adiciona CompaniesController hexagonal`

---

### T45: Atualizar companies.jsp

**What**: Listagem paginada de empresas.
**Where**: `src/main/webapp/companies.jsp`
**Depends on**: T44
**Reuses**: `companies.jsp`
**Requirement**: PAG-03, ARQ-06

**Done when**:

- [ ] Itera `page.items`; paginação incluída; contexto sem caminho fixo
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): companies.jsp paginado`

---

### T46: Atualizar form-company.jsp

**What**: Formulário de empresa com erros por campo.
**Where**: `src/main/webapp/form-company.jsp`
**Depends on**: T45
**Reuses**: `form-company.jsp`
**Requirement**: ARQ-06

**Done when**:

- [ ] Erros por campo exibidos
- [ ] Select de usuários mantido
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): form-company com validação`

---

### T47: Implementar CoursesController

**What**: Rotas `/courses`, `/course/{form,insert,update,delete}`; checkbox ausente → `active=false`; selects de instrutor; mensagens do spec.
**Where**: `src/main/java/crudmanager/adapter/in/web/CoursesController.java`
**Depends on**: T46
**Reuses**: -
**Requirement**: CURSO-03, CURSO-05, CURSO-06, CURSO-07, CURSO-08, CURSO-10, CURSO-12, ARQ-05, PAG-07

**Done when**:

- [ ] Testes Mockito com IDs: `CURSO-03`, `CURSO-05`, `CURSO-06`, `CURSO-07`, `CURSO-08` (reexibe com valores), `CURSO-10`, `CURSO-12`, `PAG-07`, `ARQ-05`
- [ ] `mvn -q test` passa
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(web): adiciona CoursesController`

---

### T48: Criar courses.jsp

**What**: Listagem de cursos: título, nível, preço, início, ativo, instrutor, Editar, Excluir, "Nenhum curso cadastrado.", paginação.
**Where**: `src/main/webapp/courses.jsp`
**Depends on**: T47
**Reuses**: -
**Requirement**: CURSO-01, CURSO-11, PAG-03

**Done when**:

- [ ] Colunas e ações do spec
- [ ] Modal de exclusão com `page`
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): courses.jsp`

---

### T49: Criar form-course.jsp

**What**: Formulário: text, textarea, number, date, select de nível, checkbox ativo, select de instrutor; preenchido na edição; erros por campo.
**Where**: `src/main/webapp/form-course.jsp`
**Depends on**: T48
**Reuses**: -
**Requirement**: CURSO-02, CURSO-04, CURSO-08

**Done when**:

- [ ] Sete campos com os tipos HTML do spec
- [ ] Instrutor e nível pré-selecionados na edição
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): form-course`

---

### T50: Implementar EnrollmentsController

**What**: Rotas `/enrollments`, `/enrollment/{form,insert,update,delete}`; radio de situação, range de progresso; selects de aluno e curso.
**Where**: `src/main/java/crudmanager/adapter/in/web/EnrollmentsController.java`
**Depends on**: T49
**Reuses**: -
**Requirement**: MATR-03, MATR-05, MATR-06, MATR-07, MATR-08, MATR-09, MATR-10, MATR-12, EDGE-2

**Done when**:

- [ ] Testes Mockito com IDs `MATR-03`…`MATR-12` do spec
- [ ] `mvn -q test` passa
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `feat(web): adiciona EnrollmentsController`

---

### T51: Criar enrollments.jsp

**What**: Listagem: aluno, curso, data, situação, progresso, pago, Editar, Excluir, "Nenhuma matrícula cadastrada.", paginação.
**Where**: `src/main/webapp/enrollments.jsp`
**Depends on**: T50
**Reuses**: -
**Requirement**: MATR-01, MATR-11, PAG-03

**Done when**:

- [ ] Colunas e ações do spec
- [ ] Modal de exclusão com `page`
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): enrollments.jsp`

---

### T52: Criar form-enrollment.jsp

**What**: Formulário: select aluno, select curso, date, radio situação, range progresso, checkbox pago; preenchido na edição; erros por campo.
**Where**: `src/main/webapp/form-enrollment.jsp`
**Depends on**: T51
**Reuses**: -
**Requirement**: MATR-02, MATR-04, MATR-08

**Done when**:

- [ ] Seis campos com os tipos HTML do spec
- [ ] Valores selecionados na edição
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `feat(web): form-enrollment`

---

### T53: Remover pacotes legados

**What**: Apagar `model/` e `controller/` do projeto base.
**Where**: `src/main/java/model/` e `src/main/java/controller/`
**Depends on**: None (previous phase complete)
**Reuses**: -
**Requirement**: ARQ-01

**Done when**:

- [ ] Diretórios removidos
- [ ] `mvn -q -DskipTests package` passa
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `refactor: remove pacotes legados model e controller`

---

### T54: Criar ArchitectureTest

**What**: Varre fontes e `pom.xml`: pacotes hexagonais existem, `domain`/`application` sem `jakarta.servlet`/`java.sql`/`javax.sql`, sem MySQL, servlets sem SQL.
**Where**: `src/test/java/crudmanager/ArchitectureTest.java`
**Depends on**: T53
**Reuses**: -
**Requirement**: ARQ-01, ARQ-02, DOCK-07

**Done when**:

- [ ] Testes `ARQ-01`, `ARQ-02`, `DOCK-07` passam
- [ ] `mvn -q test` passa
- [ ] Gate passes: `mvn -q test`

**Tests**: unit
**Gate**: quick
**Commit**: `test: regras da arquitetura hexagonal`

---

### T55: Criar Dockerfile

**What**: Multi-stage: `maven:3.9-eclipse-temurin-21` gera o WAR; `tomcat:10.1-jdk21` o serve como `crud-manager.war`.
**Where**: `Dockerfile`
**Depends on**: T54
**Reuses**: -
**Requirement**: DOCK-01

**Done when**:

- [ ] `docker build .` conclui
- [ ] WAR copiado para `webapps/crud-manager.war`
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `build(docker): adiciona Dockerfile multi-stage`

---

### T56: Criar docker-compose.yml

**What**: `db` (postgres:16, volume nomeado, `script-sql.sql` em initdb, healthcheck) e `app` (`depends_on: service_healthy`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, porta 8080).
**Where**: `docker-compose.yml`
**Depends on**: T55
**Reuses**: -
**Requirement**: DOCK-01, DOCK-02, DOCK-03, DOCK-04, DOCK-05

**Done when**:

- [ ] `docker compose config -q` passa
- [ ] `docker compose up -d --build --wait` leva a `http://localhost:8080/crud-manager/login` com 200
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `build(docker): compose com app e PostgreSQL`

---

### T57: Criar smoke.sh: base, login e menu

**What**: Script que sobe o compose se preciso, aguarda, faz login com cookie e imprime `✓ <ID>`; casos de login, logout, menu e subida.
**Where**: `scripts/smoke.sh`
**Depends on**: T56
**Reuses**: -
**Requirement**: AUTH-02, AUTH-06, AUTH-07, NAV-01, NAV-02, NAV-03, DOCK-01, DOCK-02, DOCK-04

**Done when**:

- [ ] `bash scripts/smoke.sh` imprime `✓` para cada ID listado e sai 0
- [ ] Falha em qualquer caso sai diferente de 0
- [ ] Gate passes: `bash scripts/smoke.sh`

**Tests**: none
**Gate**: smoke
**Commit**: `test(smoke): subida, login e menu`

---

### T58: Smoke: Cursos e Matrículas

**What**: Casos de listagem e formulários (selects preenchidos pelo banco, valores na edição) e CRUD de Usuários, Posts e Empresas.
**Where**: `scripts/smoke.sh`
**Depends on**: T57
**Reuses**: -
**Requirement**: CURSO-01, CURSO-02, CURSO-04, MATR-01, MATR-02, MATR-04, ARQ-06

**Done when**:

- [ ] `bash scripts/smoke.sh` imprime `✓` para os IDs listados
- [ ] Gate passes: `bash scripts/smoke.sh`

**Tests**: none
**Gate**: smoke
**Commit**: `test(smoke): cursos, matrículas e CRUDs existentes`

---

### T59: Smoke: paginação

**What**: Casos com o seed: 10 itens por página, `?page=` inválido e acima do limite, controles, ocultos com uma página.
**Where**: `scripts/smoke.sh`
**Depends on**: T58
**Reuses**: -
**Requirement**: PAG-03, PAG-04, PAG-08

**Done when**:

- [ ] `bash scripts/smoke.sh` imprime `✓ PAG-03`, `✓ PAG-04`, `✓ PAG-08`
- [ ] Gate passes: `bash scripts/smoke.sh`

**Tests**: none
**Gate**: smoke
**Commit**: `test(smoke): paginação`

---

### T60: Smoke: persistência, banco fora e vazios

**What**: Persistência após reinício do `app`, página de erro com banco parado, listas vazias de cursos e matrículas.
**Where**: `scripts/smoke.sh`
**Depends on**: T59
**Reuses**: -
**Requirement**: DOCK-05, DOCK-06, CURSO-11, MATR-11

**Done when**:

- [ ] `bash scripts/smoke.sh` imprime `✓` para os IDs listados, deixa o stack de pé e o banco rodando
- [ ] Gate passes: `bash scripts/smoke.sh`

**Tests**: none
**Gate**: smoke
**Commit**: `test(smoke): persistência, falha de banco e listas vazias`

---

### T61: Escrever README

**What**: Como subir (`docker compose up --build`), URL, usuários e senha do seed, estrutura hexagonal, como rodar `mvn test` e `mvn verify`, lista de artefatos pedidos pelo enunciado.
**Where**: `README.md`
**Depends on**: T60
**Reuses**: -
**Requirement**: DOCK-01

**Done when**:

- [ ] Comandos conferidos na execução
- [ ] Cada artefato do enunciado aponta para seu caminho
- [ ] Gate passes: build gate

**Tests**: none
**Gate**: build
**Commit**: `docs: adiciona README da plataforma de cursos`

---

## Checks

**C1** - Sem sessão, GET de URL protegida responde 302 para `/crud-manager/login` (AUTH-01, AC 1)
Proof: run all | ✓ AUTH-01

**C2** - `/login` exibe campos e-mail, senha e o botão "Entrar" (AUTH-02, AC 2)
Proof: run smoke | ✓ AUTH-02

**C3** - Login correto cria a sessão e redireciona para `/crud-manager/` (AUTH-03, AC 3)
Proof: run all | ✓ AUTH-03

**C4** - E-mail ou senha errados reexibem o login com "E-mail ou senha inválidos" e sem sessão (AUTH-04, AC 4)
Proof: run all | ✓ AUTH-04

**C5** - E-mail ou senha vazios reexibem o login com "Informe e-mail e senha" (AUTH-05, AC 5)
Proof: run all | ✓ AUTH-05

**C6** - Logado, o menu Perfil mostra o nome do usuário (AUTH-06, AC 6)
Proof: run smoke | ✓ AUTH-06

**C7** - "Sair" invalida a sessão e redireciona para `/crud-manager/login` (AUTH-07, AC 7)
Proof: run smoke | ✓ AUTH-07

**C8** - Logado, GET `/login` redireciona para `/crud-manager/` (AUTH-08, AC 8)
Proof: run all | ✓ AUTH-08

**C9** - `/login`, `/css/*`, `/js/*`, `/fonts/*`, `/images/*` passam pelo filtro sem sessão (AUTH-09, AC 9)
Proof: run all | ✓ AUTH-09

**C10** - A senha é guardada como PBKDF2WithHmacSHA256 com salt individual, nunca em texto puro (AUTH-10, AC 10)
Proof: run all | ✓ AUTH-10

**C11** - O login troca o identificador de sessão (AUTH-11, AC 11)
Proof: run all | ✓ AUTH-11

**C12** - Criar usuário exige senha e grava só o hash (AUTH-12, AC 12)
Proof: run all | ✓ AUTH-12

**C13** - Editar usuário com senha em branco mantém a senha atual (AUTH-13, AC 13)
Proof: run all | ✓ AUTH-13

**C14** - A lista de cursos mostra título, nível, preço, início, ativo, instrutor, Editar e Excluir (CURSO-01, AC 1)
Proof: run smoke | ✓ CURSO-01

**C15** - O formulário de curso tem os sete campos e o select de instrutor lista os usuários do banco (CURSO-02, AC 2)
Proof: run smoke | ✓ CURSO-02

**C16** - Curso válido é gravado e a lista mostra "Curso '<título>' salvo com sucesso." (CURSO-03, AC 3)
Proof: run all | ✓ CURSO-03

**C17** - Editar abre o formulário preenchido com o instrutor selecionado (CURSO-04, AC 4)
Proof: run smoke | ✓ CURSO-04

**C18** - A edição válida atualiza e mostra "Curso '<título>' atualizado com sucesso." (CURSO-05, AC 5)
Proof: run all | ✓ CURSO-05

**C19** - Confirmar a exclusão remove e mostra "Curso '<título>' deletado com sucesso." (CURSO-06, AC 6)
Proof: run all | ✓ CURSO-06

**C20** - Curso com matrículas não é excluído e mostra "… Há dados relacionados ao curso." (CURSO-07, AC 7)
Proof: run all | ✓ CURSO-07

**C21** - Campo obrigatório vazio ou preço negativo reexibe o formulário com valores digitados e mensagem por campo (CURSO-08, AC 8)
Proof: run all | ✓ CURSO-08

**C22** - Título com mais de 150 caracteres é rejeitado com mensagem de tamanho máximo (CURSO-09, AC 9)
Proof: run all | ✓ CURSO-09

**C23** - Checkbox "ativo" desmarcado grava o curso como inativo (CURSO-10, AC 10)
Proof: run all | ✓ CURSO-10

**C24** - Sem cursos, a lista mostra "Nenhum curso cadastrado." (CURSO-11, AC 11)
Proof: run smoke | ✓ CURSO-11

**C25** - Curso inexistente na edição ou exclusão redireciona com "Curso não encontrado." (CURSO-12, AC 12)
Proof: run all | ✓ CURSO-12

**C26** - A lista de matrículas mostra aluno, curso, data, situação, progresso, pago, Editar e Excluir (MATR-01, AC 1)
Proof: run smoke | ✓ MATR-01

**C27** - O formulário de matrícula tem os seis campos e os selects de aluno e curso vêm do banco (MATR-02, AC 2)
Proof: run smoke | ✓ MATR-02

**C28** - Matrícula válida é gravada e mostra "Matrícula salva com sucesso." (MATR-03, AC 3)
Proof: run all | ✓ MATR-03

**C29** - Editar abre o formulário com aluno, curso, situação e progresso selecionados (MATR-04, AC 4)
Proof: run smoke | ✓ MATR-04

**C30** - A edição válida atualiza e mostra "Matrícula atualizada com sucesso." (MATR-05, AC 5)
Proof: run all | ✓ MATR-05

**C31** - Confirmar a exclusão remove e mostra "Matrícula deletada com sucesso." (MATR-06, AC 6)
Proof: run all | ✓ MATR-06

**C32** - Par aluno-curso repetido é rejeitado com "Este aluno já está matriculado neste curso." (MATR-07, AC 7)
Proof: run all | ✓ MATR-07

**C33** - Campo vazio ou progresso fora de 0 a 100 é rejeitado com mensagem por campo (MATR-08, AC 8)
Proof: run all | ✓ MATR-08

**C34** - Situação Concluída com progresso menor que 100 é rejeitada com "Matrícula concluída exige progresso 100." (MATR-09, AC 9)
Proof: run all | ✓ MATR-09

**C35** - Checkbox "pago" desmarcado grava a matrícula como não paga (MATR-10, AC 10)
Proof: run all | ✓ MATR-10

**C36** - Sem matrículas, a lista mostra "Nenhuma matrícula cadastrada." (MATR-11, AC 11)
Proof: run smoke | ✓ MATR-11

**C37** - Matrícula inexistente na edição ou exclusão redireciona com "Matrícula não encontrada." (MATR-12, AC 12)
Proof: run all | ✓ MATR-12

**C38** - Cada listagem mostra no máximo 10 itens ordenados por id crescente (PAG-01, AC 1)
Proof: run all | ✓ PAG-01

**C39** - `?page=N` mostra os itens da página N (PAG-02, AC 2)
Proof: run all | ✓ PAG-02

**C40** - Com mais de uma página aparecem Anterior, números e Próximo, com a atual destacada (PAG-03, AC 3)
Proof: run smoke | ✓ PAG-03

**C41** - Na primeira página "Anterior" fica desabilitado e na última "Próximo" fica desabilitado (PAG-04, AC 4)
Proof: run smoke | ✓ PAG-04

**C42** - `page` ausente, não numérico ou menor que 1 mostra a página 1 (PAG-05, AC 5)
Proof: run all | ✓ PAG-05

**C43** - `page` acima da última mostra a última página (PAG-06, AC 6)
Proof: run all | ✓ PAG-06

**C44** - Excluir o único item da última página redireciona para a nova última página (PAG-07, AC 7)
Proof: run all | ✓ PAG-07

**C45** - Com uma única página os controles de paginação ficam ocultos (PAG-08, AC 8)
Proof: run smoke | ✓ PAG-08

**C46** - `docker compose up --build` sobe PostgreSQL 16 e Tomcat 10.1 com a app em `http://localhost:8080/crud-manager/` (DOCK-01, AC 1)
Proof: run smoke | ✓ DOCK-01

**C47** - Com volume vazio, `script-sql.sql` cria as cinco tabelas e o seed (DOCK-02, AC 2)
Proof: run smoke | ✓ DOCK-02

**C48** - URL, usuário e senha do banco vêm de variáveis de ambiente, sem credencial no código (DOCK-03, AC 3)
Proof: run all | ✓ DOCK-03

**C49** - O contêiner da app depende do healthcheck do PostgreSQL (`service_healthy`) (DOCK-04, AC 4)
Proof: run smoke | ✓ DOCK-04

**C50** - Reiniciar o contêiner da app preserva os dados gravados (DOCK-05, AC 5)
Proof: run smoke | ✓ DOCK-05

**C51** - Com o banco parado, a página mostra mensagem de erro sem stack trace (DOCK-06, AC 6)
Proof: run smoke | ✓ DOCK-06

**C52** - O projeto usa o driver PostgreSQL e não referencia o driver MySQL (DOCK-07, AC 7)
Proof: run all | ✓ DOCK-07

**C53** - `script-sql.sql` define `courses`, `enrollments` com FKs e unique, e `users.password_hash` (DB-01, AC 1)
Proof: run all | ✓ DB-01

**C54** - Logado, o cabeçalho tem os links Cursos e Matrículas ao lado de Usuários, Posts e Empresas (NAV-01, AC 2)
Proof: run smoke | ✓ NAV-01

**C55** - Toda página protegida mostra o cabeçalho com os cinco links de cadastro (NAV-02, AC 3)
Proof: run smoke | ✓ NAV-02

**C56** - O link "Posts" abre `/crud-manager/posts` (NAV-03, AC 4)
Proof: run smoke | ✓ NAV-03

**C57** - O código está em `domain`, `application` (`port.in`, `port.out`, `service`), `adapter.in.web` e `adapter.out.persistence` (ARQ-01, AC 1)
Proof: run all | ✓ ARQ-01

**C58** - `domain` e `application` não importam `jakarta.servlet`, `java.sql` nem `javax.sql` (ARQ-02, AC 2)
Proof: run all | ✓ ARQ-02

**C59** - Validação, paginação, regra de matrícula e autenticação rodam nos serviços, sem Servlet (ARQ-03, AC 3)
Proof: run all | ✓ ARQ-03

**C60** - `DAOFactory` devolve a implementação JDBC de cada uma das cinco portas (ARQ-04, AC 4)
Proof: run all | ✓ ARQ-04

**C61** - Os Servlets só traduzem HTTP e chamam as portas de entrada (ARQ-05, AC 5)
Proof: run all | ✓ ARQ-05

**C62** - Usuários, Posts e Empresas continuam listando, criando, editando e excluindo, com paginação e login (ARQ-06, AC 6)
Proof: run smoke | ✓ ARQ-06

**C63** - Excluir usuário que é instrutor, aluno, autor ou tem empresa mantém o usuário e avisa dados relacionados (EDGE-1, edge case)
Proof: run all | ✓ EDGE-1

**C64** - Id não numérico em editar ou excluir redireciona com registro não encontrado, sem erro 500 (EDGE-2, edge case)
Proof: run all | ✓ EDGE-2

**C65** - Acentos e emojis gravados voltam idênticos (UTF-8) (EDGE-3, edge case)
Proof: run all | ✓ EDGE-3

**C66** - Sessão expirada leva ao login na próxima requisição (EDGE-4, edge case)
Proof: run all | ✓ EDGE-4

**C67** - POST sem sessão redireciona para o login sem alterar dados (EDGE-5, edge case)
Proof: run all | ✓ EDGE-5

---

## Handoff

The feature's live state, written as it happens - a compaction gives no warning.

**Settled mid-build:** Build provisório até a T56 - user: "Linha Build provisória (Recomendado)"; execução em 8 lotes com sub-agentes - user: "Sim, 8 lotes (Recomendado)"
**Dispatched:** Lote 1 (Fase 1+2, T1-T11) enviado em HEAD 4fd6b89, returned; Lote 2 (Fase 3, T12-T18) enviado em HEAD ceaa9b1
**Walked:** none
