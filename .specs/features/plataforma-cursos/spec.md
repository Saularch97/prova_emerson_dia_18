# Plataforma de Cursos Specification

**Approved**: 2026-11-12 - user: "aprovado"

## Problem Statement

O CRUD Manager (Jakarta EE 10, Servlets/JSP, JDBC, MySQL) tem três cadastros (Usuários, Posts, Empresas), sem login e sem paginação. A atividade da disciplina exige dois novos cadastros num tema próprio (plataforma de cursos), login, paginação em todos os cadastros, e a entrega roda em Docker com PostgreSQL. O código é reorganizado em arquitetura hexagonal (portas e adaptadores).

## Goals

- [ ] CRUDs de Curso e Matrícula funcionando de ponta a ponta (listar, criar, editar, excluir), cada um com 5+ campos de tipos HTML distintos e FK escolhida por select.
- [ ] Toda URL da aplicação, exceto o login e os arquivos estáticos, exige usuário autenticado.
- [ ] As cinco listagens (Usuários, Posts, Empresas, Cursos, Matrículas) paginam com 10 itens por página.
- [ ] `docker compose up` sobe app e PostgreSQL prontos para uso, sem passos manuais.
- [ ] Código organizado em domain, application (portas e casos de uso) e adapters; domínio e aplicação não conhecem Servlet nem JDBC.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Papéis/permissões (admin, aluno) | Enunciado pede só login; todo usuário logado acessa tudo |
| Auto-cadastro público ("Criar conta") | Não pedido; usuários são criados no CRUD de Usuários |
| Campo de busca das listagens | Enunciado não pede busca; o campo decorativo foi removido das cinco telas |
| Menu "Dados cadastrais" do Perfil | Fora do enunciado; o item sem ação foi removido |
| Recuperação de senha, "lembrar-me", bloqueio por tentativas | Não pedido |
| Deploy em produção/nuvem | Só Docker local |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Entidades novas | Curso e Matrícula | Escolha do usuário | y |
| Mecanismo de login | E-mail + senha na tabela `users`, sem papéis, sem auto-cadastro | Escolha do usuário | y |
| Arquitetura | Hexagonal aplicada a todo o projeto, inclusive Users, Posts e Companies; `DAOFactory` vira composition root e continua listando os DAOs | Usuário: "siga hexagonal o recomendado" | y |
| Build/execução | Maven, Tomcat 10.1 (JDK 21), PostgreSQL 16, Dockerfile multi-stage e docker compose | Usuário: "o recomendado" | y |
| Hash de senha | PBKDF2WithHmacSHA256 com salt por usuário, API nativa do JDK | Sem dependência extra; senha nunca em texto puro | n |
| Senha dos usuários do seed | `senha123` para os cinco | Permite entrar logo após o `docker compose up` | n |
| Tamanho de página | 10 itens | Valor comum; ajustável em um ponto | n |
| Testes | JUnit 5; serviços com portas falsas; adaptador de persistência com Testcontainers (PostgreSQL); servlets com Mockito | Gate por tarefa roda no runner do projeto | n |
| Contexto da aplicação | `/crud-manager` (WAR `crud-manager.war`) | O código atual embute esse caminho nas JSPs e controllers | n |
| Link "Posts" do menu | Passa a apontar para `/posts` | Hoje aponta para a home, e o cabeçalho será revisto de qualquer forma | n |
| Senha no CRUD de Usuários | Obrigatória ao criar; em branco ao editar mantém a atual | Sem isso nenhum usuário novo consegue entrar | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Login e proteção de acesso ⭐ MVP

**User Story**: Como usuário cadastrado, quero entrar com e-mail e senha para usar o sistema, e como dono dele quero que visitantes anônimos não acessem nada.

**Why P1**: Exigência explícita do enunciado e pré-requisito de uso.

**Acceptance Criteria**:

1. WHEN um visitante sem sessão requisita `/crud-manager/` ou qualquer URL protegida THEN the system SHALL redirecionar (302) para `/crud-manager/login`. <!-- AUTH-01 -->
2. WHEN um visitante abre `/crud-manager/login` THEN the system SHALL exibir a página com campos e-mail e senha e o botão "Entrar". <!-- AUTH-02 -->
3. WHEN o usuário envia e-mail e senha corretos THEN the system SHALL criar a sessão e redirecionar para `/crud-manager/`. <!-- AUTH-03 -->
4. IF o e-mail não existe ou a senha não confere THEN the system SHALL reexibir o login com a mensagem "E-mail ou senha inválidos" e não criar sessão. <!-- AUTH-04 -->
5. IF e-mail ou senha chegam vazios THEN the system SHALL reexibir o login com a mensagem "Informe e-mail e senha". <!-- AUTH-05 -->
6. WHILE o usuário está autenticado, the system SHALL exibir seu nome no menu Perfil. <!-- AUTH-06 -->
7. WHEN o usuário autenticado aciona "Sair" no menu Perfil THEN the system SHALL invalidar a sessão e redirecionar para `/crud-manager/login`. <!-- AUTH-07 -->
8. WHILE o usuário está autenticado, WHEN ele abre `/crud-manager/login` THEN the system SHALL redirecioná-lo para `/crud-manager/`. <!-- AUTH-08 -->
9. The system SHALL servir `/login`, `/css/*`, `/js/*`, `/fonts/*` e `/images/*` sem exigir autenticação. <!-- AUTH-09 -->
10. The system SHALL armazenar senhas apenas como hash PBKDF2WithHmacSHA256 com salt individual, nunca em texto puro. <!-- AUTH-10 -->
11. WHEN uma nova sessão é criada no login THEN the system SHALL trocar o identificador de sessão anterior. <!-- AUTH-11 -->
12. WHEN o CRUD de Usuários cria um usuário THEN the system SHALL exigir o campo senha e gravar apenas seu hash. <!-- AUTH-12 -->
13. WHEN o CRUD de Usuários edita um usuário com o campo senha em branco THEN the system SHALL manter a senha atual. <!-- AUTH-13 -->

**Independent Test**: Sem sessão, abrir `/crud-manager/users` leva ao login; entrar com `emerson@mail.com`/`senha123` mostra a home; "Sair" volta ao login.

---

### P1: CRUD de Cursos ⭐ MVP

**User Story**: Como usuário logado, quero cadastrar e manter cursos para montar o catálogo da plataforma.

**Why P1**: Primeiro dos dois CRUDs exigidos.

Campos do curso (tipo HTML): título (`text`, obrigatório, máx. 150), descrição (`textarea`, obrigatório), preço (`number`, ≥ 0, 2 casas), data de início (`date`, obrigatória), nível (`select`: Iniciante, Intermediário, Avançado), ativo (`checkbox`), instrutor (`select` de usuários, FK `users`).

**Acceptance Criteria**:

1. WHEN o usuário abre `/crud-manager/courses` THEN the system SHALL listar os cursos com título, nível, preço, início, ativo, instrutor (nome), e as ações Editar e Excluir. <!-- CURSO-01 -->
2. WHEN o usuário aciona "Adicionar Curso" THEN the system SHALL exibir o formulário com os sete campos acima, sendo o instrutor um select preenchido com os usuários do banco. <!-- CURSO-02 -->
3. WHEN o usuário envia o formulário válido THEN the system SHALL gravar o curso, redirecionar para a listagem e exibir "Curso '<título>' salvo com sucesso.". <!-- CURSO-03 -->
4. WHEN o usuário aciona Editar THEN the system SHALL exibir o formulário preenchido com os dados do curso, incluindo o instrutor selecionado. <!-- CURSO-04 -->
5. WHEN o usuário envia a edição válida THEN the system SHALL atualizar o curso e exibir "Curso '<título>' atualizado com sucesso.". <!-- CURSO-05 -->
6. WHEN o usuário confirma a exclusão no modal THEN the system SHALL remover o curso e exibir "Curso '<título>' deletado com sucesso.". <!-- CURSO-06 -->
7. IF o curso a excluir possui matrículas THEN the system SHALL manter o curso e exibir "Curso '<título>' não pode ser deletado. Há dados relacionados ao curso.". <!-- CURSO-07 -->
8. IF título, descrição, data de início ou instrutor estão vazios, ou o preço é negativo ou não numérico, THEN the system SHALL rejeitar o envio, reexibir o formulário com os valores digitados e uma mensagem por campo inválido. <!-- CURSO-08 -->
9. IF o título excede 150 caracteres THEN the system SHALL rejeitar o envio com mensagem de tamanho máximo. <!-- CURSO-09 -->
10. WHEN o checkbox "ativo" está desmarcado no envio THEN the system SHALL gravar o curso como inativo. <!-- CURSO-10 -->
11. IF a lista de cursos está vazia THEN the system SHALL exibir "Nenhum curso cadastrado.". <!-- CURSO-11 -->
12. IF o id do curso a editar ou excluir não existe THEN the system SHALL redirecionar para a listagem com a mensagem "Curso não encontrado.". <!-- CURSO-12 -->

**Independent Test**: Criar um curso escolhendo instrutor no select, vê-lo na lista, editar o preço, excluir.

---

### P1: CRUD de Matrículas ⭐ MVP

**User Story**: Como usuário logado, quero matricular alunos em cursos e acompanhar o progresso.

**Why P1**: Segundo CRUD exigido; liga Usuário e Curso.

Campos da matrícula: aluno (`select` de usuários, FK `users`), curso (`select` de cursos, FK `courses`), data da matrícula (`date`, obrigatória), situação (`radio`: Ativa, Concluída, Cancelada), progresso (`range` 0 a 100), pago (`checkbox`). Um aluno só tem uma matrícula por curso.

**Acceptance Criteria**:

1. WHEN o usuário abre `/crud-manager/enrollments` THEN the system SHALL listar as matrículas com aluno (nome), curso (título), data, situação, progresso, pago, e as ações Editar e Excluir. <!-- MATR-01 -->
2. WHEN o usuário aciona "Adicionar Matrícula" THEN the system SHALL exibir o formulário com os seis campos acima, com aluno e curso como selects preenchidos pelo banco. <!-- MATR-02 -->
3. WHEN o usuário envia o formulário válido THEN the system SHALL gravar a matrícula, redirecionar para a listagem e exibir "Matrícula salva com sucesso.". <!-- MATR-03 -->
4. WHEN o usuário aciona Editar THEN the system SHALL exibir o formulário preenchido, com aluno, curso, situação e progresso selecionados. <!-- MATR-04 -->
5. WHEN o usuário envia a edição válida THEN the system SHALL atualizar a matrícula e exibir "Matrícula atualizada com sucesso.". <!-- MATR-05 -->
6. WHEN o usuário confirma a exclusão no modal THEN the system SHALL remover a matrícula e exibir "Matrícula deletada com sucesso.". <!-- MATR-06 -->
7. IF o par aluno-curso já tem matrícula THEN the system SHALL rejeitar o envio com "Este aluno já está matriculado neste curso." e reexibir o formulário. <!-- MATR-07 -->
8. IF aluno, curso, data ou situação estão vazios, ou o progresso está fora de 0 a 100, THEN the system SHALL rejeitar o envio com uma mensagem por campo inválido. <!-- MATR-08 -->
9. IF a situação é Concluída e o progresso é menor que 100 THEN the system SHALL rejeitar o envio com "Matrícula concluída exige progresso 100.". <!-- MATR-09 -->
10. WHEN o checkbox "pago" está desmarcado no envio THEN the system SHALL gravar a matrícula como não paga. <!-- MATR-10 -->
11. IF a lista de matrículas está vazia THEN the system SHALL exibir "Nenhuma matrícula cadastrada.". <!-- MATR-11 -->
12. IF o id da matrícula a editar ou excluir não existe THEN the system SHALL redirecionar para a listagem com "Matrícula não encontrada.". <!-- MATR-12 -->

**Independent Test**: Criar matrícula escolhendo aluno e curso, ver na lista, mudar situação para Concluída com progresso 100, excluir.

---

### P1: Paginação em todos os cadastros ⭐ MVP

**User Story**: Como usuário, quero navegar listas longas por páginas.

**Why P1**: Exigência explícita para todos os cadastros.

**Acceptance Criteria**:

1. WHEN o usuário abre qualquer listagem (Usuários, Posts, Empresas, Cursos, Matrículas) THEN the system SHALL exibir no máximo 10 itens, ordenados por id crescente. <!-- PAG-01 -->
2. WHEN a listagem é requisitada com `?page=N` THEN the system SHALL exibir os itens da página N. <!-- PAG-02 -->
3. WHILE a listagem tem mais de uma página, the system SHALL exibir controles Anterior, números de página e Próximo, com a página atual destacada. <!-- PAG-03 -->
4. WHILE a página atual é a primeira, the system SHALL exibir "Anterior" desabilitado; WHILE é a última, SHALL exibir "Próximo" desabilitado. <!-- PAG-04 -->
5. IF `page` está ausente, não é número, ou é menor que 1 THEN the system SHALL exibir a página 1. <!-- PAG-05 -->
6. IF `page` excede a última página THEN the system SHALL exibir a última página. <!-- PAG-06 -->
7. WHEN o usuário exclui o único item da última página THEN the system SHALL redirecionar para a nova última página. <!-- PAG-07 -->
8. WHILE a listagem cabe em uma única página, the system SHALL ocultar os controles de paginação. <!-- PAG-08 -->

**Independent Test**: Com 25 registros, a página 1 mostra 10, a 3 mostra 5; `?page=99` mostra a 3.

---

### P1: Docker com PostgreSQL ⭐ MVP

**User Story**: Como avaliador, quero subir tudo com um comando.

**Why P1**: Pedido do usuário; entrega reprodutível.

**Acceptance Criteria**:

1. WHEN `docker compose up --build` roda numa máquina limpa THEN the system SHALL subir o PostgreSQL 16 e o Tomcat 10.1 com a aplicação acessível em `http://localhost:8080/crud-manager/`. <!-- DOCK-01 -->
2. WHEN o volume do banco está vazio THEN the system SHALL executar `script-sql.sql` na inicialização, criando as tabelas `users`, `posts`, `companies`, `courses`, `enrollments` e os dados de exemplo. <!-- DOCK-02 -->
3. The system SHALL ler URL, usuário e senha do banco de variáveis de ambiente definidas no compose, sem credenciais no código. <!-- DOCK-03 -->
4. WHILE o PostgreSQL não está saudável, the system SHALL manter o contêiner da aplicação sem iniciar (healthcheck + `depends_on: service_healthy`). <!-- DOCK-04 -->
5. WHEN o contêiner da aplicação reinicia THEN the system SHALL preservar os dados gravados (volume nomeado do PostgreSQL). <!-- DOCK-05 -->
6. IF o banco ficar inacessível durante uma requisição THEN the system SHALL exibir mensagem de erro na página e registrar o erro no log, sem expor stack trace ao usuário. <!-- DOCK-06 -->
7. The system SHALL usar o driver JDBC do PostgreSQL e abandonar o do MySQL. <!-- DOCK-07 -->

**Independent Test**: `docker compose up --build` em diretório limpo, abrir a URL e entrar.

---

### P1: Banco e cabeçalho

**User Story**: Como avaliador, quero ver o SQL das tabelas novas e os cadastros novos no menu.

**Why P1**: Artefatos exigidos.

**Acceptance Criteria**:

1. The system SHALL definir em `script-sql.sql`, em sintaxe PostgreSQL, `courses` (FK `instructor_id` → `users`) e `enrollments` (FKs `student_id` → `users` e `course_id` → `courses`, `UNIQUE(student_id, course_id)`), e `users.password_hash` para o login. <!-- DB-01 -->
2. The system SHALL exibir no cabeçalho, para usuário logado, os links "Cursos" (`/courses`) e "Matrículas" (`/enrollments`) ao lado de Usuários, Posts e Empresas. <!-- NAV-01 -->
3. WHEN o usuário abre qualquer página protegida THEN the system SHALL exibir o cabeçalho com todos os cinco links de cadastro. <!-- NAV-02 -->
4. WHEN o usuário aciona o link "Posts" THEN the system SHALL abrir `/crud-manager/posts`. <!-- NAV-03 -->

**Independent Test**: Logar e clicar em cada link do cabeçalho.

---

### P2: Arquitetura hexagonal

**User Story**: Como desenvolvedor, quero domínio isolado de Servlet e JDBC para testar regras sem infraestrutura.

**Why P2**: Pedido do usuário; não altera o comportamento visível, mas organiza todo o código.

**Acceptance Criteria**:

1. The system SHALL organizar o código em `domain`, `application` (`port.in`, `port.out`, `service`), `adapter.in.web` e `adapter.out.persistence`. <!-- ARQ-01 -->
2. The system SHALL manter `domain` e `application` sem imports de `jakarta.servlet`, `java.sql` ou `javax.sql`. <!-- ARQ-02 -->
3. The system SHALL implementar validações, paginação, regra de matrícula e autenticação nos serviços da camada `application`. <!-- ARQ-03 -->
4. The system SHALL oferecer as portas de saída (interfaces DAO) para Users, Posts, Companies, Courses, Enrollments, com implementação JDBC PostgreSQL em `adapter.out.persistence`, registradas no `DAOFactory`. <!-- ARQ-04 -->
5. WHEN os Servlets recebem requisição THEN the system SHALL apenas traduzir HTTP para chamadas às portas de entrada e preparar a resposta (JSP). <!-- ARQ-05 -->
6. WHEN Usuários, Posts e Empresas são usados após a refatoração THEN the system SHALL manter o comportamento atual de listar, criar, editar e excluir, agora paginado e com login. <!-- ARQ-06 -->

**Independent Test**: `mvn test` roda os testes de serviço sem banco; o teste de arquitetura passa.

---

## Edge Cases

- IF a exclusão de um usuário viola FK (é instrutor, aluno, autor de post ou tem empresa) THEN the system SHALL manter o usuário e exibir mensagem de dados relacionados.
- IF o usuário envia id não numérico em editar/excluir THEN the system SHALL redirecionar para a listagem com mensagem de registro não encontrado, sem erro 500.
- WHEN o usuário envia dados com acentos e emojis THEN the system SHALL gravar e exibir exatamente o texto enviado (UTF-8).
- IF a sessão expira durante o uso THEN the system SHALL redirecionar para o login na próxima requisição.
- IF um POST de criar/editar/excluir chega sem sessão THEN the system SHALL redirecionar para o login sem alterar dados.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| AUTH-01 | P1: Login | Specify | Pending |
| AUTH-02 | P1: Login | Specify | Pending |
| AUTH-03 | P1: Login | Specify | Pending |
| AUTH-04 | P1: Login | Specify | Pending |
| AUTH-05 | P1: Login | Specify | Pending |
| AUTH-06 | P1: Login | Specify | Pending |
| AUTH-07 | P1: Login | Specify | Pending |
| AUTH-08 | P1: Login | Specify | Pending |
| AUTH-09 | P1: Login | Specify | Pending |
| AUTH-10 | P1: Login | Specify | Pending |
| AUTH-11 | P1: Login | Specify | Pending |
| AUTH-12 | P1: Login | Specify | Pending |
| AUTH-13 | P1: Login | Specify | Pending |
| CURSO-01 | P1: Cursos | Specify | Pending |
| CURSO-02 | P1: Cursos | Specify | Pending |
| CURSO-03 | P1: Cursos | Specify | Pending |
| CURSO-04 | P1: Cursos | Specify | Pending |
| CURSO-05 | P1: Cursos | Specify | Pending |
| CURSO-06 | P1: Cursos | Specify | Pending |
| CURSO-07 | P1: Cursos | Specify | Pending |
| CURSO-08 | P1: Cursos | Specify | Pending |
| CURSO-09 | P1: Cursos | Specify | Pending |
| CURSO-10 | P1: Cursos | Specify | Pending |
| CURSO-11 | P1: Cursos | Specify | Pending |
| CURSO-12 | P1: Cursos | Specify | Pending |
| MATR-01 | P1: Matrículas | Specify | Pending |
| MATR-02 | P1: Matrículas | Specify | Pending |
| MATR-03 | P1: Matrículas | Specify | Pending |
| MATR-04 | P1: Matrículas | Specify | Pending |
| MATR-05 | P1: Matrículas | Specify | Pending |
| MATR-06 | P1: Matrículas | Specify | Pending |
| MATR-07 | P1: Matrículas | Specify | Pending |
| MATR-08 | P1: Matrículas | Specify | Pending |
| MATR-09 | P1: Matrículas | Specify | Pending |
| MATR-10 | P1: Matrículas | Specify | Pending |
| MATR-11 | P1: Matrículas | Specify | Pending |
| MATR-12 | P1: Matrículas | Specify | Pending |
| PAG-01 | P1: Paginação | Specify | Pending |
| PAG-02 | P1: Paginação | Specify | Pending |
| PAG-03 | P1: Paginação | Specify | Pending |
| PAG-04 | P1: Paginação | Specify | Pending |
| PAG-05 | P1: Paginação | Specify | Pending |
| PAG-06 | P1: Paginação | Specify | Pending |
| PAG-07 | P1: Paginação | Specify | Pending |
| PAG-08 | P1: Paginação | Specify | Pending |
| DOCK-01 | P1: Docker | Specify | Pending |
| DOCK-02 | P1: Docker | Specify | Pending |
| DOCK-03 | P1: Docker | Specify | Pending |
| DOCK-04 | P1: Docker | Specify | Pending |
| DOCK-05 | P1: Docker | Specify | Pending |
| DOCK-06 | P1: Docker | Specify | Pending |
| DOCK-07 | P1: Docker | Specify | Pending |
| DB-01 | P1: Banco e cabeçalho | Specify | Pending |
| NAV-01 | P1: Banco e cabeçalho | Specify | Pending |
| NAV-02 | P1: Banco e cabeçalho | Specify | Pending |
| NAV-03 | P1: Banco e cabeçalho | Specify | Pending |
| ARQ-01 | P2: Hexagonal | Specify | Pending |
| ARQ-02 | P2: Hexagonal | Specify | Pending |
| ARQ-03 | P2: Hexagonal | Specify | Pending |
| ARQ-04 | P2: Hexagonal | Specify | Pending |
| ARQ-05 | P2: Hexagonal | Specify | Pending |
| ARQ-06 | P2: Hexagonal | Specify | Pending |

**Coverage:** 62 total, 0 mapped to tasks, 62 unmapped ⚠️

---

## Sources

- Enunciado da atividade (colado pelo usuário) - exige 2 CRUDs com 5+ campos de tipos distintos e FK por select, login, paginação, SQL, modelo, DAOs no DAOFactory, Servlets, JSPs, cabeçalho.
- https://github.com/emerson-prof-carvalho/crud-manager-public - projeto base (clonado em `~/crud-manager-base`).
- Pedido do usuário: tema plataforma de cursos, Docker, arquitetura hexagonal, PostgreSQL.

---

## Success Criteria

- [ ] Todos os 62 critérios têm check com teste que os prova.
- [ ] `docker compose up --build` em diretório limpo leva a login, cursos e matrículas funcionando.
- [ ] `mvn test` passa, incluindo o teste de arquitetura (ARQ-02).
