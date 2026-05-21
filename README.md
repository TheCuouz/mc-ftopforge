## `/ftopforge hologram` — in-game hologram administration (v1.2.3+)

All subcommands require `ftopforge.admin` (default op).

| Subcommand | Effect |
|---|---|
| `/ftopforge hologram set` | Move hologram to where you are standing. Player only. |
| `/ftopforge hologram move <world> <x> <y> <z>` | Move hologram to explicit coordinates. World must be loaded. |
| `/ftopforge hologram info` | Show engine, location, enabled state, refresh interval, line count. |
| `/ftopforge hologram tp` | Teleport to the live hologram. Player only. |
| `/ftopforge hologram disable` | Remove hologram and set `holograms.enabled: false` in `config.yml`. |
| `/ftopforge hologram enable` | Spawn hologram and set `holograms.enabled: true`. |
| `/ftopforge hologram refresh` | Force an immediate redraw without waiting for the timer. |

Mutations write directly to `config.yml`. The first mutate per server process creates a defensive backup at `plugins/FTopForge/config.yml.bak`. Paper preserves YAML comments on save, so the shipped schema's documentation stays intact.

`/ftopforge reload` rebuilds the hologram service in addition to reloading config and messages — changes to `holograms.format`, `holograms.refresh-interval-seconds`, `holograms.engine`, or `holograms.enabled` take effect live with no restart.

Tab completion is available across the whole `/ftopforge` tree and `/ftop`.
