# Release test checklist

Run before every release. Automated tests first, then the manual checks.

## Automated

- [ ] `./gradlew build`: compiles and all unit tests pass
- [ ] `./gradlew runGameTestServer`: "All N required tests passed"

## Singleplayer (`./gradlew runClient`, new survival world, cheats on)

Basics
- [ ] Mod list shows EnviroMine Revived with credits to the original authors
- [ ] HUD row above the hunger bar; air bubbles stack above it underwater
- [ ] F3 shows the `[EnviroMine]` line; `/enviromine_revived show` matches it
- [ ] Stats survive leaving and rejoining the world; they reset on death

Temperature
- [ ] Standing next to lava or a lit campfire raises the ambient; an unlit campfire does not
- [ ] `/enviromine_revived set @s body_temp 41.5`: action-bar warning, Heatstroke icon
- [ ] `... body_temp 44`: slow damage on Normal; none on Peaceful; stops at 5 hearts on Easy
- [ ] Fire Resistance removes Heatstroke
- [ ] `... body_temp 31`: Hypothermia icon, slower walking and mining
- [ ] Full leather in a snowy biome: ambient higher than without; in a desert: no change

Hydration and water
- [ ] Empty-hand right-click on a river: drink sound, +10; nothing when full
- [ ] Water cauldron with empty hand: drink, cauldron level drops
- [ ] Bottles: ocean "Salt water", swamp "Dirty water", snowy "Cold water", river has no extra line
- [ ] A cauldron over a lit campfire gives a clean bottle, even in a swamp
- [ ] Salt water gives Thirst; dirty water sometimes Hunger or Poison
- [ ] `... hydration 5`: Dehydration icon; `... hydration 0`: slow damage

Config
- [ ] Mods → EnviroMine Revived → Config: client and server sections, readable names and tooltips
- [ ] Client: anchor, offsets, number mode, °F, show in creative all change the HUD
- [ ] Server: `features.hydration = false` hides the droplet half and disables drinking, live
- [ ] Server: an out-of-order threshold logs a warning and the defaults are used
- [ ] A data pack overriding `minecraft:torch` heat takes effect after `/reload`

## Dedicated server (`./gradlew runServer`, then connect the dev client to `localhost`)

- [ ] Server starts without errors (first start needs `eula=true` in `run/eula.txt`)
- [ ] No client-only classes load on the server (no `NoClassDefFoundError` in the log)
- [ ] Joining client gets the HUD and the server's config values
- [ ] Two players: each only receives their own stats
- [ ] Drinking, bottles, effects and damage behave as in singleplayer
- [ ] Editing the server config while running applies to connected players
- [ ] Stats are kept after a server restart

## Before publishing

- [ ] Licence confirmed by the original authors (see docs/LICENSE_NOTES.md)
- [ ] Final mod name and id chosen (still the placeholder `enviromine_revived`)
- [ ] Credits checked against the original project's member list
- [ ] Version number set; CHANGES.md up to date
