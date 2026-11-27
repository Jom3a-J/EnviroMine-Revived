# EnviroMine Revived

A small, vanilla-plus survival addition for **Minecraft 1.21.1 (NeoForge)**: body temperature and
hydration (thirst). It is a port of the temperature and hydration systems from the abandoned
EnviroMine mod for Minecraft 1.7.10, rebuilt for modern Minecraft with gentler defaults.

> Status: 1.0.0, feature complete. The original author's CurseForge page invites updates and puts the
> source under CC BY-NC-SA 4.0 (see docs/LICENSE_NOTES.md).

## Features

- **Body temperature**, pushed by the biome, time of day, rain, shade, height, nearby heat and cold
  (fire, lava, furnaces, torches, ice, snow), water, sprinting, sleeping, nearby animals and what
  you wear. It changes slowly, over minutes.
- **Hydration**, which drains over about an hour, faster when hot, sprinting or in a desert.
- **Water types**: clean, cold, dirty (may make you sick) and salty (makes you thirstier).
- **A compact HUD** above the hunger bar that can be moved, hidden or switched to numbers.
- **Gentle, telegraphed consequences**: a warning first, then a mild effect, then slow damage with
  vanilla starvation's difficulty limits.
- **Tunable**: every number in a server config, block, armour and biome values in data packs.
  The server decides everything; clients only display it.

Everything else from the original (cave physics, gases, sanity, …) is out of scope. See
[ROADMAP.md](ROADMAP.md).

## How it plays

- **Staying comfortable:** temperate biomes need no attention. In the cold, wear leather (it
  insulates without ever overheating you) and stay near a campfire or torches. In the heat, keep
  drinking: sweating holds your temperature while you are well hydrated.
- **Drinking:** right-click water with an empty hand for a sip, or a water cauldron (lowers it by
  one level). Water bottles restore more.
- **Water types:** swamp water and deep cave water are dirty, ocean and beach water is salty,
  snowy or high-mountain water is cold. Bottles show their type in the tooltip. Boil water to make
  it safe: a cauldron over a campfire, fire, magma or lava always gives clean water.
- **Levels:** level 1 is a warning (HUD colour and a message); level 2 gives Heatstroke,
  Hypothermia or Dehydration; level 3 also deals half a heart every 4.5 s. Fire Resistance
  prevents heatstroke.

See [docs/BALANCE.md](docs/BALANCE.md) for how long each biome takes to become a problem.

## Configuration

- **Client** (`config/enviromine_revived-client.toml`): HUD position, offsets, number mode, °C/°F.
- **Server** (`config/enviromine_revived-server.toml`): switch temperature, hydration, effects,
  damage and drinking by hand on or off, and every balance number. Reloads live.
- **Data packs**: heat of blocks, effect of armour, per-biome overrides. See
  [docs/DATAPACKS.md](docs/DATAPACKS.md).

Both config files can also be edited in game: Mods → EnviroMine Revived → Config.

## Credits

This mod is based on **EnviroMine** by **Funwayguy, thislooksfun, GenDeathrow and Timbucktato**, with
contributions from Anton, Mrkwtkr, TheVikingWarrior, Vexatos, LorenzoDCC and Ken
(source: https://github.com/EnviroMine/EnviroMine-1.7), used under
[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/). It is an unofficial port and
is not endorsed by the original authors. What changed is listed in [CHANGES.md](CHANGES.md).

## Licence

[CC BY-NC-SA 4.0](LICENSE), the same as the original. You may share and adapt it for
non-commercial purposes if you give credit and keep the same licence. No paid or ad-gated
downloads. See [docs/LICENSE_NOTES.md](docs/LICENSE_NOTES.md) for the licence research.

## Building and testing

Requires JDK 21.

```
./gradlew build              # jar in build/libs, runs the unit tests
./gradlew runGameTestServer  # in-world tests on a headless server, then exits
./gradlew runClient          # dev client
./gradlew runServer          # dev dedicated server
```

In game (operator, cheats on):

- `/enviromine_revived show [player]`
- `/enviromine_revived set <player> body_temp <20..50>`
- `/enviromine_revived set <player> hydration <0..100>`
- `/enviromine_revived reset <player>`
- **F3** shows an `[EnviroMine]` line with the stats the client received.

Stats are frozen in creative and spectator mode. The full manual checklist is in
[docs/TEST_CHECKLIST.md](docs/TEST_CHECKLIST.md).

## Code layout

| Package | Contents |
|---|---|
| `model` | The rules: temperature, hydration, water types, levels. Plain Java, unit tested |
| `hud` | HUD layout maths. Plain Java, unit tested |
| `world` | Reading the world into the model's inputs; water sources; data-map lookups |
| `config` | Server config and the live settings snapshot |
| `data`, `registry`, `network` | Player data, effects, damage types, item components, data maps, the drink packet |
| `client` | HUD, client config, tooltips, client-side drinking |
| `gametest` | In-world tests |

## Documentation

- [docs/BALANCE.md](docs/BALANCE.md): what the defaults mean in play
- [docs/DATAPACKS.md](docs/DATAPACKS.md): the server config and data-pack files
- [docs/TEST_CHECKLIST.md](docs/TEST_CHECKLIST.md): release test checklist
- [docs/ORIGINAL_FEATURES.md](docs/ORIGINAL_FEATURES.md): how the original systems work
- [docs/PORTING_MAP.md](docs/PORTING_MAP.md): old systems and their modern replacements
- [docs/LICENSE_NOTES.md](docs/LICENSE_NOTES.md): licence research and the author's permission
