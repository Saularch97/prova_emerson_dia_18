# plataforma-cursos verification

**Verdict**: FAIL
**Proofs**: `proofs.md` at 7f64082 (HEAD c037cf7 adds only `proofs.md` and `tasks.md`, no code)
**Verifier**: independent sub-agent (author != verifier)

## Checks

| Check | Test | Holds |
| --- | --- | --- |
| C1 | `AuthFilterTest` `AUTH-01 sem sessão, rota protegida redireciona para /login sem seguir a cadeia` | yes (302 to /login; the web.xml mapping is not unit-tested, but live GET /, /users, /courses.jsp, /login/../users without session all returned 302 to /login) |
| C2 | `scripts/smoke.sh` `case_AUTH_02` | yes |
| C3 | `LoginControllerTest` `AUTH-03 login correto guarda o usuário na sessão e redireciona para a raiz` | yes |
| C4 | `LoginControllerTest` `AUTH-04 credencial errada reexibe login...` + `AuthServiceTest` `AUTH-04` (2 tests) | yes |
| C5 | `LoginControllerTest` `AUTH-05 campos vazios reexibem login...` + `AuthServiceTest` `AUTH-05` (5 combos) | yes |
| C6 | `scripts/smoke.sh` `case_AUTH_06` | yes |
| C7 | `scripts/smoke.sh` `case_AUTH_07` | yes (the old cookie is rejected after logout) |
| C8 | `LoginControllerTest` `AUTH-08 GET /login com usuário autenticado redireciona para a raiz` | yes |
| C9 | `AuthFilterTest` `AUTH-09 /login, /css/, /js/, /fonts/ e /images/ passam sem autenticação` | yes (positive cases only; the negative side is held by the AUTH-01 tests) |
| C10 | `Pbkdf2PasswordHasherTest` `AUTH-10 ...` (4 tests; `matchesSeedHash` pins the algorithm) | yes |
| C11 | `LoginControllerTest` `AUTH-11 login troca o identificador de sessão antes de gravar o usuário` | yes (live: JSESSIONID changed across the login POST) |
| C12 | `UserServiceTest` `AUTH-12 create grava só o hash...` / `create sem senha...` + `JdbcUserDAOIT` AUTH-12 | yes |
| C13 | `UserServiceTest` `AUTH-13 update com senha em branco mantém o hash atual` | yes (live: new user edited with blank and with whitespace-only password still logs in with the old one) |
| C14 | `scripts/smoke.sh` `case_CURSO_01` | yes |
| C15 | `scripts/smoke.sh` `case_CURSO_02` | yes |
| C16 | `CoursesControllerTest` `CURSO-03 POST /course/insert ...` + `CourseServiceTest` `CURSO-03` | yes |
| C17 | `scripts/smoke.sh` `case_CURSO_04` | yes (the checkbox is asserted only for the active seed row) |
| C18 | `CoursesControllerTest` `CURSO-05 POST /course/update ...` + `CourseServiceTest` `CURSO-05` | yes |
| C19 | `CoursesControllerTest` `CURSO-06 POST /course/delete ...` + `CourseServiceTest` `CURSO-06` | yes |
| C20 | `CoursesControllerTest` `CURSO-07 ... mantém o curso e avisa dados relacionados` + `JdbcCourseDAOIT` `CURSO-07` | yes |
| C21 | `CourseServiceTest` `CURSO-08` (3 tests) + `CoursesControllerTest` `CURSO-08` (5 tests) | no - the per-field message is not held: unparsable price/date skip the service, so other invalid fields vanish (F1); the service tests assert only `containsKey` for 5 of 6 fields and no test combines two invalid fields |
| C22 | `CourseServiceTest` `CURSO-09 título com 151 caracteres ... 150 é aceito` | yes (exact message, 150 and 151; counts UTF-16 units, see F4) |
| C23 | `CoursesControllerTest` `CURSO-10 checkbox ativo ausente grava o curso como inativo` + `JdbcCourseDAOIT` `CURSO-10` | yes |
| C24 | `scripts/smoke.sh` `case_CURSO_11` | yes |
| C25 | `CoursesControllerTest` `CURSO-12` (GET update, POST update, POST delete) + `CourseServiceTest` `CURSO-12` | yes |
| C26 | `scripts/smoke.sh` `case_MATR_01` | yes |
| C27 | `scripts/smoke.sh` `case_MATR_02` | yes |
| C28 | `EnrollmentsControllerTest` `MATR-03 POST /enrollment/insert ...` + `EnrollmentServiceTest` `MATR-03` | yes |
| C29 | `scripts/smoke.sh` `case_MATR_04` | yes |
| C30 | `EnrollmentsControllerTest` `MATR-05 POST /enrollment/update ...` | yes |
| C31 | `EnrollmentsControllerTest` `MATR-06 POST /enrollment/delete ...` | yes |
| C32 | `EnrollmentServiceTest` `MATR-07 par aluno-curso repetido lança DuplicateException...` + `JdbcEnrollmentDAOIT` `MATR-07` | yes (exact message, other student and other course accepted) |
| C33 | `EnrollmentServiceTest` `MATR-08` (2 tests) + `EnrollmentsControllerTest` `MATR-08` (3 tests) | no - same gap as C21: with a bad progress or date the empty status/student/course messages vanish (F1), and the service asserts `containsKey` only, never the message of the 4 required fields |
| C34 | `EnrollmentServiceTest` `MATR-09 concluída com progresso menor que 100 ...` | yes (99 rejected with the exact message, 100 accepted) |
| C35 | `EnrollmentsControllerTest` `MATR-10 checkbox pago ausente grava ... não paga` + `JdbcEnrollmentDAOIT` `MATR-10` | yes (the service test `MATR-10` proves only that paid=false is not overwritten, F5; the rule is held by the web test) |
| C36 | `scripts/smoke.sh` `case_MATR_11` | yes |
| C37 | `EnrollmentsControllerTest` `MATR-12` (GET, POST update, POST delete) + `EnrollmentServiceTest` `MATR-12` | yes |
| C38 | `PaginationTest` `PAG-01 tamanho de página é 10`, `PageTest` `PAG-01`, `WebUtilTest` `PAG-01` | no - the PAG-01-named tests assert the constant 10, immutability and parsePage; no one asserts at most 10 rows or id order. That is held only by the `ARQ-06 findPage ordena por id crescente` ITs (not selected by `✓ PAG-01`) and a smoke count that does not check order (F3) |
| C39 | `PaginationTest` `PAG-02` (offset, parse, clamp) + `UserServiceTest` `list(3)` first id 21 | yes |
| C40 | `scripts/smoke.sh` `case_PAG_03` | yes (all five lists, active page 1 and 2) |
| C41 | `scripts/smoke.sh` `case_PAG_04` | yes (all five lists, first and last page) |
| C42 | `PaginationTest` `PAG-05` (3 tests: null, empty, blank, abc, 2.5, overflow, 0, -4) | yes (live: 1.5, 1e1, 2abc show page 1) |
| C43 | `PaginationTest` `PAG-06` (99 of 25 = 3; 3 of 20 = 2; empty = 1) | yes |
| C44 | `CoursesControllerTest` / `PostsControllerTest` / `UsersControllerTest` `PAG-07 POST ... delete` | no - the PAG-07 tests assert only that the redirect carries `?page=N`; none deletes the only item of the last page and sees the new last page. Only smoke `PAG-07` (posts only) does, and C44's proof is `run all` (F3) |
| C45 | `scripts/smoke.sh` `case_PAG_08` + `PageTest` `PAG-08` | yes (smoke proves 1 of 5 lists; the JSP is the shared `pagination.jsp`) |
| C46 | `scripts/smoke.sh` `case_DOCK_01` | yes |
| C47 | `scripts/smoke.sh` `case_DOCK_02` | yes (inspects the live DB; it does not start from an empty volume) |
| C48 | `JdbcConnectionFactoryTest` `DOCK-03` (3 tests) | yes |
| C49 | `scripts/smoke.sh` `case_DOCK_04` | yes |
| C50 | `scripts/smoke.sh` `case_DOCK_05` | yes |
| C51 | `scripts/smoke.sh` `case_DOCK_06` | yes (list route only) |
| C52 | `ArchitectureTest` `DOCK-07 pom.xml usa o driver PostgreSQL e não há MySQL ...` | yes |
| C53 | `SchemaIT` `DB-01` (4 tests) | yes |
| C54 | `scripts/smoke.sh` `case_NAV_01` | yes |
| C55 | `scripts/smoke.sh` `case_NAV_02` | yes |
| C56 | `scripts/smoke.sh` `case_NAV_03` | yes |
| C57 | `ArchitectureTest` `ARQ-01 pacotes hexagonais existem e contêm classes` | yes |
| C58 | `ArchitectureTest` `ARQ-02 domain e application não importam ...` | yes (scans import lines only; a grep for fully qualified use found none) |
| C59 | `UserServiceTest` / `CourseServiceTest` / `EnrollmentServiceTest` / `AuthServiceTest` `ARQ-03` + rule tests | yes |
| C60 | `DAOFactoryTest` `ARQ-04 createDAO devolve a implementação JDBC de cada uma das cinco portas` | yes |
| C61 | `ArchitectureTest` `ARQ-05 adapter/in/web ... não importa java.sql nem DAOs` | yes |
| C62 | `scripts/smoke.sh` `case_ARQ_06` | yes |
| C63 | `JdbcUserDAOIT` `EDGE-1 excluir usuário referenciado ...` (Emerson) + `UsersControllerTest` `EDGE-1` | no - one user that holds several relations; instructor, student, post author and company are not each exercised (F6); service/web tests mock the exception |
| C64 | `CoursesControllerTest`, `EnrollmentsControllerTest`, `PostsControllerTest`, `CompaniesControllerTest`, `UsersControllerTest` `EDGE-2` (non-numeric id on GET update and POST delete/update) | yes |
| C65 | `JdbcCourseDAOIT` `EDGE-3 descrição com acentos e emoji ...` + `CharacterEncodingFilterTest` `EDGE-3` | yes (live: title with accents and emoji round-trips through the form and the list) |
| C66 | `AuthFilterTest` `EDGE-4 sessão expirada ...` | yes |
| C67 | `AuthFilterTest` `EDGE-5 POST sem sessão redireciona para /login ...` | yes (one route; live POST /course/insert without session: 302 and the table unchanged) |

## Findings

| Finding | Evidence | Fix | Closed |
| --- | --- | --- | --- |
| F1. Unparsable price, date or progress hides the other fields' messages (CURSO-08, MATR-08 ask for one message per invalid field) | Live: POST /course/insert with title empty, description empty, price `abc` shows only the price message; title empty plus date `garbage` shows only the date message. POST /enrollment/update with status empty and progress `abc` shows only the progress message; CONCLUIDA 99 plus date `xx` shows only the date message. `CoursesController.save` and `EnrollmentsController.save` skip the service when `errors` is not empty. The tests (`CURSO-08 preço não numérico ...`, `MATR-08 progresso não numérico e data inválida ...`) use otherwise-valid input, so none fails | run the service validation on the parsed fields and merge its errors with the parse errors; add a test with an empty title plus `price=abc` and one with empty status plus `progress=abc` asserting both messages | 682dc68 - C21 (CURSO-08), C33 (MATR-08): controllers always call the service and merge errors; web tests `CURSO-08 título vazio e preço abc ...`, `MATR-08 situação vazia e progresso abc ...`; service tests assert literal messages |
| F2. Price 100000000 and above (and 99999999.99 is the top accepted) is rejected with the wrong reason | Live: price `100000000` shows "...maior ou igual a zero, com até 2 casas decimais" though it has 0 decimals; no test covers the 99999999.99 / 100000000 boundary (`PRICE_LIMIT` in `CourseService`) | separate message for the upper bound ("no máximo 99999999,99") and a service test at 99999999.99 (accepted) and 100000000 (rejected) | 8628031 - C21 (CURSO-08): `CourseServiceTest` `CURSO-08 preço 99999999.99 é aceito; 100000000 é rejeitado ...` |
| F3. C38 (PAG-01) and C44 (PAG-07) selected tests do not hold the criterion | PAG-01 tests: constant 10, `Page` immutability, `parsePage`. Order by id and LIMIT live in `ARQ-06 findPage ordena por id crescente` ITs and smoke PAG-01 counts rows without checking order. PAG-07 tests assert `?page=N` in the redirect, never the new last page | rename or add IT tests named `PAG-01` (10 rows, ascending ids, 11th on page 2) for the five DAOs; for PAG-07 add a service test that deletes the only item of the last page and asserts `list(oldLast).getPage()` is the new last, or point C44 at smoke `PAG-07` | c3dacac - C38 (PAG-01): five `JdbcXxxDAOIT` `PAG-01 findPage devolve 10 linhas ...`; C44 (PAG-07): five `XxxServiceTest` `PAG-07 excluir o único item da última página ...` |
| F4. Title length counts UTF-16 units, not characters (CURSO-09) | Live: a title of 100 emoji (100 characters) is rejected with "no máximo 150 caracteres"; 150 `é` accepted. `CourseService` uses `String.length()` | count with `codePointCount` and add a test with 100 emoji (accepted) and 151 | 751973c - C22 (CURSO-09): `CourseServiceTest` `CURSO-09 o limite conta caracteres ...` (100, 150, 151 emojis) |
| F5. Service test `MATR-10` and `CURSO-10` (`ARQ-03 ... inactiveKept`) prove only that a false value is preserved | `EnrollmentServiceTest.unpaidKept`, `CourseServiceTest.inactiveKept`: pass for any service that does not touch the flag; the rule (absent checkbox -> false) is held by the web tests | drop or rename them; keep the web tests as the proof | 00e6c30 - C35 (MATR-10), C23 (CURSO-10): service tests renamed without requirement ID; web tests remain the proof |
| F6. C63 (EDGE-1) covers one of four relations | `JdbcUserDAOIT.deleteReferencedUser` deletes Emerson, who is instructor, author and owner at once; a missing FK on `posts`, `companies` or `enrollments` would still pass. Live: deleting user 1 keeps the user with the message, and `script-sql.sql` has no `ON DELETE CASCADE` | add four IT cases, each with a user bound to a single relation | ecb5c72 - C63 (EDGE-1): four `JdbcUserDAOIT` `EDGE-1 excluir usuário que é ...` tests |
| F7. Rules the criteria name but only mocks test | CURSO-08 refill of typed values is asserted for description, price and instructor only (not title, date, level, active; live refill works); `EnrollmentsController` resets an unparsable progress to 0 in the redisplayed form; DOCK-06 is probed on the list route only (login with the DB down goes to `error.jsp`); PAG-08 hiding is smoke on 1 of 5 lists | assert the full refill in the web test; keep the typed progress text; add the login route to the DOCK-06 case | 4b782bc - C21 (CURSO-08) full refill, C51 (DOCK-06) `LoginControllerTest` `DOCK-06` + smoke `case_DOCK_06` login probe, C45 (PAG-08) smoke `case_PAG_08` on five lists; `progressInput` kept (JSP in 682dc68) |

Probed live and holding: price 1.234 rejected, 0 decimals and `99999999,99` accepted; title 150 accepted and 151 rejected; accents and emoji round trip; ATIVA with progress 100 accepted; CONCLUIDA 100 accepted, 99 rejected; progress -1, 101 and 100.5 rejected, ` 50 ` accepted; `?page=1.5`, `1e1`, `2abc` show page 1; user edit with blank or whitespace password keeps the old login; blank password on create rejected; deleting a course with enrollments and the instructor user is refused; POST without session is redirected and changes nothing. All data created was deleted (users 15, courses 12, enrollments 13, as the seed).
