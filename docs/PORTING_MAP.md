# Porting map: 1.7.10 Forge → 1.21.1 NeoForge

The modern APIs named here are the ones I expect to use. Each still has to be checked against the
NeoForge 21.1 docs (docs.neoforged.net) and the decompiled sources at M0 before code relies on it.

| Original (1.7.10) | Problem today | Replacement (1.21.1 NeoForge) |
|---|---|---|
| `EnviroDataTracker` + static `trackerList` keyed by player **name** | Global mutable map, name-keyed (breaks on rename), not thread-safe | A data attachment (`AttachmentType` with a codec, `copyOnDeath` decided per stat) on the player; logic in a server tick handler |
| `getEntityData()` NBT keys `ENVIRO_TMP`, `ENVIRO_HYD` | Manual save/load | Attachment serialisation handles saving |
| `PacketEnviroMine` (NBT, sent to all players within 128 blocks) | Old SimpleNetworkWrapper; leaks other players' stats | `CustomPacketPayload` + `StreamCodec`, registered in `RegisterPayloadHandlersEvent`, sent only to the owning player |
| `getSurroundingData` (11³ cube every 1.5 s, mixed with sanity/air) | Slow, everything in one 900-line method | Pure-Java `TemperatureModel` taking a plain `Environment` record; a small Minecraft adapter samples the world (smaller radius, cached) |
| `BiomeGenBase.temperature`, `rainfall`, `BiomeDictionary.Type` | Removed | `Biome#getBaseTemperature`, `Biome#getPrecipitationAt`, biome **tags** (`#c:is_swamp`, `#c:is_ocean`, ...) |
| `BiomeProperties`, `BlockProperties`, `ArmorProperties`, `ItemProperties` (Forge `.cfg` files per object) | Old config system, numeric IDs/metadata | NeoForge **data maps** (`RegisterDataMapTypesEvent`) on biomes, blocks and items, loaded from datapacks; Fire Protection via enchantment lookups |
| `DimensionProperties` | Numeric dimension IDs | Data-driven JSON keyed by `ResourceKey<Level>` (or a dimension-type data map); possibly v1.1 |
| `EM_Settings` / `EM_ConfigHandler` | Old Forge config | `ModConfigSpec`: SERVER config for balance numbers, CLIENT config for HUD |
| `EnviroPotion` with config potion IDs 27–30 | Numeric IDs gone | `MobEffect`s registered through `DeferredRegister` |
| `EnviroDamageSource` | Static damage sources gone | `DamageType` JSON in `data/<modid>/damage_type/` + `ResourceKey<DamageType>` |
| Dirty/salty/cold bottle items (`EnviroItem*WaterBottle`) | Old item registration, metadata | Items via `DeferredRegister.Items`; or one item with a data component for water type (to decide in M4) |
| `drinkWater` on `PlayerInteractEvent` (empty-hand right-click) | Event split and renamed | `PlayerInteractEvent.RightClickBlock` / `RightClickEmpty` with a block raycast; fill bottles via `UseItemOnBlockEvent` or item use hooks |
| Cauldron metadata levels | Metadata gone | `LayeredCauldronBlock.LEVEL` block state |
| `OverlayHandler`, `HudItemTemperature/Hydration` | `RenderGameOverlayEvent` gone | `RegisterGuiLayersEvent` layer drawing with `GuiGraphics` |
| `isDaytime`, world time sine | Still available | `Level#getDayTime`, `Level#isDay` |
| `canBlockSeeTheSky`, saved light | Renamed | `Level#canSeeSky`, `Level#getBrightness(LightLayer, pos)` |
| `Minecraft.getMinecraft()` calls in shared code | Crashes dedicated servers | Client code only in client-only classes, loaded via `Dist.CLIENT` |
| Achievements | Gone | Out of scope (advancements maybe later) |
