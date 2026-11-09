#!/bin/sh
# AppCDS 학습 실행: 실제 DB·비밀값 없이 애플리케이션을 띄워 기동과 Swagger 경로의 클래스를 적재한 뒤 종료하며 아카이브를 만든다.
# 아래 값은 전부 빌드 전용 자리 표시자이며, vercel 프로파일은 기동 중 DB에 연결하지 않는다.
# 사용: vercel-cds-train.sh <메인 클래스> <Swagger 그룹> <context-path>
set -eu

MAIN_CLASS=$1
SWAGGER_GROUP=$2
CONTEXT_PATH=$3
BASE_URL="http://127.0.0.1:18080$CONTEXT_PATH"

SPRING_PROFILES_ACTIVE=vercel \
PORT=18080 \
SERVER_SERVLET_CONTEXT_PATH="$CONTEXT_PATH" \
PGHOST_UNPOOLED=127.0.0.1 PGDATABASE=build_only PGUSER=build_only PGPASSWORD=build-only-not-real \
GREENBOWL_JWT_SECRET_KEY=build-only-not-real-build-only-not-real-build-only-not-real \
GREENBOWL_FRONTEND_URL=http://localhost:3000 \
GREENBOWL_FRONTEND_VERCEL_URL=http://localhost:3000 \
GREENBOWL_FRONTEND_SWYG_URL=http://localhost:3000 \
LLM_SERVER_API_URL=http://127.0.0.1:9 LLM_SERVER_STREAMING_ENDPOINT=/build-only LLM_SERVER_SSE_ENDPOINT=build-only LLM_SERVER_JSON_ENDPOINT=build-only \
RECIPE_LLM_TYPE=build-only RECIPE_LLM_TEMPLATE=build-only RECIPE_SECRET_KEY=build-only-not-real \
MENUS_LLM_TYPE=build-only MENUS_LLM_TEMPLATE=build-only MENUS_SECRET_KEY=build-only-not-real \
DETAILED_MENU_LLM_TYPE=build-only DETAILED_MENU_LLM_TEMPLATE=build-only DETAILED_MENU_SECRET_KEY=build-only-not-real \
java -XX:ArchiveClassesAtExit=/app/app.jsa -XX:TieredStopAtLevel=1 -XX:+UseSerialGC \
    @/app/classpath.args "$MAIN_CLASS" &
PID=$!

attempt=0
until curl -fsS -o /dev/null "$BASE_URL/swagger-ui/index.html"; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 240 ] || ! kill -0 "$PID" 2>/dev/null; then
        echo "AppCDS training: application did not start" >&2
        exit 1
    fi
    sleep 0.5
done

curl -fsS -o /dev/null "$BASE_URL/v2/api-docs?group=$SWAGGER_GROUP"
curl -fsS -o /dev/null "$BASE_URL/swagger-resources"
# 실패 응답 경로까지 적재한다
curl -s -o /dev/null "$BASE_URL/" || true

kill -TERM "$PID"
wait "$PID" || true

test -s /app/app.jsa
ls -la /app/app.jsa
