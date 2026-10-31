# All Items

Collect every item in Minecraft, one at a time, in random order. Client-side Fabric mod for singleplayer.

**Download:** [Modrinth](https://modrinth.com/mod/all-items-by-opniki)

## How it works

The top of the screen only shows the item you're currently looking for. As soon as it's in your inventory, off-hand or an armour slot, it's ticked off and the next one appears.

The item list is read from the game's registry when a challenge starts. Nothing is hardcoded, so the list always matches the installed Minecraft version. Potions, tipped arrows and enchanted books are split into one entry per variant. The order is shuffled with a seed, so the same seed always gives the same order.

## Features

- Random order, reproducible via seed
- Automatic detection in inventory, off-hand and armour slots
- Progress, item icon and name at the top of the screen
- Warning for items you can't get in survival (command blocks, spawn eggs, barriers …), which you can skip
- Scrollable overview of done, skipped and remaining items. Remaining items are sorted alphabetically, so the upcoming order stays hidden
- Chat message and sound for every find, plus a celebration at the end
- Adjustable colour and background
- Progress saved per world

## Commands

| Command | Effect |
| --- | --- |
| `/challenge start [seed]` | Start a new challenge |
| `/challenge list` | Open the overview |
| `/challenge skip` | Skip the current item |
| `/challenge current` | Show the current item in chat |
| `/challenge pause` / `resume` | Pause or continue detection |
| `/challenge hide` / `show` | Hide or show the display |
| `/challenge bg on` / `off` | Toggle the background |
| `/challenge color <hex>` | Change the colour, e.g. `FF55FF` |
| `/challenge dump` | Write the full item list to `config/item-challenge/items.txt` |
| `/challenge reset` | Clear all progress |

## Project structure

Each supported Minecraft version has its own folder:

```
26.2/   Fabric project for Minecraft 26.2
26.3/   Fabric project for Minecraft 26.3
```

Main classes in `src/main/java/de/niklas/itemchallenge/`:

- `ChallengeState` builds the item list, tracks progress and saves it per world
- `Variants` splits items like potions and enchanted books into their variants
- `Unobtainable` lists items that can't be obtained in survival
- `ChallengeHud` and `OverviewScreen` handle everything on screen
- `ChallengeCommands` registers all `/challenge` commands

## Build

Requires the JDK version used by the target Minecraft version.

```
cd 26.3
./gradlew build
```

The finished `.jar` is in `build/libs/`.

Works well together with [Challenge Timer](https://github.com/OPNiki/challenge-timer) if you want to track your run time.

## Author

Made by [OPNiki](https://github.com/OPNiki) · [Portfolio](https://opniki.github.io)
