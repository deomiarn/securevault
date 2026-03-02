#!/usr/bin/env bash
set -euo pipefail

# ─── Colors ───────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# ─── PIDs to track ────────────────────────────────────────────────────
PIDS=()

cleanup() {
    echo -e "\n${RED}${BOLD}Shutting down all services...${NC}"
    for pid in "${PIDS[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            kill -- -"$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true
        fi
    done
    wait 2>/dev/null
    echo -e "${GREEN}All services stopped.${NC}"
    exit 0
}

trap cleanup SIGINT SIGTERM

# ─── Check prerequisites ─────────────────────────────────────────────
echo -e "${BOLD}SecureVault Development Starter${NC}"
echo -e "────────────────────────────────────────"

command -v docker >/dev/null 2>&1 || { echo -e "${RED}Docker is required but not installed.${NC}"; exit 1; }
command -v java >/dev/null 2>&1 || { echo -e "${RED}Java 21 is required but not installed.${NC}"; exit 1; }
command -v node >/dev/null 2>&1 || { echo -e "${RED}Node.js is required but not installed.${NC}"; exit 1; }

# ─── Start infrastructure (Postgres + Redis) ─────────────────────────
echo -e "\n${CYAN}[infra]${NC} Checking Postgres and Redis..."

if docker compose ps --status running 2>/dev/null | grep -q "securevault-db"; then
    echo -e "${CYAN}[infra]${NC} Postgres already running"
else
    echo -e "${CYAN}[infra]${NC} Starting Postgres and Redis..."
    docker compose up -d postgres redis
fi

if docker compose ps --status running 2>/dev/null | grep -q "securevault-redis"; then
    echo -e "${CYAN}[infra]${NC} Redis already running"
else
    echo -e "${CYAN}[infra]${NC} Starting Redis..."
    docker compose up -d redis
fi

# Wait for Postgres to be ready
echo -e "${CYAN}[infra]${NC} Waiting for Postgres to be ready..."
for i in $(seq 1 30); do
    if docker compose exec -T postgres pg_isready -U securevault >/dev/null 2>&1; then
        echo -e "${CYAN}[infra]${NC} ${GREEN}Postgres is ready${NC}"
        break
    fi
    if [ "$i" -eq 30 ]; then
        echo -e "${RED}Postgres failed to start within 30 seconds${NC}"
        exit 1
    fi
    sleep 1
done

# Wait for Redis to be ready
echo -e "${CYAN}[infra]${NC} Waiting for Redis to be ready..."
for i in $(seq 1 30); do
    if docker compose exec -T redis redis-cli ping >/dev/null 2>&1; then
        echo -e "${CYAN}[infra]${NC} ${GREEN}Redis is ready${NC}"
        break
    fi
    if [ "$i" -eq 30 ]; then
        echo -e "${RED}Redis failed to start within 30 seconds${NC}"
        exit 1
    fi
    sleep 1
done

# ─── Helper: run service with colored prefix ─────────────────────────
run_service() {
    local name="$1"
    local color="$2"
    local dir="$3"
    local cmd="$4"

    (
        cd "$dir"
        eval "$cmd" 2>&1 | while IFS= read -r line; do
            echo -e "${color}[${name}]${NC} $line"
        done
    ) &
    PIDS+=($!)
}

echo -e "\n${BOLD}Starting services...${NC}"
echo -e "────────────────────────────────────────"

# ─── Backend services ─────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

run_service "auth   " "$GREEN"   "$SCRIPT_DIR/auth-service"    "./gradlew bootRun --console=plain -q"
echo -e "${GREEN}[auth   ]${NC} Starting on port 8081..."

run_service "vault  " "$BLUE"    "$SCRIPT_DIR/vault-service"   "./gradlew bootRun --console=plain -q"
echo -e "${BLUE}[vault  ]${NC} Starting on port 8082..."

run_service "audit  " "$YELLOW"  "$SCRIPT_DIR/audit-service"   "./gradlew bootRun --console=plain -q"
echo -e "${YELLOW}[audit  ]${NC} Starting on port 8083..."

run_service "gateway" "$MAGENTA" "$SCRIPT_DIR/gateway"         "./gradlew bootRun --console=plain -q"
echo -e "${MAGENTA}[gateway]${NC} Starting on port 8080..."

# ─── Frontends ───────────────────────────────────────────────────────
run_service "react  " "$CYAN"    "$SCRIPT_DIR/frontend/react-app"    "npm run dev"
echo -e "${CYAN}[react  ]${NC} Starting on port 5173..."

WHITE='\033[0;37m'
run_service "angular" "$WHITE"   "$SCRIPT_DIR/frontend/angular-admin" "npx ng serve --port 4200"
echo -e "${WHITE}[angular]${NC} Starting on port 4200..."

echo -e "\n────────────────────────────────────────"
echo -e "${BOLD}All services starting!${NC}"
echo -e "  React Frontend:  ${CYAN}http://localhost:5173${NC}"
echo -e "  Angular Admin:   ${WHITE}http://localhost:4200${NC}"
echo -e "  API Gateway:     ${MAGENTA}http://localhost:8080${NC}"
echo -e "  Auth Service:    ${GREEN}http://localhost:8081${NC}"
echo -e "  Vault Service:   ${BLUE}http://localhost:8082${NC}"
echo -e "  Audit Service:   ${YELLOW}http://localhost:8083${NC}"
echo -e "\nPress ${RED}Ctrl+C${NC} to stop all services"
echo -e "────────────────────────────────────────\n"

# Wait for all background processes
wait
