# Changelog

All notable changes to FTopForge are documented here.

## [1.2.0] - 2026-05-19 — Bundles B+C+D + Polish (Custom 9 / Sesión 12)

### Added
- **Holograms** (Bundle B): DH soft-dep autodetect (HD dropped — DEC-038), refresh scheduler, line formatter con 16 placeholders `%top1..N_*%` y `%next_recalc%`.
- **Discord webhook** (Bundle C): embed payload con JSON manual, 4 event types (recalc-finished, top1-changed, top10-shuffle, weekly-reset), cooldown configurable por event-type (default 6h shuffles, 0 weekly-reset). HttpURLConnection + Semaphore(2). Skipped en deploy (disabled in config).
- **PAPI placeholders** (Bundle C): 48 placeholders (top1..10 × {name, value, value_raw, leader} + 4 player + 4 meta) vía `PlaceholderResolver` pure-logic + `FtopForgePlaceholders` shim con lazy Class.forName load.
- **History** (Bundle D): tabla `ftf_history` (composite PK recalc_id+faction_id), `HistoryService` subscribed a `RecalcRunner.onFinish`, retención 90d con cleanup-on-recalc, ASCII chart 12×8 con footer Pico/Min/Δ, RFC-4180 CSV exporter streaming a `plugins/FTopForge/exports/history-YYYYMMDDHHmmss.csv`.
- **Forensics** (Bundle D): tabla `ftf_forensics`, BlockPlace/BlockBreak listeners (MONITOR priority), batch async writer (ConcurrentLinkedDeque + flush por size/interval + overflow protection), `FactionsClaimResolver` con LRU+TTL 30s cache (DEC-040). `ForensicsDao` con insertBatch / queryByFaction / deleteOlderThan.
- **Rewards + Season** (Bundle D): tablas `ftf_seasons` + `ftf_meta`, `CronScheduler` weekly con timezone IANA (5 tests), catch-up on startup si missed (DEC-039), `PayoutRunner` con freeze/unfreeze via `CalculationEngine.setFrozen` + `Bukkit.dispatchCommand` via `CommandDispatcher` interface. Manual `/ftopforge rewards run`. Soft season strategy (no worth reset, solo season marker).
- **`/ftopforge worth`** (Polish): item-in-hand o block-target lookup vía raytrace, MOB_SPAWNER detection via `BlockStateMeta`, formato chat con flag notListed.
- **Dynamic banner modules line** (Polish): placeholder runtime `${ftf.modules}` (DEC-042 — renombrado desde `${modules}` para evitar clash con built-in Maven `project.modules`). `RuntimeState` POJO populated durante `onEnable` wiring. Línea final: `modules: GUI | Holo(DH) | PAPI | History | Forensics | Rewards | bStats`.
- **bStats** (Polish): `org.bstats:bstats-bukkit:3.0.2` shadeado + relocado a `com.cristian.ftopforge.libs.bstats`. `BStatsHook.enable` con `pluginId=0` placeholder (DEC-041) + 3 custom charts (faction_count, recalc_duration_ms, modules_enabled drilldown).
- **+71 tests** (79 baseline → **150 tests**). 0 failures.

### Changed
- `RecalcRunner` — añadido `FinishCallback` nested interface + `onFinish(cb)` registration + invocation hook en el lambda (additive, sin breaking changes).
- `TopCache` — añadidos `previousTop1Id()` y `previousTop10Ids()` accessors con snapshot capture al inicio de `rebuild()` antes del write-lock.
- `CalculationEngine` — añadido `setFrozen(boolean)` / `isFrozen()` + freeze check en `tryStart` (usado por `PayoutRunner` durante weekly payouts).
- `FactionsUUIDHook` — añadidos `factionIdAt(World, int, int)` y `factionIdOf(UUID)`.
- `Dialect` — extendido con DDLs para 4 tablas nuevas + index methods.
- `JdbcRepository` — añadido `getDataSource()` accessor (los DAOs de Bundle B/C/D necesitan DataSource, no single Connection).
- `Banner.print(plugin, storageType)` — añadido overload `print(plugin, storageType, modulesLine)`; 2-arg shim sigue funcionando.

### Fixed
- Banner modules line resolution: Maven sustituía `${modules}` (built-in `project.modules`) por `[]` durante resource filtering en single-module projects. Renombrado runtime placeholder a `${ftf.modules}` (DEC-042).

### Schema (additive)
- `ftf_history` (composite PK recalc_id+faction_id, índices en finished_at y faction_id)
- `ftf_forensics` (autoincrement id, 3 índices: faction, chunk, player)
- `ftf_seasons` (autoincrement season_id, índice en ended_at)
- `ftf_meta` (KV store con VARCHAR meta_key PK + meta_value)

### Deploy
- Backups en `_migracion/backups/2026-05-19_FACTIONS_pre_bundle_bcd/` (ftopforge-1.1.0.jar + ftopforge-1.2.0-RC1.jar + FTopForge config dir).
- Deploy via `tiamat-deployer`: RC1 (commit `fa57e7d`) → smoke → fix banner placeholder → 1.2.0 final (commit `b6d6fc5`) → redeploy → Done en 13.986s, 0 SEVERE.
- Jar: `ftopforge-1.2.0.jar` ≈ 12.81 MB (HikariCP + sqlite-jdbc + bStats shaded).

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
