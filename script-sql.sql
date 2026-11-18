
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS companies;
DROP TABLE IF EXISTS posts;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    sexo CHAR(1) CHECK (sexo IN ('M', 'F')),
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL
);

CREATE TABLE posts (
    id SERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    post_date DATE NOT NULL,
    user_id INT NOT NULL REFERENCES users(id)
);

CREATE TABLE companies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    role VARCHAR(128) NOT NULL,
    start DATE NOT NULL,
    "end" DATE,
    user_id INT NOT NULL REFERENCES users(id)
);

CREATE TABLE courses (
    id SERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    price NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    start_date DATE NOT NULL,
    level VARCHAR(15) NOT NULL CHECK (level IN ('INICIANTE', 'INTERMEDIARIO', 'AVANCADO')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    instructor_id INT NOT NULL REFERENCES users(id)
);

CREATE TABLE enrollments (
    id SERIAL PRIMARY KEY,
    student_id INT NOT NULL REFERENCES users(id),
    course_id INT NOT NULL REFERENCES courses(id),
    enrolled_on DATE NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('ATIVA', 'CONCLUIDA', 'CANCELADA')),
    progress INT NOT NULL CHECK (progress BETWEEN 0 AND 100),
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (student_id, course_id)
);

INSERT INTO users (nome, sexo, email, password_hash) VALUES
('Emerson Carvalho', 'M', 'emerson@mail.com', '210000:bwqZ/8+HdsEayk5IdH3kqA==:Ue0ne4GdGZ4Yrx0rjSPLELEgOAtyAA+J49DL12ZPysw='),
('Luiza Carvalho', 'F', 'lu@mail.com', '210000:SE6pHA2SeqtZFYeYLfJIbw==:2yzlmZ7bfeVAhlSgr47WmfV/+o/h36RhOl4JWlJtg/I='),
('Elenice Carvalho', 'F', 'le@mail.com', '210000:KwpJi6+7Xo0CM7rlHPV6xA==:B4mcbBzjUVUQZ5rciY0kEGRik7BU80FOVaTNDEfS59I='),
('Noé Carvalho', 'M', 'noe@mail.com', '210000:KMc2mCwAb5DSKVXJpcvbyQ==:X7XvvozE6FpqTUDcc/hZtznDqrnhlGj0HfJ3iY3ksjA='),
('Rosânia Carvalho', 'F', 'ro@mail.com', '210000:Hn8cIw94dtC6mjyVL2Verg==:61cND3y7gv6j5eybGodL8SMbnkEmeFhnjsAOU0IGBjQ='),
('Ana Souza', 'F', 'ana@mail.com', '210000:5k7gBYOOnyJu+hys8FY0tw==:TuJ7VjsVtSn2Sk0bMn60sUGoMGvHWR7zd2tRIH5667s='),
('Bruno Lima', 'M', 'bruno@mail.com', '210000:5X+DnHHBvTW7+96Swykxrg==:5GVbkVE1fsKiPUej/CoJ7jjlfaA8/O9ayELJOIYIqTM='),
('Carla Mendes', 'F', 'carla@mail.com', '210000:rb5CxgQPjEv0ZZdcuqkmfg==:RIXMfehbNqLCM+5c8p9/9nWTa6OM53rDKEbKzF/HreY='),
('Diego Alves', 'M', 'diego@mail.com', '210000:F7yULm1r/7zCgRgNY4cpSw==:ZXlV1aphFkgaNqIXG4SpO08/iIgcB9a2TFFUOxYTvNI='),
('Elisa Ramos', 'F', 'elisa@mail.com', '210000:P3TsAQGBDIrmKS7cQbDIdQ==:hQPsRXueO9cMdhscnzntsIf3AXNAx9ktq4MOEl9vXxI='),
('Fábio Nunes', 'M', 'fabio@mail.com', '210000:WFXnLP+OrCKS7Y/AOkowGw==:xEIqjvuzZGrauoVYo+03coDGGH5TKqXmoc37jLETfO4='),
('Gabriela Rocha', 'F', 'gabriela@mail.com', '210000:IjpwlD2peUHfuMjUNG02Gw==:aeSHffhn1TFUO9DkIAuBg/E+Ung1mo3WgvrlVuU/Q10='),
('Heitor Costa', 'M', 'heitor@mail.com', '210000:qvesiAOVLa+75XbJFocL6w==:vDqQV78byZ2pKnAzpliFI+IHqdDYctbHAB8Sno6diwA='),
('Isabela Dias', 'F', 'isabela@mail.com', '210000:6t3zq3tYD90ZMq0RxGOyTw==:qOcngGbdpRiDSCw5WQUZgJY1NW2ZAYa3qJwwyMKeo/Q='),
('João Pereira', 'M', 'joao@mail.com', '210000:CLcOYSWge2HXDKriM/Kz7Q==:h5xsTiOWznKhsqVqNxQ3r4PbwlLKnJjaEheKtNhPi70=');

INSERT INTO posts (content, post_date, user_id) VALUES
('Olá do Emerson', CURRENT_DATE, 1),
('Olá da Luiza', CURRENT_DATE, 2),
('Olá da Elenice', CURRENT_DATE, 3),
('Olá do Noé', CURRENT_DATE, 4),
('Olá da Rosânia', CURRENT_DATE, 5),
('Olá da Rosânia 1', CURRENT_DATE, 5),
('Olá da Rosânia 2', CURRENT_DATE, 5),
('Olá da Rosânia 3', CURRENT_DATE, 5),
('Bem-vinda à plataforma, Ana', CURRENT_DATE, 6),
('Terminei o curso de Java', CURRENT_DATE, 7),
('Dúvida sobre JSP', CURRENT_DATE, 8),
('Novo curso de SQL no ar', CURRENT_DATE, 9),
('Boas-vindas ao grupo', CURRENT_DATE, 10);

INSERT INTO companies (name, role, start, "end", user_id) VALUES
('Acme Tecnologia', 'Desenvolvedor', '2018-02-01', '2020-06-30', 1),
('Beta Sistemas', 'Analista', '2020-07-01', NULL, 1),
('Gama Educação', 'Professora', '2015-03-10', NULL, 2),
('Delta Software', 'Estagiária', '2019-01-15', '2019-12-20', 3),
('Épsilon Dados', 'Engenheiro de Dados', '2021-05-03', NULL, 4),
('Zeta Consultoria', 'Consultora', '2017-09-01', '2021-01-31', 5),
('Eta Cloud', 'DevOps', '2022-02-14', NULL, 6),
('Theta Labs', 'Pesquisador', '2016-08-22', '2019-08-21', 7),
('Iota Mídia', 'Designer', '2020-10-05', NULL, 8),
('Kappa Finanças', 'Analista de Risco', '2018-04-02', '2022-03-31', 9),
('Lambda Games', 'Programador', '2021-11-08', NULL, 10),
('Mi Logística', 'Coordenadora', '2019-06-17', NULL, 11);

INSERT INTO courses (title, description, price, start_date, level, active, instructor_id) VALUES
('Java Básico', 'Fundamentos da linguagem Java.', 199.90, '2026-03-02', 'INICIANTE', TRUE, 1),
('Java Avançado', 'Streams, concorrência e JVM.', 349.90, '2026-04-06', 'AVANCADO', TRUE, 1),
('Servlets e JSP', 'Aplicações web com Jakarta EE.', 249.00, '2026-03-16', 'INTERMEDIARIO', TRUE, 2),
('SQL Essencial', 'Consultas, joins e modelagem.', 149.00, '2026-02-09', 'INICIANTE', TRUE, 3),
('PostgreSQL na Prática', 'Administração e desempenho.', 299.00, '2026-05-04', 'INTERMEDIARIO', TRUE, 3),
('Docker para Devs', 'Contêineres e compose.', 179.50, '2026-03-23', 'INICIANTE', TRUE, 4),
('Arquitetura Hexagonal', 'Portas e adaptadores.', 399.00, '2026-06-01', 'AVANCADO', TRUE, 4),
('HTML e CSS', 'Páginas responsivas.', 99.00, '2026-01-12', 'INICIANTE', FALSE, 5),
('JavaScript Moderno', 'ES2023 na prática.', 219.00, '2026-04-20', 'INTERMEDIARIO', TRUE, 5),
('Git e GitHub', 'Versionamento colaborativo.', 0.00, '2026-02-02', 'INICIANTE', TRUE, 6),
('Testes com JUnit', 'Testes automatizados em Java.', 189.00, '2026-05-18', 'INTERMEDIARIO', TRUE, 7),
('Segurança Web', 'OWASP e boas práticas.', 329.00, '2026-06-15', 'AVANCADO', FALSE, 8);

INSERT INTO enrollments (student_id, course_id, enrolled_on, status, progress, paid) VALUES
(2, 1, '2026-02-20', 'ATIVA', 40, TRUE),
(3, 1, '2026-02-21', 'CONCLUIDA', 100, TRUE),
(4, 1, '2026-02-22', 'CANCELADA', 10, FALSE),
(5, 2, '2026-03-30', 'ATIVA', 25, TRUE),
(6, 3, '2026-03-05', 'ATIVA', 60, TRUE),
(7, 3, '2026-03-06', 'CONCLUIDA', 100, TRUE),
(8, 4, '2026-01-30', 'CONCLUIDA', 100, FALSE),
(9, 4, '2026-01-31', 'ATIVA', 75, TRUE),
(10, 5, '2026-04-25', 'ATIVA', 5, FALSE),
(11, 6, '2026-03-10', 'ATIVA', 50, TRUE),
(12, 7, '2026-05-20', 'ATIVA', 0, FALSE),
(13, 9, '2026-04-10', 'CANCELADA', 15, FALSE),
(1, 10, '2026-01-25', 'CONCLUIDA', 100, TRUE);
