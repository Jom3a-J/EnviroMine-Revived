# EnviroMine Revived (working title)

A small, vanilla-plus survival addition for **Minecraft 1.21.1 (NeoForge)**: body temperature and
hydration (thirst). It is a port of the temperature and hydration systems from the abandoned
EnviroMine mod for Minecraft 1.7.10, rebuilt for modern Minecraft with gentler defaults.

> Status: early development (milestone M0, the mod loads and does nothing yet). Not released.
> The name is a placeholder.

## Planned for v1

- Body temperature pushed by biome, time of day, weather, altitude, nearby heat sources and armour.
- Hydration that drains over time; clean, cold, dirty and salty water.
- A small HUD that can be hidden or moved.
- Everything tunable through config and data packs. Server-authoritative.

Everything else from the original (cave physics, gases, sanity, …) is out of scope. See
[ROADMAP.md](ROADMAP.md).

## Credits

This mod is based on **EnviroMine** by **Funwayguy and the EnviroMine team**
(source: https://github.com/EnviroMine/EnviroMine-1.7), used under
[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/). It is an unofficial port and
is not endorsed by the original authors. What changed is listed in [CHANGES.md](CHANGES.md).

## Licence

[CC BY-NC-SA 4.0](LICENSE), the same as the original. You may share and adapt it for
non-commercial purposes if you give credit and keep the same licence. No paid or ad-gated
downloads. See [docs/LICENSE_NOTES.md](docs/LICENSE_NOTES.md) for the licence research.

## Building

Requires JDK 21.

```
./gradlew build              # jar in build/libs, runs the unit tests
./gradlew runGameTestServer  # in-world tests on a headless server, then exits
./gradlew runClient          # dev client
./gradlew runServer          # dev dedicated server
```

## How it plays

- **Drinking:** right-click water with an empty hand for a sip, or a water cauldron (lowers it by
  one level). Water bottles work too and restore more.
- **Water types:** swamp water and deep cave water are dirty (may cause Hunger or Poison), ocean
  and beach water is salty (makes you thirsty), snowy or high-mountain water is cold (cools you
  more). Boil it: a cauldron over a campfire, fire, magma or lava always gives clean water.
- **Levels:** level 1 is a warning; level 2 gives Heatstroke, Hypothermia or Dehydration; level 3
  also deals slow damage, with the same difficulty limits as starvation.

## Testing in game

- Press **F3**: the left side shows an `[EnviroMine]` line with body temperature, hydration and
  the felt ambient temperature (until the HUD exists).
- Operator command (cheats on):
  - `/enviromine_revived show [player]`
  - `/enviromine_revived set <player> body_temp <20..50>`
  - `/enviromine_revived set <player> hydration <0..100>`
  - `/enviromine_revived reset <player>`
- Stats are frozen in creative and spectator mode; use survival to see them change.

## Documentation

- [docs/DATAPACKS.md](docs/DATAPACKS.md): the server config and data-pack files (blocks, armour, biomes)

- [docs/ORIGINAL_FEATURES.md](docs/ORIGINAL_FEATURES.md): how the original systems work
- [docs/PORTING_MAP.md](docs/PORTING_MAP.md): old systems and their modern replacements
