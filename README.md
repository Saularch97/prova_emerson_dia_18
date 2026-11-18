# CRUD Manager - Plataforma de Cursos

Aplicação web (Jakarta EE 10, Servlets/JSP, JDBC puro) com cinco cadastros: Usuários, Posts, Empresas, Cursos e Matrículas. Tem login por e-mail e senha, listagens paginadas (10 itens por página) e roda em Docker com PostgreSQL 16 e Tomcat 10.1 (JDK 21). O código segue arquitetura em camadas (controller, service e DAO), com injeção de dependência por construtor.

## Como subir

Requer Docker com o plugin compose.

```bash
docker compose up --build
```

Quando o app estiver pronto, abra <http://localhost:8080/crud-manager/>. O PostgreSQL executa `script-sql.sql` na primeira inicialização (volume vazio), criando as tabelas e os dados de exemplo. Os dados ficam no volume nomeado `pgdata` e sobrevivem a reinícios; para recomeçar do seed, `docker compose down -v`.

### Usuários do seed

A senha de todos é `senha123`.

| Nome | E-mail |
| ---- | ------ |
| Emerson Carvalho | `emerson@mail.com` |
| Luiza Carvalho | `lu@mail.com` |
| Elenice Carvalho | `le@mail.com` |
| Noé Carvalho | `noe@mail.com` |
| Rosânia Carvalho | `ro@mail.com` |

O seed tem ainda dez usuários (`ana@mail.com`, `bruno@mail.com`, `carla@mail.com`, `diego@mail.com`, `elisa@mail.com`, `fabio@mail.com`, `gabriela@mail.com`, `heitor@mail.com`, `isabela@mail.com`, `joao@mail.com`), também com `senha123`.

### Variáveis de ambiente do banco

A aplicação lê a conexão de `DB_URL`, `DB_USER` e `DB_PASSWORD` (sem valores padrão no código). O `docker-compose.yml` as define a partir de variáveis que você pode sobrescrever no shell ou em um arquivo `.env`:

| Variável | Uso | Padrão do compose |
| -------- | --- | ----------------- |
| `POSTGRES_DB` | nome do banco (vira parte de `DB_URL`) | `crud_cursos` |
| `POSTGRES_USER` | usuário do banco (vira `DB_USER`) | `crud` |
| `POSTGRES_PASSWORD` | senha do banco (vira `DB_PASSWORD`) | `crud_dev_pass` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | lidas pelo app; no compose valem `jdbc:postgresql://db:5432/${POSTGRES_DB}` e os dois valores acima | - |

Os valores padrão são só de desenvolvimento.

## Testes

```bash
mvn test                  # testes unitários e de arquitetura, sem Docker
mvn verify                # inclui os testes de integração do adaptador JDBC com Testcontainers 1.21.4; exige Docker recente
bash scripts/smoke.sh     # smoke test ponta a ponta contra o stack docker compose
```

`mvn test` e `mvn verify` imprimem `✓ <ID do critério> descrição` para cada teste. O `smoke.sh` sobe o stack se ele ainda não estiver de pé, imprime `✓ <ID>` por caso aprovado e sai com código diferente de zero se algum falhar; `bash scripts/smoke.sh AUTH-02` roda um caso só. Os casos que esvaziam listas (PAG-08, MATR-11, CURSO-11) rodam num stack descartável (projeto compose `crudcursos-smoke`, porta 18080), então o banco do stack principal permanece igual ao seed; os casos que criam dados os removem ao terminar. O caso DOCK-06 para e religa o banco, e o DOCK-05 reinicia o app.

Para gerar só o WAR: `mvn -q -DskipTests package` (saída em `target/crud-manager.war`).

## Estrutura (camadas)

Fluxo: `controller` (HTTP) → `service` (regras de negócio) → `dao` (persistência). Cada camada depende da interface da camada abaixo, nunca da implementação.

```
src/main/java/crudmanager/
  controller/                    Servlets, AuthFilter e filtro de encoding: traduzem HTTP para chamadas de serviço e escolhem a JSP
  service/                       interfaces de serviço (UserService, CourseService, AuthService, ...) e Pagination
    impl/                        implementações (UserServiceImpl, ...): validações, paginação, regra de matrícula, autenticação
  dao/                           interfaces DAO (UserDAO, CourseDAO, ...)
    jdbc/                        DAOs JDBC PostgreSQL (Jdbc*DAO), DAOFactory e conexão por variáveis de ambiente
  model/                         entidades (User, Post, Company, Course, Enrollment) e Page
  exception/                     exceções (validação, não encontrado, dados relacionados, duplicidade)
  security/                      PasswordHasher e Pbkdf2PasswordHasher (PBKDF2WithHmacSHA256 com salt por usuário)
  config/                        ServiceLocator: ponto de composição que injeta DAOs e hasher nos serviços
src/main/webapp/                 JSPs (listagens, formulários, cabeçalho, login), CSS/JS estáticos e web.xml
src/test/java/                   testes JUnit 5 (serviços com DAOs falsos, Mockito nos Servlets, Testcontainers nos DAOs, ArchitectureTest)
scripts/smoke.sh                 smoke test do stack
script-sql.sql                   esquema PostgreSQL e dados de exemplo
Dockerfile, docker-compose.yml   imagem multi-stage (Maven, depois Tomcat 10.1) e stack app + db
```

A regra de dependência é verificada por `ArchitectureTest`: `model`, `exception`, `service`, `security` e as interfaces de `dao` não importam `jakarta.servlet`, `java.sql` nem `javax.sql`; `controller` não importa `dao`; `service` não importa `controller`, `dao.jdbc` nem `config`.

## Artefatos pedidos pelo enunciado

| Artefato | Caminho |
| -------- | ------- |
| Script SQL (tabelas e dados) | `script-sql.sql` |
| Modelo (classes de domínio) | `src/main/java/crudmanager/model/` (`User`, `Post`, `Company`, `Course`, `Enrollment`) |
| Interfaces DAO | `src/main/java/crudmanager/dao/` (`UserDAO`, `PostDAO`, `CompanyDAO`, `CourseDAO`, `EnrollmentDAO`) |
| Classes DAO (JDBC) | `src/main/java/crudmanager/dao/jdbc/` (`JdbcUserDAO`, `JdbcPostDAO`, `JdbcCompanyDAO`, `JdbcCourseDAO`, `JdbcEnrollmentDAO`) |
| DAOFactory | `src/main/java/crudmanager/dao/jdbc/DAOFactory.java` |
| Servlets (controllers) | `src/main/java/crudmanager/controller/` (`UsersController`, `PostsController`, `CompaniesController`, `CoursesController`, `EnrollmentsController`) |
| JSPs de listagem | `src/main/webapp/users.jsp`, `index.jsp` (Posts), `companies.jsp`, `courses.jsp`, `enrollments.jsp`; paginação em `pagination.jsp` |
| JSPs de formulário | `src/main/webapp/form-user.jsp`, `form-post.jsp`, `form-company.jsp`, `form-course.jsp`, `form-enrollment.jsp` |
| Exclusão (modal de confirmação) | `src/main/webapp/modal.html`, incluído em cada listagem; envia o POST para `/<entidade>/delete` |
| Cabeçalho (menu) | `src/main/webapp/nav-menu.jsp` (inclui `base-head.jsp` nas páginas) |
| Login: filtro | `src/main/java/crudmanager/controller/AuthFilter.java` (registrado em `src/main/webapp/WEB-INF/web.xml`) |
| Login: controller | `src/main/java/crudmanager/controller/LoginController.java` (`/login` e `/logout`) |
| Login: página | `src/main/webapp/login.jsp` |

## URLs

| Página | URL |
| ------ | --- |
| Login | `/crud-manager/login` |
| Posts (home) | `/crud-manager/` ou `/crud-manager/posts` |
| Usuários | `/crud-manager/users` |
| Empresas | `/crud-manager/companies` |
| Cursos | `/crud-manager/courses` |
| Matrículas | `/crud-manager/enrollments` |

Toda URL, exceto `/login` e os arquivos estáticos (`/css`, `/js`, `/fonts`, `/images`), exige sessão; sem ela o `AuthFilter` redireciona para o login.
