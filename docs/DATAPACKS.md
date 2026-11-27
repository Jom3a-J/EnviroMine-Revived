# Data packs and config

Balance numbers live in the **server config**; values for particular blocks, armour and biomes
live in **data packs**. Both can be changed without touching code.

## Server config

`config/enviromine_revived-server.toml` (created on first start). It is also editable in game:
Mods → EnviroMine Revived → Config. Changes apply without a restart, and clients receive the
server's values when they join.

| Section | What it holds |
|---|---|
| `features` | Switch temperature, hydration, effects, damage and drinking by hand on or off |
| `temperature` | Every number of the body temperature model (biome scale, altitude, weather, rates, …) |
| `hydration` | Drain rates, sweating, the Thirst effect |
| `thresholds` | Body temperatures and hydration levels where warnings, effects and damage start |
| `water.clean/cold/dirty/salty` | What each kind of water does (hydration, cooling, sickness chances) |

The client config, `config/enviromine_revived-client.toml`, only holds HUD settings.

## Data maps

NeoForge data maps. A file in your data pack at
`data/enviromine_revived/data_maps/<registry>/<name>.json` (the folder is named after the map,
not after your pack) adds to or overrides the mod's defaults. Entries can be ids or tags
(`"#minecraft:candles"`). A pack loaded later wins. Run `/reload` to apply changes.

The mod's own defaults are in the mod jar under `data/enviromine_revived/data_maps/`.

### Block temperature: `data_maps/block/block_temperature.json`

```json
{
  "values": {
    "minecraft:torch": { "temperature": 30 },
    "minecraft:campfire": { "temperature": 75 },
    "minecraft:blue_ice": { "temperature": -0.2 },
    "#minecraft:candles": { "temperature": 10 }
  }
}
```

- `temperature`: heat (positive) or cold (negative). Heat sources: the hottest one in range counts,
  falling off with distance over 5 blocks, and meets the surroundings half-way. Cold blocks: all of
  them in range add up, so keep cold values small.
- `only_when_lit` (default `true`): for blocks with a lit state (furnaces, campfires, candles),
  only count while lit.

To remove an entry from the defaults:

```json
{ "remove": ["minecraft:torch"] }
```

### Armour: `data_maps/item/armor_temperature.json`

```json
{
  "values": {
    "minecraft:leather_chestplate": { "insulation": 3 },
    "minecraft:iron_helmet": { "sun_add": 2, "night_add": -1, "sun_mult": 1.1 },
    "minecraft:carved_pumpkin": { "shade_add": 1, "night_add": 1 }
  }
}
```

Applies to anything worn in an armour slot. For each piece, depending on whether you are in the
sun (day, open sky, above 0 °C), in the shade (day, otherwise) or out at night:
`*_add` is added to the felt temperature, and `*_mult` multiplies it (all pieces' multipliers are
combined as `1 + sum(mult - 1)`). `insulation` warms you only while you are colder than
comfortable (25 °C felt), and never past it, so it suits clothing. Leave fields out for no effect.

Defaults: leather insulates (3 per piece), iron and gold heat up in the sun, diamond reflects a
little. Chainmail, netherite and turtle shells have no effect.

### Biomes: `data_maps/worldgen/biome/biome_climate.json`

```json
{
  "values": {
    "minecraft:mangrove_swamp": { "extra_drain": 0.02 },
    "#c:is_mushroom": { "ambient": 18, "water": "clean" },
    "mymod:volcanic_wastes": { "ambient": 40, "water": "dirty", "extra_drain": 0.05 }
  }
}
```

- `ambient`: sets the biome's temperature in °C directly, instead of deriving it from the
  biome's vanilla temperature value (useful for modded biomes).
- `water`: `clean`, `cold`, `dirty` or `salty` for all water in the biome (a heated cauldron still
  boils it clean).
- `extra_drain`: extra hydration lost every 1.5 s while in the biome.

The mod ships no biome entries: vanilla biome temperatures and the biome tags `c:is_swamp`,
`minecraft:is_ocean`, `minecraft:is_beach`, `c:is_snowy` and `c:is_icy` are used instead.
