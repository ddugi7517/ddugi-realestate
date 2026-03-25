# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

뚜기부동산(Ddugi Real Estate) — 서울 아파트 실거래가 분석 시스템. Scrapes MOLIT (국토교통부) API data, runs price analysis, and sends Telegram notifications.

## Commands

### Backend (Spring Boot + Gradle)
```bash
cd backend
./gradlew bootRun                    # Run with local profile (default)
./gradlew build                      # Build jar
./gradlew test                       # Run all tests
./gradlew test --tests "ClassName"   # Run single test class
```

### Frontend (Next.js)
```bash
cd frontend
npm install
npm run dev     # Dev server at http://localhost:3000
npm run build   # Production build
npm run lint    # ESLint check
```

### Docker (full stack)
```bash
docker-compose up -d        # Start all services
docker-compose down         # Stop all services
docker-compose logs -f      # Follow logs
```

## Environment Setup

Create `.env` from `.env.example`:
```
MOLIT_API_KEY=<공공데이터포털 API 키>
TELEGRAM_BOT_TOKEN=<BotFather 토큰>
TELEGRAM_CHAT_ID=<채팅 ID>
```

Local dev requires PostgreSQL on `localhost:5432` (DB: `ddugi_realestate`, user: `ddugi`) and Redis on `localhost:6379`. Tests use H2 in-memory — no external DB needed.

## Architecture

### Data Flow
```
MOLIT API → MolitApiScraperService → PriceHistory (DB)
         → SnapshotService → PriceSnapshot (monthly aggregates)
         → PriceAnalysisService → PriceAnalysisResult
         → TelegramBotService → Telegram notifications
         → Frontend API → Next.js dashboard
```

### Backend Structure (`com.ddugi.realestate`)
- `scraper/` — MOLIT API client (XML parsing via WebFlux)
- `analysis/` — Price change rates, 6-month volatility (stddev/avg×100), recommendation logic
- `notification/` — Telegram bot integration and message building
- `entity/` — `Property`, `PriceHistory`, `PriceSnapshot`, `PriceAnalysisResult`, `ScrapingLog`

**Recommendation reasons:** `REBOUND_AFTER_DROP` (2 months down → bounce), `STABLE_UPTREND` (low volatility + 3 months up), `HIGH_TRADE_VOLUME` (≥2× regional avg), `UNDERVALUED` (≥15% below regional avg)

**Scheduler:** Scraping runs daily at 6:00 AM via `@Scheduled`

### Spring Profiles
- `local` — localhost PostgreSQL/Redis, file logging to `./logs`
- `docker` — service DNS names (`ddugi-postgres`, `ddugi-redis`)
- `test` — H2 in-memory with create-drop DDL

### Frontend Structure (`src/`)
- `app/` — Next.js App Router pages: `/`, `/analysis`, `/recommended`, `/volatile`, `/notification`
- `components/` — `PriceChart`, `PropertyCard`, `StatCard`, `RegionSelector`, `Sidebar`
- `lib/api.ts` — Axios client; all requests go to `/api` (proxied to `localhost:8081`)
- `types/index.ts` — Shared types including `REGION_MAP` (18 Seoul districts) and `RECOMMEND_REASON_MAP`

### Key API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/scraping/molit/trade` | Scrape trade data |
| POST | `/api/scraping/molit/rent` | Scrape rental data |
| POST | `/api/analysis/run` | Create snapshots + run analysis |
| GET | `/api/analysis/recommended` | Get recommended properties |
| GET | `/api/analysis/trend` | Price trend data |
| POST | `/api/notification/all` | Send all Telegram alerts |
