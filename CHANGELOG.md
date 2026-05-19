# Changelog

All notable changes to FTopForge are documented here.

## 1.1.0 (2026-05-19) — Bundle A: GUI top + breakdown

### Added
- GUI top con paginación (6 filas, top 45/página, cabezas del líder).
- GUI breakdown por faction (3 filas, summary + 5 categorías + comparación vs media top 10).
- Skull cache persistido a SQLite (`ftf_skull_cache`, TTL 24h, warmup 200).
- `/ftop gui` y `/ftop chat` para forzar output.
- Auto-detect player→GUI / console→chat (`gui.use-gui-on-ftop: auto`).
- Config nuevo: `gui.top.*`, `gui.breakdown.*`, `gui.skull-cache.*`.
- i18n strings nuevos en `messages-{es,en}.yml` bajo `gui.*`.

### Internal
- `TopCache.topRange(int, int)` + `TopCache.averagesTop10()`.
- `RecalcRunner` getters `lastFinishedAt()` + `isRunning()` + `nextScheduledAt()`.
- Tests nuevos: SkullCacheDaoTest, SkullCacheTest, TopGuiLayoutTest, MaterialKeyTest, BreakdownMathTest, FTopCommandRoutingTest. Total ~79 tests.

## 1.0.0 (2026-05-19) — Custom 7 baseline
- Initial release replacing cracked FactionsTop v5.0.7.2 on FACTIONS.
- See spec `docs/superpowers/specs/2026-05-19-ftopforge-design.md` for full §4.1 baseline scope.
