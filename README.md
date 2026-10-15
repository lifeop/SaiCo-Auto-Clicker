# SaiCo Auto-Clicker

Client-side auto-clicker for Minecraft 1.8.9. Clicks only while the mouse button is held, and stops on its own when a menu is open or while you bow, eat, drink, or block.

## Requirements

- Minecraft **1.8.9**
- Minecraft Forge **11.15.1.2318** (1.8.9)
- Client only. It does not need to be installed on a server.

## Install

1. Install Forge for 1.8.9.
2. Put `saico-auto-clicker-2.0.jar` in your `.minecraft/mods` folder.
3. Launch Minecraft with the Forge profile.

Settings are saved to `.minecraft/config/saicoautoclicker.cfg`.

## Default keybinds

| Key | Action |
| --- | --- |
| `R` | Toggle the clicker on or off |
| `G` | Switch between left-click and right-click |
| `Right Shift` | Open the settings menu |

All three can be changed in the settings menu or in Minecraft's Controls screen, under **SaiCo Auto-Clicker**.

`G` does nothing while the clicker is off. Opening a menu does not turn the clicker off for good: if it was on, it pauses and turns back on when you close the menu.

## Click modes

**Left click.** Hold left mouse. The clicker swings and attacks the entity under your crosshair at the chosen rate. It does not mine blocks. CPS is capped from 1 to 10.

**Right click.** Hold right mouse while holding paper or a book.

Clicks also pause while you are drawing a bow, eating, drinking, or blocking.

## Settings menu

- Active / Inactive status
- Click rate slider (used by left click)
- Click mode
- Action bar toggle
- Rebindable keybinds, with a show/hide control for that section
- Click sound picker

The action bar shows the mode and CPS while left click is active, and the mode only while right click is active. A server action bar replaces it for a short time. You can turn the action bar off.

## Sounds

The menu plays a click when you press buttons. There are 32 built-in sounds. **Baby Click** is the default.

Mute silences those menu clicks. Picking a sound on the Sound page still plays a preview.

To add your own, use **Add** on the Sound page, or drop a `.ogg` file into:

```
.minecraft/config/saicoautoclicker
```

## Building

Java 8 is required.

```bat
gradlew.bat setupDecompWorkspace
gradlew.bat build
```

The mod jar is written to `build/libs/saico-auto-clicker-2.0.jar`.

Run a development client with:

```bat
gradlew.bat runClient
```

## Credits

Made by [Life](https://github.com/lifeop).

The menu uses [Inter](https://github.com/rsms/inter), licensed under the SIL Open Font License. See `src/main/resources/assets/saicoautoclicker/font/OFL.txt`.
