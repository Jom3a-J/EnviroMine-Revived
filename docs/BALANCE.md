# Balance (v1 defaults)

Goal: a small addition to vanilla survival. Temperate places never need attention; extreme places
need water, a fire or warm clothes, and give plenty of warning before anything hurts.

## Simulated hour of play

Run on 2026-11-27 with the default config and the M1 model (one update every 1.5 s, real day and
night cycles starting at sunrise). The player stands outside in clear weather and **never drinks**,
so these are worst cases. "Leather" is a full leather set (insulation 3 per piece). Times are
minutes until the first time each level is reached.

| Biome | Armour | Ambient noon / night (°C) | Body range (°C) | Warning | Effect (level 2) | Damage (level 3) |
|---|---|---|---|---|---|---|
| Plains, swamp | none | 26.5 / 19.1 | 36.5–37.2 | never | never | never |
| Forest | none | 23.5 / 16.2 | 35.1–37.0 | never | never | never |
| Jungle | none | 30.5 / 23.2 | 37.0–37.9 | never | never | never |
| Cave at y 0 | none | 24.0 / 19.1 | 35.3–37.0 | never | never | never |
| Taiga | none | 8.8 / 1.4 | 32.8–37.0 | 28 | never | never |
| Taiga | leather | 20.8 / 13.4 | 34.6–37.0 | 49 | never | never |
| Snowy plains | none | 0.0 / −7.3 | 30.9–37.0 | 18 | 49 | never |
| Snowy plains | leather | 12.0 / 4.7 | 33.5–37.0 | 34 | never | never |
| Snowy slopes (y 150) | none | −7.5 / −14.8 | 29.3–37.0 | 15 | 38 | 55 |
| Snowy slopes (y 150) | leather | 4.5 / −2.8 | 31.9–37.0 | 22 | 59 | never |
| Frozen peaks (y 200) | none | −16.7 / −24.0 | 27.4–37.0 | 12 | 30 | 42 |
| Frozen peaks (y 200) | leather | −4.7 / −12.0 | 29.9–37.0 | 16 | 41 | 59 |
| Desert, savanna | none or leather | 45.0 / 23.9 | 37.0–39.2 | 49 | never | never |
| Nether wastes | none or leather | 45.0 / 45.0 | 37.0–41.1 | 28 | 58 | never |

Hydration with no drinking at all: about 4 left after an hour in mild places, empty in deserts and
the Nether (sweating and dry air). One bottle restores 25, one sip from a river 10.

## What the numbers mean

- **Temperate biomes, caves and leather never cause problems.** Leather only insulates: it warms you
  up to comfortable (25 °C felt) and never past it, so wearing it in a desert is harmless.
- **Cold is the main danger**, and it scales with height. Snowy plains give a warning after about
  18 minutes outdoors; high peaks hurt after about 40. Leather buys 15–20 minutes; a campfire or
  torch nearby solves it.
- **Heat is held off by water.** While hydration stays at 75 or more, sweating keeps the body at
  38 °C even in a desert. Keep drinking and deserts stay safe; stop and the warning comes in under
  an hour. The Nether has no water, so bring bottles.

## Changes in the balance pass (M6)

- Leather changed from flat warmth (+2 to +3 per piece, which made temperate and desert play hot
  in full leather: body up to 41.4 °C) to **insulation 3 per piece**. Data packs can use the new
  `insulation` field on any worn item.
- Nothing else needed changing: no temperate biome reaches even a warning.

## Known limits

- Rain, wind and seasons are not modelled beyond the rain cooling and day/night cycle.
- Body temperature changes slowly by design (about 0.1 °C per minute in mild conditions), so
  conditions matter over minutes, not seconds. Standing in lava is the exception.
