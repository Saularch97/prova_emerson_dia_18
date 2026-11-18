# STATE

## Decisions

### AD-001
- **Decision**: Todo o projeto segue arquitetura hexagonal em `crudmanager.domain`, `crudmanager.application` (`port.in`, `port.out`, `service`) e `crudmanager.adapter` (`in.web`, `out.persistence`, `out.security`), com `DAOFactory` como composition root dos DAOs.
- **Reason**: Pedido do usuário ("siga hexagonal"); regras (validação, paginação, login) ficam testáveis sem Servlet nem banco.
- **Trade-off**: Refatora Users, Posts e Companies, que já funcionavam; mais classes que o tutorial da disciplina.
- **Scope**: todo o código Java.
- **Date**: 2026-11-12
- **Status**: superseded by AD-002

### AD-002
- **Decision**: Arquitetura em camadas controller → service → dao em `crudmanager.controller`, `service` (+ `service.impl`), `dao` (+ `dao.jdbc`), `model`, `exception`, `security` e `config` (`ServiceLocator` monta as dependências). Serviços e DAOs são interfaces injetadas por construtor (DIP).
- **Reason**: Pedido do usuário: trocar a hexagonal por camadas com controller, service e DAO, mantendo DI, SOLID e boas práticas.
- **Trade-off**: Perde-se a nomenclatura de portas/adaptadores; o comportamento e as URLs não mudam.
- **Scope**: todo o código Java; `ArchitectureTest` (ARQ-01, 02, 05, 07) verifica as dependências entre camadas.
- **Date**: 2026-11-18
- **Status**: active

### AD-002
- **Decision**: PostgreSQL 16 via JDBC puro (sem ORM, sem pool), Maven, WAR `crud-manager.war` em Tomcat 10.1/JDK 21, configuração do banco por variáveis de ambiente.
- **Reason**: Pedido do usuário; mantém o JDBC didático do projeto base e o contexto `/crud-manager`.
- **Trade-off**: Conexão por operação (sem pool), como no projeto base.
- **Scope**: persistência, build e execução.
- **Date**: 2026-11-12
- **Status**: active

### AD-003
- **Decision**: Cada teste leva como `@DisplayName` o ID do critério (ex.: `AUTH-03 login correto cria sessão`) e um listener JUnit imprime `✓ <displayName>`; o smoke test em shell imprime `✓ <ID>`.
- **Reason**: O Surefire não lista testes individuais; os checks do tasks.md provam um critério por linha impressa.
- **Trade-off**: Convenção de nome obrigatória em todo teste.
- **Scope**: testes e `scripts/smoke.sh`.
- **Date**: 2026-11-12
- **Status**: active

## Handoff

none
