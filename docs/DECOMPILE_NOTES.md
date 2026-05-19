# FactionsTop v5.0.7.2 — Decompile Observations

**Source:** `Tiamat/FACTIONS/plugins/FactionsTop.jar` (320 KB), decompiled with CFR 0.152 on 2026-05-19. 113 .java files extracted to `docs/superpowers/decompile/factionstop-5.0.7.2/` (gitignored at the suite level — not committed).

**Why decompile:** identify exact behaviors of the cracked plugin so the rewrite (`mc-ftopforge`) hits feature parity, and confirm the `Thread.suspend()` bug locations the new `CalculationEngine` must replace.

**Package root:** `me.CustomEnchants.FactionsTopV5` (the v5 line, by SpigotMC user 97588). The jar phones home to `https://api.spigotmc.org/legacy/premium.php?user_id=97588&resource_id=20661` on every `onEnable` — we drop that entirely.

## Architecture overview

The cracked plugin is built around a singleton-everywhere god-object pattern:

- `Main extends JavaPlugin` holds **public static volatile** mutable state: `unsorted` (HashMap<factionId, FTop>), `tocalculate` (ArrayList<FLocation>), `players_to_calculate` (ArrayList<String>), and seven `boolean` flags (`is_calculating`, `can_start_calculating_spawners`, `can_start_sorting`, `can_start_finding_correct_chunks`, `can_start_calculating_money`, `can_start_calculating_richestmember`).
- Every utility class has a `public static FOO instance = new FOO()` field — no DI, no constructors.
- The recalc "pipeline" is five `Thread` subclasses chained through these boolean flags, plus three `Runnable` watchdogs scheduled at 20-tick (1s) intervals that poll the flags and call `.run()` on the next stage. **This is the bug surface we are replacing.**

## Recalc loop (the bug)

Five `Thread`-extending classes orchestrated by flag-polling watchdogs:

1. `Tasks.ProvideBasicFactionInformation` — iterates `Factions.getInstance().getAllFactions()`, creates/refreshes `FTop` entries in `Main.unsorted`, calls `fTop.resetValues()`. Then sets `can_start_finding_correct_chunks = true`.
2. `Tasks.ProvideCorrectChunksToCalculate` — iterates all non-wilderness/safezone/warzone factions, flattens every `faction.getAllClaims()` (FLocations) into `Main.tocalculate`. Sets `is_calculating=true`, calls `Main.calculatespawnervalue.run()`.
3. `Tasks.CalculateSpawnerValue` — **the throttled chunk processor**. See "Throttle bug" below.
4. `Tasks.CalculateMoneyBalances` — for each online FPlayer of each faction, async fetches `Main.economy.getBalance(...)` and accumulates onto `fTop.money_balance`. Same throttle pattern.
5. `Tasks.CalculateRichestMember` — sorts `fTop.cashed_balances`, picks top entry.

### Throttle bug (Thread.suspend / Thread.stop)

The watchdog runnables (`CheckIfCanStartCalculating`, `CheckIfCanStartLookingForCorrectChunks`, `CheckIfCanStartSorting`) are scheduled via `scheduleSyncRepeatingTask(..., 20L, 20L)` and just call `.run()` (not `.start()`) on the Thread subclasses — so the "Threads" are actually executed on the main thread synchronously.

When a stage is done, it calls `this.suspend()` (a method of `java.lang.Thread`):

- `CalculateSpawnerValue.java:149` — `CalculateSpawnerValue.this.suspend();` inside the `BukkitRunnable.runTaskTimer(plugin, 20L, 40L)` exit branch.
- `CalculateMoneyBalances.java:86` — `CalculateMoneyBalances.this.suspend();`
- `CalculateRichestMember.java:31` and `:39` — two `CalculateRichestMember.this.suspend();` calls.

`Thread.suspend()` was deprecated in JDK 1.2, marked for removal in JDK 19, and **throws `UnsupportedOperationException` on JDK 20+**. The whole pipeline silently dies the first time it tries to "pause" itself on Paper 1.21 / JDK 21.

`Main.onDisable` also calls `.stop()` on all five Thread fields (lines 125, 128, 131, 134, 137 of `Main.java`) — `Thread.stop()` is in the same deprecate-then-remove pipeline and is **also already removed** in JDK 21 (UnsupportedOperationException). So `/reload`, server shutdown, or any plugin disable on JDK 21 throws too.

### Chunks-per-tick rate

`CalculateSpawnerValue.run()` reads `FileUtil.instance.chunks_calculated_per_second` from `Config.yml` (default **20**), then schedules a `BukkitRunnable.runTaskTimer(plugin, 20L, 40L)` — i.e. period 40 ticks (2 seconds), not 20. So the config name lies: it's chunks-per-2-seconds in practice. For each tick, it calls `calculateChunk(fLocation)` up to N times, then removes those from `tocalculate`. Each `calculateChunk` schedules its own `BukkitRunnable.runTaskAsynchronously` — meaning **the chunk's tile entities are read on the main thread (synchronously, via `chunk.getTileEntities()` in the outer method), but the per-block processing happens on an async pool**. This is unsafe: `BlockState[]` from a sync `getTileEntities()` is snapshot-safe to iterate async, but the scanner calls `blockState.getBlock()` and `chest.getInventory().getContents()` from the async task — those are NOT thread-safe on Paper.

### Chunk iteration strategy (relevant to plan Task 15)

`CalculateSpawnerValue.calculateChunk` calls `chunk.getTileEntities()` — **only tile entities**, NOT all blocks. So the rewrite plan's "tile entities only" decision in Task 15 matches the legacy behavior exactly. No need to reconsider.

## Value calculation

- **Block scan:** `Tasks.CalculateSpawnerValue.calculateChunk` does `instanceof` checks for `Beacon`, `Chest`, `CreatureSpawner`, `Dropper`, `Dispenser`, `Hopper`. Each adds to its specific counter on `FTop` and to `block_balance` from `PriceManager.instance.<BlockType>`. Beacons and trapped chests are counted but `TrappedChest` doesn't appear in the scan (only loaded into `PriceManager` — possibly dead config).
- **Spawner scan:** `CalculateSpawnerValue` line 89-93: casts `BlockState` to `CreatureSpawner`, calls `block.getSpawnedType().getTypeId()` (returns a `short` — the legacy 1.7–1.12 entity numeric ID), then `Util.instance.addSpawner(fTop, s)` does a switch on the short. **This is bound to the legacy `EntityType.getTypeId()` API removed in 1.13+**; it's broken on 1.21 anyway. The rewrite must switch to `EntityType` enum directly.
- **Item scan:** Four sibling classes in `Utils.Scanners.` — `ChestScanner`, `HopperScanner`, `DropperScanner`, `DispenserScanner`. Each iterates `inventory.getContents()` (an `ItemStack[]`) and does a chain of `if (material == Material.XYZ)` accumulating onto `fTop.item_worth` and the per-item counter. `ChestScanner` also handles `DoubleChest` (gets full `inventory.getInventory()` vs `getBlockInventory()`). The item set is hardcoded: bedrock, TNT, obsidian, diamond/iron/gold/emerald/redstone/coal blocks, all diamond/iron/gold armor pieces and tools, ender dragon egg, golden apple, "god apple" (notch apple), and 27 spawner items. ~85 distinct item slots, hardcoded into `FTop` as public `int` fields.
- **Balance aggregation:** `Tasks.CalculateMoneyBalances` uses Vault: `Main.economy.getBalance((OfflinePlayer)player)`. Only online players are summed — offline player balances are NEVER queried (so `money_balance` is the live online-member sum, not the true faction wealth). Per-player balance is cached into `fTop.cashed_balances` (HashMap<playerName, Double>) for the `CalculateRichestMember` step.

## Persistence

**In-memory primary + optional MySQL mirror.** No SQLite, no flat-file YAML for snapshots.

- `Main.unsorted = new HashMap<String, FTop>()` is the authoritative store — declared `public static volatile`. Lost on restart.
- `Utils.Files.MySQLFileUtil` (singleton) reads `plugins/FactionsTop/MySQL.yml` with keys `use-mysql`, `host`, `user`, `password`, `database`, `table`, `port`. Uses the bundled `code.husky.mysql.MySQL` connection wrapper (shaded — see `code/husky/`).
- `Runnables.MySQLDataTask` is the only writer: scheduled via `scheduleAsyncRepeatingTask(plugin, 0L, mysql_save_interval_in_seconds * 20)` (default interval 300s = 5 min). On each run it `TRUNCATE`s the table, then for each `FTop` calls `fTop.writeToMySQLServer()` which does a hand-built `INSERT INTO <table>(36 cols) VALUES (36 ?)`. Cols: `FactionName, FactionLeader, TotalWorth, BlockWorth, SpawnerWorth, ItemWorth, BalanceWorth, RichestMember, RichestMemberBalance, CreeperSpawner..VillagerSpawner` (27 individual spawner counts).
- Schema uses `FLOAT(6)` for all money values — **lossy past ~7 significant digits**, so wealth >$9.9M loses precision. Rewrite should use `DOUBLE` or `BIGINT`.
- No connection pool. `MySQLFileUtil.c` is a single `Connection` opened on enable and closed on disable. No reconnect logic. Per-FTop INSERTs are serial on a single connection from an async repeating task.
- No `UPDATE` path — truncate-and-reinsert every cycle. Concurrent readers of the `data` table see an empty table for the brief moment between TRUNCATE and the INSERTs completing.

**Implication for plan Task 18 (Dialect):** the legacy uses MySQL, not SQLite. But the legacy MySQL schema is so weak (FLOAT, TRUNCATE+INSERT, no pool) that "feature parity" with it is a low bar. The rewrite's planned HikariCP + dialect abstraction is straight-up better; no Dialect ordering preference is implied — SQLite-default is fine as long as MySQL works.

## Commands

- `/ftop [page]` — `Commands.FTopCMD.onCommand`. Player-only. Parses arg[0] as int page (defaults to 1 on parse failure or no args). Delegates to `UserInterface.instance.send(player, page)`. `UserInterface.send` schedules a `runTaskLaterAsynchronously` with delay = `FileUtil.ftop_command_delay` (default 5 ticks) — so the command intentionally lags by 250ms. `sendData` paginates `SortingUtil.instance.getFactions()` (which re-sorts the entire `Main.unsorted` map on every command invocation by `getTotalValue()` descending), then sends a `mkremins.fanciful.FancyMessage` per line with a giant hover-tooltip that has ~95 `%placeholder%` replacements (faction name, leader, blocks total, money total, spawners total, item total, richest member, every spawner type count, every item count). Default line format: `&6%rank%. &b%factionname% &a$ %value%`. Header: `&4&l&m-------------&e&lPage &c&l%page%&4&l&m-------------`.
- `/fvalue [faction]` — `Commands.FValueCMD.onCommand`. Player-only (console gets a "no console" message). No args → looks up the player's own faction. With args[0] → looks up by tag via `Factions.getInstance().getByTag(...)`. Builds the same monstrous FancyMessage tooltip as `/ftop`. Note: number formatting is `NumberFormat.getNumberInstance(Locale.US).format(long)` — comma separators, integer only.
- `/ftopadmin [sub]` — `Commands.FTopAdminCMD`. Subcommands wired in constructor: `Author`, `ForceRecalculate`, `Reload`, `SetChunksPerSecond`, `SetPlayersPerSecond`, `WriteToMySQL`. With no args, dumps the version banner + the SpigotMC user URL + lists subcommands the sender has permission for. The version banner literally hardcodes `Messages.version_number` and the registered SpigotMC user ID — anti-leak theater that does nothing.
- Permission gating is per-subcommand via `CMD.require_permission` / `CMD.permission` fields on each subcommand class.

## Concurrency primitives

- **No locks, no AtomicReference, no ConcurrentHashMap.** Just `public static volatile HashMap<String, FTop>` and prayer. Multiple async tasks mutate the same `FTop` fields (`block_balance`, per-spawner counters) without synchronization. Race conditions on every recalc cycle; counters can desync from totals.
- Five `Thread` fields, but `.run()` is called instead of `.start()`, so the actual concurrency is Bukkit-scheduler driven: `scheduleSyncRepeatingTask` for the watchdogs, `runTaskTimer` for the chunk loop, `runTaskAsynchronously` for per-chunk processing.
- The async per-chunk task does Bukkit calls (`Block.getBlockData()`, `Chest.getBlockInventory()`, `DoubleChest.getInventory()`) that are NOT thread-safe — works by luck on Spigot, will deadlock or NPE on modern Paper.

**Implication for plan Task 17 (TopCache RW lock):** the legacy has zero concurrency safety. Our RW-lock plan is genuine value-add, not just parity.

## Config files written on first run

`Config.yml` (set in `FileUtil.setup`):

- `Options.broadcast-when-starting-calculation` = true
- `Options.broadcast-when-stopping-calculation` = true
- `Options.factions-per-page` = 10
- `Options.chunks-calculated-per-second` = 20
- `Options.player-balances-calculated-per-second` = 20
- `Options.ftop-command-delay` = 5
- `Options.ftop-recalculation-delay-in-seconds` = 1800 (30 min)
- `Options.include-block-value-in-total-worth` = true
- `Options.include-spawner-value-in-total-worth` = true
- `Options.include-player-balance-in-total-worth` = true
- `Options.include-itemworth-in-total-worth` = true
- `Options.prevent-ftop-usage-while-calculating` = true
- `Options.mysql-save-interval-in-seconds` = 300
- `Blocks.{Beacon:25000, Chest:20, Dispenser:1000, Dropper:1000, Hopper:5000, TrappedChest:20}`
- `Spawners.{Bat:10k, Blaze:350k, Chicken:5k, Cow:50k, Creeper:500k, Enderman:200k, Ghast:20k, Giant:10k, Horse:100k, IronGolem:1M, MagmaCube:10k, MushroomCow:100k, Ocelot:10k, Pig:10k, Pigman:750k, Rabbit:10k, Sheep:10k, Skeleton:70k, Snowman:10k, SilverFish:1.5M, Slime:10k, Spider:45k, Squid:10k, Villager:120k, Witch:15k, Wolf:10k, Zombie:70k}`

`Items.yml` — separate file written by `Utils.Files.ItemsFileUtil` (not yet read for this notes pass; defaults can be cross-referenced when seeding plan Task 6 `items.yml`).

`Language.yml` — written by `FileUtil.setup` if missing. Sections: `Calculation.{starting, finished}`, `Command-FTop.{line, header, command-preventage, tooltip[]}`, `Command-FValue.{console-sender, your-faction-not-synced, other-faction-not-synced, faction-doesent-exist, no-faction, line-self, line-others, tooltip[]}`, `Command-Reload.{no-permission, reloaded}`. Default tooltip lists from `getDefaultFTopToolTip()` / `getDefaultFValueToolTip()` are seeded with ChatColor `&`-codes — useful reference for `messages-es.yml` / `messages-en.yml` in plan Task 6, but our MiniMessage rewrite will not preserve `&` codes.

`MySQL.yml` — defaults: host=127.0.0.1, user=root, password=1234, database=FactionsTop, table=data, port=3306, use-mysql=false.

## Behaviors we will NOT replicate

- `Thread.suspend()` throttle — replaced by `BukkitScheduler.runTaskTimerAsynchronously` with chunks-per-tick token-bucket in `CalculationEngine` (plan Task 20).
- `Thread.stop()` on disable — replaced by `BukkitTask.cancel()` + a `CountDownLatch` for in-flight drains.
- SpigotMC license phone-home in `Main.loadConfig0()` — dropped.
- `public static volatile` god-state in `Main` — replaced by injected services (FTopForgePlugin + DI in plan Task 25).
- Bukkit API calls from async tasks (e.g. `chest.getInventory()` from `runTaskAsynchronously`) — replaced by snapshot-first-then-async pattern (read tile entity state on main, value-compute on async).
- TRUNCATE-then-INSERT MySQL writer — replaced by `INSERT ... ON DUPLICATE KEY UPDATE` (MySQL) / `INSERT OR REPLACE` (SQLite) via plan Task 18 dialect layer.
- `FLOAT(6)` money columns — use DOUBLE / decimal.
- `EntityType.getTypeId()` (removed in 1.13+) — use `EntityType` enum names from `getSpawnedType()`.
- `ChatColor.translateAlternateColorCodes('&', ...)` everywhere — MiniMessage exclusively per suite convention.
- `mkremins.fanciful.FancyMessage` tooltips — Adventure `Component.hoverEvent(...)` instead.
- Singleton-everywhere pattern — constructor injection.

## Behaviors that ARE worth replicating

- Tooltip-on-hover for each ftop row with per-block / per-spawner / per-item breakdown (the legacy default tooltip is a good template — see `FileUtil.getDefaultFTopToolTip` lines 277-305 and `getDefaultFValueToolTip` line 307+).
- Default block/spawner price table in `Config.yml` (above) — sane starting values; seed into plan Task 5 `config.yml`.
- Default item price table (deferred to plan Task 6 — re-read `ItemsFileUtil.java` then).
- 30-minute recalc cadence (default `ftop-recalculation-delay-in-seconds = 1800`).
- 10 factions per page pagination.
- Sort key: descending `getTotalValue()` where total = block + spawner + balance + item (each toggleable).
- "Calculation starting / finished" broadcast messages (toggleable).
- `prevent-ftop-usage-while-calculating` flag — when true, `/ftop` rejects with a friendly message during recalc.
- Subcommand split: `/ftop`, `/fvalue`, `/ftopadmin reload|forcerecalc|setchunkspersecond|setplayerspersecond|writetomysql`. Our plan splits this differently (`/ftop` + `/ftopforge recalc|reload|version`); the legacy admin set is more granular but covers the same surface.
- Per-faction skip for wilderness/safezone/warzone (`Faction.isWilderness() || isSafeZone() || isWarZone()`).
- Balance-from-online-members-only is a server-owner-tunable choice, not a bug — many servers prefer it. Make it configurable.

## Cross-task implications for the plan

1. **Task 14 (Valuators):** the legacy hardcodes ~85 items + 27 spawner types as integer fields on `FTop`. Our snapshot data class (Task 13) should hold these as `Map<Material, Long>` and `Map<EntityType, Long>` rather than 100+ fields. The default seed file for Task 6 (`items.yml`) can be lifted from `Utils.Files.ItemsFileUtil` (read on next pass).
2. **Task 15 (ChunkValuator):** confirmed "tile entities only" matches legacy. No change needed.
3. **Task 16 (BalanceAggregator):** legacy only sums online members. Default to "online only" with a config flag `include-offline-balances: false` for parity, but expose the toggle since it's a common request.
4. **Task 18 (Dialect):** no MySQL ordering preference from legacy — its MySQL schema is so weak that parity is trivial. SQLite-default + MySQL-opt-in is fine.
5. **Task 23 (`/ftop`):** match the legacy line/header format and tooltip placeholders for migration ease — server owners with custom `Language.yml` will want to port their templates. Document the placeholder mapping (legacy `%richestmember%` etc. → MiniMessage `<richestmember>` or similar).
6. **Task 24 (`/ftopforge`):** consider adding `setchunkspersecond` / `setplayerspersecond` / `writetomysql` subcommands for legacy admin parity, or document why we dropped them (config-reload is enough; chunks-per-second is now a hot-reload field).
