# Original EnviroMine (1.7.10): temperature and hydration

Source: EnviroMine-1.7 at commit `1652062` (2015-12-07). Only the v1-scope systems are described.
File references are relative to `src/main/java/enviromine/`.

## Update loop (`trackers/EnviroDataTracker.java`, `handlers/EM_StatusManager.java`)

- Each tracked entity has `bodyTemp` (start 37 °C), `hydration` (0–100, start 100) and a derived
  `airTemp` (ambient). Players and some animals are tracked.
- `updateData()` runs every **30 ticks** (1.5 s). It calls `getSurroundingData(entity, 5)`, which
  scans a **11×11×11** block cube around the entity, plus the hotbar, armour and nearby mobs.
- Side effects (potions, damage) only apply every `attackDelay` updates (default 1).
- Creative mode freezes all values. Values are rounded (2–3 decimals) before syncing.
- Data is stored in the entity's custom NBT (`ENVIRO_TMP`, `ENVIRO_HYD`) and synced to all clients
  within 128 blocks via an NBT packet.

## Ambient temperature (`getSurroundingData`)

1. **Biome**: average over the 11×11 horizontal ring of
   `getBiomeTemp(t) = t ≥ 0 ? sin(t·45°)·45 : sin(t·45°)·(−15)` where `t` is the vanilla biome
   temperature at that position. So vanilla 2.0 (desert) → 45 °C, 0.8 (plains) → ~29.4 °C.
   *Bug to flag*: for negative `t` this returns a **positive** value (sin of a negative angle times
   −15), so the coldest biomes come out warmer than `t = 0`. The port should not copy this.
2. **Altitude**: below y=48 the temperature is pulled towards 30 °C ("geothermal") linearly with
   depth. Between y=90 and 256 it is pulled towards −30 °C linearly. At y ≥ 256 it is capped at −30.
3. **Cold blocks**: block temperatures below 0 subtract (with distance falloff) as `cooling`.
4. **Sleeping**: +10.
5. **Rain** (if biome has rainfall): −10, and cooling rate rises to 0.01 when under open sky.
6. **Shade** (daytime, no sky access, not raining): −2.5.
7. **Day/night**: `dayPercent = clamp((sin(time·360° − 30°)·0.5 + 0.5)·1.25, 0, 1)`; subtract
   `(1 − dayPercent)·10`, and another `(1 − dayPercent)·30` in biomes with zero rainfall (deserts
   get very cold at night).
8. **Nearby mobs** (box of ~5 blocks): if the average mob body temperature − 12 is warmer than the
   ambient, ambient becomes the mean of the two (huddling warmth).
9. **Armour**: per piece, depending on day+sky / shade / night, a multiplier and an additive offset.
   Total: `ambient *= 1 + Σ(mult − 1)`, then `ambient += Σ offset`. Defaults:

   | Material | night add | shade add | sun add | sun mult |
   |---|---|---|---|---|
   | leather | +1 | +1 | +1 | 1.0 |
   | iron | −1 | 0 | +2 | 1.1 |
   | gold | 0 | 0 | +2.5 | 1.2 |
   | diamond | 0 | 0 | 0 | 0.9 |
   | chain, other | 0 | 0 | 0 | 1.0 |

   Fire Protection levels across all pieces scale heat above 37 by `1 − levels/18`.
10. **In water**: if ambient > 50, −50; else if > 25, set to 25. Cooling rate 0.01.
11. **Heat sources**: the hottest block in range with falloff
    `f(T, d) = T − T·d²/max²`, `max = √3·range` (≈ 8.66), becomes `temp`; if `temp` > ambient the
    result is the **average** of the two. Defaults: lava 200, fire 75, torch 75, lit furnace 75,
    netherrack/nether bricks 50, snow/ice small negatives (cooling). Hotbar items can also act as
    heat sources.
12. **Standing in lava**: 200 with instant rise. **Burning**: at least 75, rise rate ≥ 0.1.
13. **Sprinting**: +2, rise rate ≥ 0.01, extra dehydration 0.05.
14. **Fire Resistance**: ambient capped at 37.
15. Biome overrides from config (`BiomeProperties`) can replace the ambient temperature and add
    temperature and dehydration rates; dimension overrides multiply rates.

## Body temperature drift (`EnviroDataTracker.updateData`)

- Target ("relTemp") = **ambient + 12**. So 25 °C ambient is comfortable (37 °C body).
- Per update, body temperature moves towards the target by `rate × amp`, where `amp = |diff|/10`
  when the gap exceeds 10, else 1. Base rates: falling 0.001, rising 0.001, raised by rain/water
  (0.01), lava nearby or strong heat source (0.005), burning (0.1), lava (1.0). All scaled by
  config `tempMult`.
- At body ≥ 38.02 hydration drops 0.1 extra per update, and if hydration ≥ 75 the body cools by 0.01
  (sweating).

## Hydration

- Base drain **0.05 per update** (every 1.5 s) → 100 → 0 in about 50 minutes of play if nothing
  else applies.
- Extra drain (`dehydrateBonus`): near lava +0.05, arid biome by day +0.05, sprinting +0.05,
  Hunger effect +0.1, biome/entity overrides. All scaled by config `hydrationMult`.
- At hydration 0: **4 damage (2 hearts) every update** (very harsh).
- Drinking (`handlers/EM_EventManager.drinkWater`): right-click a water block or a filled cauldron
  with an empty hand. Effects by water type:

  | Type | Hydration | Body temp | Extra |
  |---|---|---|---|
  | 0 clean | +10 | −0.05 if ≥ 37.05 | |
  | 1 dirty | +10 | −0.05 | 50 % Hunger 10 s, 25 % Poison 10 s |
  | 2 salty | +5 | −0.05 | Dehydration effect 30 s, may stack amplifier |
  | 3 cold | +10 | −0.1 if ≥ 30.1 | |

  A cauldron above fire or lava always gives clean (boiled) water.
- Water type (`getWaterType`): biome config override first; otherwise dirty if swamp, jungle,
  dead, wasteland, water colour looks off, **or y < seaLevel/0.75 (≈ 85)**; salty if ocean or
  beach; cold if snowy, coniferous, frozen, or y > 2·seaLevel; else clean.
  *Bug to flag*: `seaLvl/0.75` (≈ 85) makes almost all surface water dirty. `seaLvl·0.75` (48) was
  probably intended.
- Filling a glass bottle gives the matching bottle item (dirty, salty, cold, or vanilla water).
- Camel pack (chest armour with NBT fill) auto-drinks. Out of scope for v1.

## Effects of extremes

Applied every update (potions last 10 s, so they stay while the condition lasts):

| Condition | Effect |
|---|---|
| body ≥ 39 | Heatstroke I |
| body ≥ 41 | Heatstroke II: Weakness, Mining Fatigue, Hunger, 10 % chance Nausea |
| body ≥ 43 | Heatstroke III: also 50 % chance of 4 damage each update |
| body ≥ 45 near lava, or ≥ 50 | set on fire 10 s |
| body ≤ 35 | Hypothermia I |
| body ≤ 32 | Hypothermia II: Weakness, Mining Fatigue |
| body ≤ 30 | Hypothermia III: also 50 % chance of 4 damage |
| cold for 120 updates (3 min) | Frostbite I: random dropping of held item (5 % per update) |
| cold for 240 updates | Frostbite II: damage |
| cold for 360 updates | frostbite becomes **permanent** |
| hydration 0 | 4 damage each update |
| Dehydration effect | −(1 + amplifier) hydration per update |

Fire Resistance removes heatstroke. Config `noNausea` swaps nausea for blindness.

## Config options in scope

`enableBodyTemp`, `enableHydrate`, `enableHeatstrokeGlobal`, `enableHypothermiaGlobal`,
`enableFrostbiteGlobal`, `tempMult`, `hydrationMult`, `noNausea`, `finiteWater`, HUD bar positions,
potion IDs (obsolete), per-biome/block/item/armour/entity/dimension property files.

## Items and blocks in scope

Dirty, salty and cold water bottles; custom effects Heatstroke, Hypothermia, Frostbite,
Dehydration; HUD items `HudItemTemperature`, `HudItemHydration`.

## Things that would feel out of place in vanilla (proposed gentler defaults)

- 4 damage every 1.5 s at zero hydration → drain to a hunger-like floor: Slowness/Weakness first,
  then 1 damage every ~4 s, never below half a heart on Easy (mirroring vanilla starvation).
- Permanent frostbite and random item dropping → drop entirely in v1.
- Most surface water dirty (the y < 85 bug) → clean by default; dirty only in swamps or with a
  datapack tag.
- Desert nights −40 °C relative swing → keep a swing, but smaller by default.
- Random fire at 45–50 °C body temp → only via standing in lava/fire, as vanilla already does.
- Nausea → off by default (accessibility).
