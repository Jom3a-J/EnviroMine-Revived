# EnviroMine Revived

**Body temperature and thirst for modern Minecraft, in a small, vanilla-friendly package.**

EnviroMine Revived brings back the temperature and hydration systems of the classic 1.7.10 mod
**EnviroMine**, rebuilt from scratch for **Minecraft 1.21.1 on NeoForge** with gentler defaults.
Temperate biomes need no attention at all. Deserts, snowfields, mountain peaks and the Nether
become places you prepare for: bring water, wear warm clothes, light a fire.

---

## Features

### 🌡️ Body temperature
Your body temperature slowly follows how warm or cold your surroundings feel. That depends on:

- the **biome**, blended smoothly across borders
- the **time of day**: deserts are scorching at noon and cold at night
- **rain** (only when you are out in it) and **shade**
- **height**: deep caves are mild, high peaks are freezing
- **heat and cold nearby**: lava, fire, lit campfires and furnaces, torches, lanterns, magma, ice and snow
- **what you wear**: leather insulates you in the cold without ever overheating you; iron and gold heat up in the sun
- **water**, sprinting, sleeping in a bed, and huddling next to animals

Changes happen over minutes, not seconds, so you always have time to react.

### 💧 Hydration
Hydration drains over about an hour of play, faster when you are hot, sprinting, or in a desert by
day. Sweating keeps you cool as long as you stay well hydrated.

- **Drink** by right-clicking water or a water cauldron with an empty hand, or drink a water bottle
  for more.
- **Four kinds of water**:
  - **Clean**: most rivers and lakes.
  - **Cold**: snowy biomes and mountain springs. It cools you down a little more.
  - **Dirty**: swamps and stagnant cave water. It may give you Hunger or Poison.
  - **Salty**: oceans and beaches. It barely quenches and makes you thirsty.
- **Boil your water**: a cauldron over a campfire, fire, magma or lava always gives clean water.
- No new bottle items: a vanilla water bottle simply shows its water type in the tooltip.

### ⚠️ Clear warnings, gentle consequences
- **Level 1:** a warning on screen and a colour change on the HUD. Nothing else.
- **Level 2:** a mild effect. Heatstroke weakens you, Hypothermia slows you down, Dehydration does both.
- **Level 3:** a stronger effect plus slow damage. It follows vanilla starvation's rules: none on
  Peaceful, it stops at 5 hearts on Easy and half a heart on Normal.

Fire Resistance protects you from heatstroke.

### 🖥️ Compact HUD
One slim row above your hunger bar:
- **Temperature scale:** a marker shows your body temperature, and a faint marker shows where it is heading.
- **Hydration bar:** next to the temperature scale.
- **Options:** move it, hide it, or switch to numbers (°C or °F).

---

## How long do I have?

Standing outside without drinking, with the default settings:

| Where | First warning | Effects start |
|---|---|---|
| Plains, forest, jungle, caves | never | never |
| Snowy plains | ~18 min (~34 min in leather) | ~49 min (not within an hour in leather) |
| Frozen mountain peaks | ~12 min | ~30 min |
| Desert | ~49 min | not within an hour |
| Nether | ~28 min | ~58 min |

Drinking, a fire nearby or the right clothes push these times back, or prevent problems entirely.

---

## Configuration and data packs

- **Server config:** every balance number, plus switches to turn temperature, hydration, effects,
  damage or drinking by hand on or off. It reloads while the game is running and is sent to every
  player who joins.
- **Client config:** HUD position, offsets, number mode and units.
- **In game:** both are editable under Mods → EnviroMine Revived → Config.
- **Data packs:** set the heat of any block, the effect of any worn item, and per-biome
  temperature, water type or thirst. Tags work too, so modded content is easy to support.

---

## Requirements and compatibility

- **Minecraft 1.21.1** with **NeoForge 21.1.267 or newer**
- Install on **both the client and the server**. The server decides everything, and clients only
  display it.
- Works in singleplayer and multiplayer. Each player only receives their own stats.
- No other mods required.

---

## Not included

Only temperature and hydration are ported. The original's other systems are **not** part of this
mod:
- cave-ins and block physics
- gases and air quality
- sanity
- food spoilage
- earthquakes
- camel packs
- frostbite

---

## Source code

- **Source code, releases and bug reports:** https://github.com/Jom3a-J/EnviroMine-Revived
- **Data-pack and config guide:** https://github.com/Jom3a-J/EnviroMine-Revived/blob/main/docs/DATAPACKS.md

---

## Credits and licence

Based on **EnviroMine** for Minecraft 1.7.10 by **Funwayguy, thislooksfun, GenDeathrow and
Timbucktato**, with contributions from Anton, Mrkwtkr, TheVikingWarrior, Vexatos, LorenzoDCC and
Ken.

- **Original source:** https://github.com/EnviroMine/EnviroMine-1.7
- **Original project:** https://www.curseforge.com/minecraft/mc-mods/enviromine

The original is abandoned. Its author released the source code under CC BY-NC-SA 4.0 and invited
others to update it. This is an unofficial port and is not endorsed by the original authors.

This mod is licensed under **CC BY-NC-SA 4.0**, the same as the original
(https://creativecommons.org/licenses/by-nc-sa/4.0/). You may share and adapt it for
non-commercial purposes if you give credit and keep the same licence. Downloads are free.
