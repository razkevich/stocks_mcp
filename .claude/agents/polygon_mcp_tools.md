# Polygon MCP Tools Plan

Proposed MCP tool endpoints using Polygon.io. All endpoints require a `POLYGON_API_KEY` header/query.

## Fundamentals

- `fundamentals.get_ticker_details`
  - GET `/v3/reference/tickers/{ticker}`
  - Input: `ticker: string`
  - Output: company profile (name, SIC/NAICS, exchange, locale, description, market cap if available).

- `fundamentals.list_financials`
  - GET `/vX/reference/financials?ticker={ticker}&period=quarter&limit=50`
  - Input: `ticker: string`, optional `period`, `order`, `limit`, date filters
  - Output: standardized income/balance/cashflow statements with as-reported fields.
  - Note: API version varies (v2/v3/vX) by account; implement a small version probe.

## Reference

- `reference.list_dividends`
  - GET `/v3/reference/dividends?ticker={ticker}&ex_dividend_date.gte={YYYY-MM-DD}`
  - Input: `ticker: string`, optional date filters, `limit`
  - Output: dividend history and upcoming events.

- `reference.list_splits`
  - GET `/v3/reference/splits?ticker={ticker}`
  - Input: `ticker: string`
  - Output: split history.

## Calendar / Corporate Events

- `calendar.earnings`
  - GET `/vX/reference/earnings?ticker={ticker}&report_date.gte={YYYY-MM-DD}`
  - Input: `ticker: string`, optional date filters
  - Output: upcoming/recent earnings with EPS actual/estimate surprises.

## Market Status

- `market.status_now`
  - GET `/v2/marketstatus/now`
  - Output: current market open/close status and sessions.

- `market.upcoming_holidays`
  - GET `/v1/marketstatus/upcoming`
  - Output: upcoming market holidays and early/late sessions.

## Prices & Aggregates

- `aggregates.get_aggregates`
  - GET `/v2/aggs/ticker/{ticker}/range/{mult}/{timespan}/{from}/{to}`
  - Input: `ticker: string`, `mult: number`, `timespan: string (minute|hour|day|week|month)`
  - Output: OHLCV bars with VWAP, volume; supports pagination via `next_url`.

- `prices.previous_close`
  - GET `/v2/aggs/ticker/{ticker}/prev`
  - Output: previous day OHLC and volume.

## News

- `news.list`
  - GET `/v2/reference/news?ticker={ticker}&limit=50`
  - Input: `ticker: string`, optional `order`, `limit`, date filters
  - Output: recent articles with publisher, headlines, URLs.

---

## Implementation Notes

- Auth: pass `Authorization: Bearer ${POLYGON_API_KEY}` if supported, or `?apiKey=` query (per account setup).
- Rate limits: honor `429` with exponential backoff and `Retry-After` header.
- Pagination: follow `next_url` until exhausted; surface a `next_page_token` to callers.
- Normalization: return consistent field names; UTC timestamps; numbers as floats.
- Testing: add fixture-backed tests for each tool with recorded responses.

If you confirm the target language/runtime (TypeScript Node MCP vs Python), I’ll scaffold the client, schemas, and error handling.

