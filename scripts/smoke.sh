#!/usr/bin/env bash
# Smoke test ponta a ponta contra o stack docker compose (app em localhost:8080, PostgreSQL 16).
#
# Uso:  bash scripts/smoke.sh          roda todos os casos
#       bash scripts/smoke.sh AUTH-02  roda so o caso indicado (sai !=0 se o ID nao existir)
#
# Saida: "✓ <ID>" no stdout para cada caso aprovado; "✗ <ID>: motivo" no stderr para cada falha.
# O script continua apos uma falha e sai com codigo !=0 no fim. So usa curl, grep, sed, awk e docker.
#
# Premissa: o banco esta no estado do seed (script-sql.sql). Os casos que criam dados usam nomes com
# prefixo "smoke" e os removem; os casos destrutivos (PAG-08, MATR-11, CURSO-11) restauram o seed.
set -u

cd "$(dirname "$0")/.." || exit 2

BASE_URL="${BASE_URL:-http://localhost:8080}"
CTX="/crud-manager"
BASE="$BASE_URL$CTX"
PGUSER_="${POSTGRES_USER:-crud}"
PGDB_="${POSTGRES_DB:-crud_cursos}"
LOGIN_EMAIL="emerson@mail.com"
LOGIN_NAME="Emerson Carvalho"
LOGIN_PASSWORD="senha123"

T="$(mktemp -d)"            # cookie jars, flags e motivo da falha; removido no trap EXIT
J=""                         # cookie jar do caso corrente

# ---------------------------------------------------------------- infraestrutura

die() { printf '%s' "$*" >"$T/reason"; exit 1; }

psql_q() { docker compose exec -T db psql -U "$PGUSER_" -d "$PGDB_" -tAc "$1" 2>/dev/null; }

flat() { tr '\n\t\r' '   ' | sed -E 's/ +/ /g'; }

has()   { grep -qF -- "$2" <<<"$1" || die "${3:-faltou}: '$2'"; }
lacks() { grep -qF -- "$2" <<<"$1" && die "${3:-nao deveria conter}: '$2'"; return 0; }
hasre() { flat <<<"$1" | grep -qE -- "$2" || die "${3:-faltou}: /$2/"; }

# GET seguindo redirects, com o cookie jar do caso; imprime o corpo final.
GET() { curl -sS --max-time 60 -L -b "$J" -c "$J" "$BASE$1" || die "curl falhou em GET $1"; }

# POST de formulario (application/x-www-form-urlencoded) seguindo redirects; args: caminho k=v ...
POST() {
  local path="$1"; shift
  local args=() kv
  for kv in "$@"; do args+=(--data-urlencode "$kv"); done
  curl -sS --max-time 60 -L -b "$J" -c "$J" "${args[@]}" "$BASE$path" || die "curl falhou em POST $path"
}

# "<codigo> <Location>" sem seguir redirects. 1o arg: jar (vazio = sem cookie); 2o: caminho.
STATUS() {
  if [ -n "$1" ]; then
    curl -sS --max-time 60 -o /dev/null -w '%{http_code} %{redirect_url}' -b "$1" "$BASE$2"
  else
    curl -sS --max-time 60 -o /dev/null -w '%{http_code} %{redirect_url}' "$BASE$2"
  fi
}

# Login real (POST do formulario); define J com um jar novo.
login() {
  J="$(mktemp "$T/jar.XXXXXX")"
  local out
  out="$(curl -sS --max-time 60 -o /dev/null -w '%{http_code} %{redirect_url}' -b "$J" -c "$J" \
    --data-urlencode "email=$LOGIN_EMAIL" --data-urlencode "password=$LOGIN_PASSWORD" "$BASE/login")"
  [ "$out" = "302 $BASE/" ] || die "login esperava 302 para $BASE/, veio: $out"
}

wait_app() {
  local i code
  for i in $(seq 1 90); do
    code="$(curl -s --max-time 5 -o /dev/null -w '%{http_code}' "$BASE/login" 2>/dev/null)"
    [ "$code" = "200" ] && return 0
    sleep 2
  done
  return 1
}

wait_db_healthy() {
  local i st
  for i in $(seq 1 60); do
    st="$(docker inspect -f '{{.State.Health.Status}}' "$(docker compose ps -q db)" 2>/dev/null)"
    [ "$st" = "healthy" ] && return 0
    sleep 2
  done
  return 1
}

# Primeira pagina em que a lista contem o texto; define LOC_PAGE e LOC_ROW (linha <tr> achatada).
# 1o arg: URL da lista (ex.: /users); 2o: texto.
locate() {
  local p body
  LOC_PAGE=""; LOC_ROW=""
  for p in 1 2 3 4 5 6 7 8 9; do
    body="$(GET "$1?page=$p")"
    if grep -qF -- "$2" <<<"$body"; then
      LOC_PAGE="$p"
      LOC_ROW="$(flat <<<"$body" | sed 's/<tr>/\n<tr>/g' | grep -F -- "$2" | head -1)"
      return 0
    fi
  done
  die "'$2' nao apareceu em $1 (paginas 1-9)"
}

row_href() { grep -oE 'href="[^"]*/update\?[A-Za-z]+=[0-9]+"' <<<"$LOC_ROW" | head -1 | sed -E 's/^href="//; s/"$//'; }
row_id()   { grep -oE '[a-z]+-id="[0-9]+"' <<<"$LOC_ROW" | head -1 | grep -oE '[0-9]+'; }

nrows()       { grep -c 'btn-xs modal-remove' <<<"$1"; }
active_page() { flat <<<"$1" | grep -oE '<li class="active"><a[^>]*>[0-9]+' | grep -oE '[0-9]+$' | head -1; }
last_page()   { grep -oE '\?page=[0-9]+"' <<<"$1" | grep -oE '[0-9]+' | sort -n | tail -1; }
has_nav()     { grep -qF 'id="pagination-nav"' <<<"$1"; }

# Bloco <select name="x">...</select> achatado; 1o arg: corpo; 2o: name.
sel() { flat <<<"$1" | sed -E "s#.*(<select[^>]*name=\"$2\")#\1#; s#</select>.*##"; }
nopts() { grep -oE '<option value="[0-9]+"' <<<"$1" | wc -l | tr -d ' '; }

# Remove, pela UI, todos os itens de uma lista. 1o arg: URL da lista; 2o: URL de exclusao.
ui_delete_all() {
  local i body ids id
  for i in $(seq 1 30); do
    body="$(GET "$1")"
    ids="$(grep -oE '[a-z]+-id="[0-9]+"' <<<"$body" | grep -oE '[0-9]+')"
    [ -z "$ids" ] && return 0
    for id in $ids; do POST "$2" "id=$id" "page=1" >/dev/null; done
  done
  die "nao consegui esvaziar $1"
}

# Apaga o que os casos criam (prefixo smoke) e acerta as sequences.
purge_smoke() {
  psql_q "DELETE FROM enrollments WHERE course_id IN (SELECT id FROM courses WHERE title LIKE 'Smoke%') OR student_id IN (SELECT id FROM users WHERE email LIKE 'smoke-%@smoke.test');
          DELETE FROM courses WHERE title LIKE 'Smoke%';
          DELETE FROM posts WHERE content LIKE 'smoke%' OR user_id IN (SELECT id FROM users WHERE email LIKE 'smoke-%@smoke.test');
          DELETE FROM companies WHERE name LIKE 'Smoke%' OR user_id IN (SELECT id FROM users WHERE email LIKE 'smoke-%@smoke.test');
          DELETE FROM users WHERE email LIKE 'smoke-%@smoke.test';" >/dev/null
  psql_q "SELECT setval(pg_get_serial_sequence('users','id'), COALESCE(MAX(id),1), MAX(id) IS NOT NULL) FROM users;
          SELECT setval(pg_get_serial_sequence('posts','id'), COALESCE(MAX(id),1), MAX(id) IS NOT NULL) FROM posts;
          SELECT setval(pg_get_serial_sequence('companies','id'), COALESCE(MAX(id),1), MAX(id) IS NOT NULL) FROM companies;
          SELECT setval(pg_get_serial_sequence('courses','id'), COALESCE(MAX(id),1), MAX(id) IS NOT NULL) FROM courses;
          SELECT setval(pg_get_serial_sequence('enrollments','id'), COALESCE(MAX(id),1), MAX(id) IS NOT NULL) FROM enrollments;" >/dev/null
}

# Restaura cursos e matriculas ao estado do seed. Escolha: em vez de reaplicar o script inteiro (que derruba
# e recria tambem users/posts/companies, exigindo parar o app), extrai de script-sql.sql so os INSERTs de
# courses e enrollments e os reexecuta numa transacao depois de TRUNCATE ... RESTART IDENTITY. Usuarios,
# posts e empresas nao sao tocados, entao o banco volta a ser identico ao seed.
restore_seed() {
  {
    echo "TRUNCATE enrollments, courses RESTART IDENTITY;"
    awk '/^INSERT INTO (courses|enrollments)/{p=1} p{print} p && /;[[:space:]]*$/{p=0}' script-sql.sql
  } | docker compose exec -T db psql -U "$PGUSER_" -d "$PGDB_" -v ON_ERROR_STOP=1 -1 -q >/dev/null 2>&1 || return 1
  purge_smoke
  rm -f "$T/seed_dirty"
}

cleanup() {
  if [ -f "$T/db_stopped" ]; then
    docker compose start db >/dev/null 2>&1
    wait_db_healthy
    rm -f "$T/db_stopped"
  fi
  if docker compose ps -q db >/dev/null 2>&1 && [ -n "$(docker compose ps -q db 2>/dev/null)" ]; then
    if [ -f "$T/seed_dirty" ]; then restore_seed; else purge_smoke; fi
  fi
  rm -rf "$T"
}
trap cleanup EXIT

# ---------------------------------------------------------------- casos: base, login e menu

case_AUTH_02() {
  local b
  b="$(curl -sS --max-time 60 -w '\n%{http_code}' "$BASE/login")"
  [ "${b##*$'\n'}" = "200" ] || die "GET /login nao deu 200"
  hasre "$b" '<input[^>]*type="email"[^>]*name="email"' "campo email ausente"
  hasre "$b" '<input[^>]*type="password"[^>]*name="password"' "campo password ausente"
  hasre "$b" '<button[^>]*type="submit"[^>]*>Entrar</button>' "botao Entrar ausente"
}

case_AUTH_06() {
  login
  has "$(GET /)" "Perfil: $LOGIN_NAME" "menu sem o nome do usuario logado"
}

case_AUTH_07() {
  login
  cp "$J" "$J.old"                       # guarda o cookie da sessao antes de sair
  local r
  r="$(STATUS "$J" /logout)"
  [ "$r" = "302 $BASE/login" ] || die "/logout esperava 302 para $BASE/login, veio: $r"
  r="$(STATUS "$J.old" /users)"
  [ "$r" = "302 $BASE/login" ] || die "sessao antiga ainda vale: GET /users deu $r"
}

NAV_LABELS=("users:Usuários" "posts:Posts" "companies:Empresas" "courses:Cursos" "enrollments:Matrículas")

nav_check() {  # corpo
  local item path label
  for item in "${NAV_LABELS[@]}"; do
    path="${item%%:*}"; label="${item#*:}"
    hasre "$1" "href=\"$CTX/$path\"[^>]*>.{0,120}$label" "link $label ($CTX/$path) ausente no cabecalho"
  done
}

case_NAV_01() {
  login
  nav_check "$(GET /)"
}

case_NAV_02() {
  login
  local p
  for p in users posts companies courses enrollments; do
    nav_check "$(GET "/$p")" || exit 1
  done
}

case_NAV_03() {
  login
  local href out
  href="$(flat <<<"$(GET /)" | grep -oE 'href="[^"]*"[^>]*><span[^>]*/><strong>&nbspPosts' | head -1 | sed -E 's/^href="([^"]*)".*/\1/')"
  [ -n "$href" ] || die "link Posts nao encontrado no menu"
  out="$(curl -sS --max-time 60 -o /dev/null -w '%{http_code} %{url_effective}' -b "$J" -c "$J" "$BASE_URL$href")"
  [ "$out" = "200 $BASE/posts" ] || die "clicar em Posts ($href) esperava '200 $BASE/posts', veio: $out"
}

case_DOCK_01() {
  local code img ver
  code="$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' "$BASE/login")"
  [ "$code" = "200" ] || die "GET $BASE/login deu $code"
  img="$(docker compose ps --format '{{.Service}} {{.Image}}')"
  grep -qE '^db postgres:16$' <<<"$img" || die "servico db nao usa a imagem postgres:16 ($img)"
  ver="$(docker compose exec -T app catalina.sh version 2>/dev/null | grep 'Server version')"
  grep -qE 'Apache Tomcat/10\.1\.' <<<"$ver" || die "app nao roda Tomcat 10.1 ($ver)"
  ver="$(psql_q 'SHOW server_version')"
  grep -qE '^16\.' <<<"$ver" || die "PostgreSQL nao e 16 ($ver)"
}

case_DOCK_02() {
  local t n
  n="$(psql_q "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name IN ('users','posts','companies','courses','enrollments')")"
  [ "$n" = "5" ] || die "esperava 5 tabelas, existem $n"
  for t in users posts companies courses enrollments; do
    n="$(psql_q "SELECT count(*) FROM $t")"
    [ -n "$n" ] && [ "$n" -gt 0 ] 2>/dev/null || die "tabela $t sem linhas do seed ($n)"
  done
}

case_DOCK_04() {
  docker compose config | awk '
    /^  app:/ {app=1; next}
    /^  [a-z]+:/ && !/^  app:/ {app=0}
    app && /depends_on:/ {dep=1; next}
    app && dep && /condition: service_healthy/ {ok=1}
    END {exit ok ? 0 : 1}' || die "depends_on do app sem condition: service_healthy"
}

# ---------------------------------------------------------------- casos: Cursos, Matriculas e CRUDs existentes

case_CURSO_01() {
  login
  local b row
  b="$(GET /courses)"
  for h in Título Nível Preço Início Ativo Instrutor Editar Excluir; do
    has "$b" "<th>$h</th>" "coluna $h ausente"
  done
  [ "$(nrows "$b")" -gt 0 ] || die "lista sem linhas"
  [ "$(grep -cE '/course/update\?courseId=[0-9]+' <<<"$b")" = "$(nrows "$b")" ] || die "nem toda linha tem botao Editar e Excluir"
  hasre "$b" 'Java Básico</td> <td>Iniciante</td> <td>199\.90</td> <td>2026-03-02</td> <td>Sim</td> <td>Emerson Carvalho</td>' "linha do curso seed 'Java Básico' incorreta"
}

case_CURSO_02() {
  login
  local b f s n
  b="$(GET /course/form)"
  f="$(flat <<<"$b")"
  grep -qE '<input type="text"[^>]*name="title"' <<<"$f" || die "title nao e input text"
  grep -qE '<textarea[^>]*name="description"' <<<"$f" || die "description nao e textarea"
  grep -qE '<input type="number"[^>]*name="price"' <<<"$f" || die "price nao e input number"
  grep -qE '<input type="date"[^>]*name="startDate"' <<<"$f" || die "startDate nao e input date"
  grep -qE '<select[^>]*name="level"' <<<"$f" || die "level nao e select"
  grep -qE '<input type="checkbox"[^>]*name="active"' <<<"$f" || die "active nao e checkbox"
  grep -qE '<select[^>]*name="instructor"' <<<"$f" || die "instructor nao e select"
  s="$(sel "$b" instructor)"
  n="$(psql_q 'SELECT count(*) FROM users')"
  [ "$(nopts "$s")" = "$n" ] || die "select de instrutor tem $(nopts "$s") opcoes, o banco tem $n usuarios"
  while IFS= read -r nome; do
    grep -qF -- "$nome" <<<"$s" || die "instrutor '$nome' do banco ausente no select"
  done < <(psql_q 'SELECT nome FROM users ORDER BY id')
}

case_CURSO_04() {
  login
  local href b f row title price start level active instr
  locate /courses "Java Básico"
  href="$(row_href)"
  [ -n "$href" ] || die "link Editar nao encontrado na linha"
  b="$(GET "${href#"$CTX"}")"
  f="$(flat <<<"$b")"
  IFS='|' read -r title price start level active instr < <(psql_q "SELECT title||'|'||price||'|'||start_date||'|'||level||'|'||active||'|'||instructor_id FROM courses WHERE title='Java Básico'")
  grep -qE "name=\"title\"[^>]*value=\"$title\"" <<<"$f" || die "titulo nao preenchido"
  grep -qE "name=\"price\"[^>]*value=\"$price\"" <<<"$f" || die "preco nao preenchido ($price)"
  grep -qE "name=\"startDate\"[^>]*value=\"$start\"" <<<"$f" || die "data nao preenchida ($start)"
  grep -qE '<textarea[^>]*name="description"[^>]*>Fundamentos da linguagem Java\.</textarea>' <<<"$f" || die "descricao nao preenchida"
  grep -qE "<option value=\"$level\" selected" <<<"$f" || die "nivel $level nao selecionado"
  grep -qE "<option value=\"$instr\" selected" <<<"$(sel "$b" instructor)" || die "instrutor $instr nao selecionado"
  if [ "$active" = "true" ]; then
    grep -qE '<input type="checkbox"[^>]*name="active"[^>]*checked' <<<"$f" || die "checkbox ativo nao marcado"
  fi
}

case_MATR_01() {
  login
  local b
  b="$(GET /enrollments)"
  for h in Aluno Curso Data Situação Progresso Pago Editar Excluir; do
    has "$b" "<th>$h</th>" "coluna $h ausente"
  done
  [ "$(nrows "$b")" -gt 0 ] || die "lista sem linhas"
  [ "$(grep -cE '/enrollment/update\?enrollmentId=[0-9]+' <<<"$b")" = "$(nrows "$b")" ] || die "nem toda linha tem botao Editar e Excluir"
  hasre "$b" 'Luiza Carvalho</td> <td>Java Básico</td> <td>2026-02-20</td> <td>Ativa</td> <td>40%</td> <td>Sim</td>' "linha da matricula seed incorreta"
}

case_MATR_02() {
  login
  local b f su sc
  b="$(GET /enrollment/form)"
  f="$(flat <<<"$b")"
  su="$(sel "$b" student)"; sc="$(sel "$b" course)"
  [ "$(nopts "$su")" = "$(psql_q 'SELECT count(*) FROM users')" ] || die "select de aluno nao bate com users do banco"
  [ "$(nopts "$sc")" = "$(psql_q 'SELECT count(*) FROM courses')" ] || die "select de curso nao bate com courses do banco"
  while IFS= read -r t; do grep -qF -- "$t" <<<"$sc" || die "curso '$t' ausente no select"; done < <(psql_q 'SELECT title FROM courses ORDER BY id')
  while IFS= read -r t; do grep -qF -- "$t" <<<"$su" || die "aluno '$t' ausente no select"; done < <(psql_q 'SELECT nome FROM users ORDER BY id')
  grep -qE '<input type="date"[^>]*name="enrolledOn"' <<<"$f" || die "enrolledOn nao e input date"
  for s in ATIVA CONCLUIDA CANCELADA; do
    grep -qE "<input type=\"radio\" name=\"status\" value=\"$s\"" <<<"$f" || die "radio de situacao $s ausente"
  done
  grep -qE '<input type="range" min="0" max="100"[^>]*name="progress"' <<<"$f" || die "progress nao e range 0-100"
  grep -qE '<input type="checkbox"[^>]*name="paid"' <<<"$f" || die "paid nao e checkbox"
}

case_MATR_04() {
  login
  local b f href aluno curso
  locate /enrollments "Luiza Carvalho"
  href="$(row_href)"
  [ -n "$href" ] || die "link Editar nao encontrado"
  b="$(GET "${href#"$CTX"}")"
  f="$(flat <<<"$b")"
  IFS='|' read -r aluno curso < <(psql_q "SELECT student_id||'|'||course_id FROM enrollments WHERE id=${href##*=}")
  grep -qE "<option value=\"$aluno\" selected" <<<"$(sel "$b" student)" || die "aluno $aluno nao selecionado"
  grep -qE "<option value=\"$curso\" selected" <<<"$(sel "$b" course)" || die "curso $curso nao selecionado"
  grep -qE 'name="status" value="ATIVA"[^>]*checked' <<<"$f" || die "situacao ATIVA nao marcada"
  grep -qE 'name="progress"[^>]*value="40"' <<<"$f" || die "progresso 40 nao preenchido"
  grep -qE '<input type="date"[^>]*name="enrolledOn"[^>]*value="2026-02-20"' <<<"$f" || die "data nao preenchida"
  grep -qE '<input type="checkbox"[^>]*name="paid"[^>]*checked' <<<"$f" || die "pago nao marcado"
}

case_ARQ_06() {
  login
  local tag="smoke-arq06-$$" b before after href id

  # Usuarios
  before="$(psql_q 'SELECT count(*) FROM users')"
  b="$(POST /user/insert "name=Smoke Usuário $tag" "gender=F" "email=$tag@smoke.test" "password=Segredo123")"
  has "$b" "Usuário 'Smoke Usuário $tag' salvo com sucesso." "usuario: mensagem de criacao"
  locate /users "Smoke Usuário $tag"
  href="$(row_href)"; id="$(row_id)"
  b="$(GET "${href#"$CTX"}")"
  has "$b" "Smoke Usuário $tag" "usuario: formulario de edicao nao preenchido"
  b="$(POST /user/update "userId=$id" "name=Smoke Usuário $tag B" "gender=M" "email=$tag@smoke.test" "password=")"
  has "$b" "Usuário 'Smoke Usuário $tag B' atualizado com sucesso." "usuario: mensagem de edicao"
  locate /users "Smoke Usuário $tag B"
  b="$(POST /user/delete "id=$(row_id)" "page=$LOC_PAGE")"
  has "$b" "Usuário 'Smoke Usuário $tag B' deletado com sucesso." "usuario: mensagem de exclusao"
  after="$(psql_q 'SELECT count(*) FROM users')"
  [ "$before" = "$after" ] || die "usuarios: sobrou lixo ($before -> $after)"

  # Posts
  before="$(psql_q 'SELECT count(*) FROM posts')"
  b="$(POST /post/insert "content=$tag post" "user=1")"
  has "$b" "Post '$tag post' salvo com sucesso." "post: mensagem de criacao"
  locate /posts "$tag post"
  href="$(row_href)"; id="$(row_id)"
  b="$(GET "${href#"$CTX"}")"
  has "$b" "$tag post" "post: formulario de edicao nao preenchido"
  b="$(POST /post/update "postId=$id" "content=$tag post B" "user=2")"
  has "$b" "Post '$tag post B' atualizado com sucesso." "post: mensagem de edicao"
  locate /posts "$tag post B"
  b="$(POST /post/delete "id=$(row_id)" "page=$LOC_PAGE")"
  has "$b" "Post '$tag post B' deletado com sucesso." "post: mensagem de exclusao"
  after="$(psql_q 'SELECT count(*) FROM posts')"
  [ "$before" = "$after" ] || die "posts: sobrou lixo ($before -> $after)"

  # Empresas
  before="$(psql_q 'SELECT count(*) FROM companies')"
  b="$(POST /company/insert "name=Smoke Empresa $tag" "role=Tester" "start=2020-01-01" "end=" "user=1")"
  has "$b" "Empresa 'Smoke Empresa $tag' salva com sucesso." "empresa: mensagem de criacao"
  locate /companies "Smoke Empresa $tag"
  href="$(row_href)"; id="$(row_id)"
  b="$(GET "${href#"$CTX"}")"
  has "$b" "Smoke Empresa $tag" "empresa: formulario de edicao nao preenchido"
  b="$(POST /company/update "companyId=$id" "name=Smoke Empresa $tag B" "role=Lead" "start=2020-01-01" "end=2021-01-01" "user=1")"
  has "$b" "Empresa 'Smoke Empresa $tag B' atualizada com sucesso." "empresa: mensagem de edicao"
  locate /companies "Smoke Empresa $tag B"
  b="$(POST /company/delete "id=$(row_id)" "page=$LOC_PAGE")"
  has "$b" "Empresa 'Smoke Empresa $tag B' deletada com sucesso." "empresa: mensagem de exclusao"
  after="$(psql_q 'SELECT count(*) FROM companies')"
  [ "$before" = "$after" ] || die "empresas: sobrou lixo ($before -> $after)"
}

# ---------------------------------------------------------------- execucao

CASES=(
  AUTH-02 AUTH-06 AUTH-07 NAV-01 NAV-02 NAV-03 DOCK-01 DOCK-02 DOCK-04
  CURSO-01 CURSO-02 CURSO-04 MATR-01 MATR-02 MATR-04 ARQ-06
)

run_case() {
  local id="$1" fn reason
  fn="case_${id//-/_}"
  rm -f "$T/reason"
  ( "$fn" )
  if [ $? -eq 0 ]; then
    printf '✓ %s\n' "$id"
    return 0
  fi
  reason="$(cat "$T/reason" 2>/dev/null)"
  printf '✗ %s: %s\n' "$id" "${reason:-falhou}" >&2
  return 1
}

ensure_up() {
  local code
  code="$(curl -s --max-time 5 -o /dev/null -w '%{http_code}' "$BASE/login" 2>/dev/null)"
  if [ "$code" != "200" ]; then
    echo "Subindo o stack: docker compose up -d --build --wait" >&2
    docker compose up -d --build --wait >&2 || { echo "docker compose up falhou" >&2; exit 2; }
  fi
  wait_app || { echo "app nao respondeu 200 em $BASE/login" >&2; exit 2; }
}

main() {
  local failed=0 id
  if [ $# -gt 0 ]; then
    if ! declare -F "case_${1//-/_}" >/dev/null; then
      echo "caso desconhecido: $1" >&2
      exit 1
    fi
    CASES=("$1")
  fi
  ensure_up
  for id in "${CASES[@]}"; do
    run_case "$id" || failed=1
  done
  return $failed
}

main "$@"
status=$?
exit $status
