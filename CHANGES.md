# Changes from the original EnviroMine

Required by CC BY-NC-SA 4.0 ("indicate if changes were made"). The original is EnviroMine for
Minecraft 1.7.10, commit `1652062` (2015-12-07) of https://github.com/EnviroMine/EnviroMine-1.7.

## Scope

- Only body temperature and hydration are ported. Air quality, sanity, gases, cave physics,
  food spoilage, earthquakes, camel packs and achievements are removed (see ROADMAP.md).

## Platform

- Rewritten for Minecraft 1.21.1 on NeoForge with Mojang mappings. No original Java code is used
  as is: the formulas are re-implemented in new code, so this is a new implementation based on the
  original's design and numbers.

## Model (M1, `enviromine_revived.model`)

The temperature and hydration rules are re-implemented as plain Java classes with unit tests.
Structure and most numbers follow the original; these are the deliberate differences.

### Bug fixes

- Cold biomes no longer come out warmer than mild ones (the original's biome formula multiplied a
  negative sine by a negative minimum).
- The hydration multiplier is applied once to activity drain (the original applied it twice).
- Shade keeps cooling in arid biomes while it rains elsewhere in the world (the original turned
  shade off whenever the world was raining, even where rain never falls).

### Behaviour changes

- Rain only cools you while you are out in it, not under a roof.
- Fire Resistance and Fire Protection act on heat above the *comfortable* ambient (25 °C).
  The original capped ambient at 37 °C, which still gave a target body temperature of 49 °C.
- Body temperature is clamped to 20–50 °C (the original had no limits).
- Cold water no longer cools a body that is already cold (the original cooled down to 30 °C).
- The hydration multiplier no longer scales how much a drink restores.

### Gentler defaults (all tunable; original value in brackets)

| Setting | Default | Original |
|---|---|---|
| Deep cave temperature | 24 °C | 30 °C (gave heatstroke deep underground) |
| High-altitude cooling starts / is full at | y 120 / y 320 | y 90 / y 256 |
| Rain cooling | 6 | 10 |
| Night cooling | 8 | 10 |
| Extra night cooling in arid biomes | 15 | 30 |
| Base hydration drain per 1.5 s | 0.04 (~62 min full to empty) | 0.05 (~50 min) |
| Dirty water poison chance | 10 % | 25 % |

## In-game logic (M2)

- Stats are a NeoForge data attachment saved with the player and synced only to that player.
  The original sent every player's stats to everyone within 128 blocks.
- Stats reset on death (like vanilla hunger) and are kept when returning from the End.
- Biome temperature is averaged over 9 points instead of 121 columns.
- Gentler block heat: torches and lanterns 30 (original torch 75, as hot as fire), furnaces,
  smokers, blast furnaces and magma 50; furnaces and campfires only count while lit. These move to
  data packs in M5.
- Netherite, chainmail, turtle and armadillo armour have no temperature effect yet.

## Still to come

- Fix: surface water is no longer almost always "dirty" (the original compared height with
  sea level / 0.75 instead of × 0.75). Lands with water types in M4.
- Gentler effects of extreme values (see docs/ORIGINAL_FEATURES.md, last section). Lands in M4.
