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

## Planned behaviour changes (will be filled in as each milestone lands)

- Fix: cold biomes no longer come out warmer than mild ones (sign error in the original's biome
  temperature formula).
- Fix: surface water is no longer almost always "dirty" (the original compared height with
  sea level / 0.75 instead of × 0.75).
- Gentler defaults for vanilla-plus play (see docs/ORIGINAL_FEATURES.md, last section).
