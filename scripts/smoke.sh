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

# ---------------------------------------------------------------- execucao

CASES=(
  AUTH-02 AUTH-06 AUTH-07 NAV-01 NAV-02 NAV-03 DOCK-01 DOCK-02 DOCK-04
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
