# Installing Unit Creator

## What you need

- BCU, any version.
- Java. If BCU runs, you already have it.

## Route 1: drop in and play

This is the recommended one. It does not modify BCU at all.

1. Get the `Unit Creator` folder, either from
   [Releases](https://github.com/privatemerchanttbc-bcu/bcu-unit-creator/releases)
   or by copying `download/Unit Creator` out of this repo.
2. Move that whole folder **inside** your BCU folder, next to the BCU jar:

       BCU/
         BCU-0-5-8-8.jar
         Unit Creator/
           Play BCU with Unit Creator.bat
           Install Unit Creator.bat
           Uninstall Unit Creator.bat
           unit-creator.jar

3. Run `Play BCU with Unit Creator.bat`.

BCU starts with the Unit Creator button on the main menu. Nothing on disk was changed, not
even the BCU jar's timestamp. Start BCU the normal way and you get plain BCU.

To remove it, delete the folder.

### Why this survives BCU updates

The launcher hooks the main menu in memory each time the game starts, so it does not care
which jar it is looking at. Update BCU and the launcher keeps working. There is nothing to
reinstall because nothing was ever installed.

## Route 2: install into the jar

Use this if you would rather keep one shortcut and have the button always present.

1. Same folder placement as above.
2. Close BCU.
3. Run `Install Unit Creator.bat`.
4. Start BCU however you normally do.

Re-run the installer after every BCU update, because updating replaces the jar.

### Uninstalling

Run `Uninstall Unit Creator.bat`.

It restores the original main menu class from the copy stored inside the jar and removes
only the files the installer added, tracked by name. Everything else in your jar is left
untouched, so any other tool you have installed keeps working.

Units you already built are stored in your BCU packs, not in the jar. Neither route touches
them, and uninstalling does not delete them.

## Which route

| | Launcher | Installer |
| --- | --- | --- |
| Modifies your BCU jar | no | yes, with a backup |
| Survives a BCU update | yes | no, re-run it |
| Starting BCU normally | plain BCU | button is there |
| Removing it | delete the folder | run the uninstaller |
| Works with other patches | yes | yes |

You can switch between them freely. If you somehow end up with both active at once, the
button notices it is already there and does not add itself twice.

## If you use other BCU tools

Unit Creator keeps everything it adds under `unitcreator/` and shares no files with anything
else, so it can sit alongside other patches without overwriting them.

The one thing to know about the installer route: some other installers rebuild the jar from
their own clean backup. If you re-run one of those afterwards, the Unit Creator button
disappears. Nothing is broken. Run `Install Unit Creator.bat` again, or just use the
launcher, which is immune to this.

## Troubleshooting

**"No BCU jar found"**

The `Unit Creator` folder is not inside your BCU folder. Move it next to your
`BCU-x-x-x-x.jar` and try again.

**"The BCU main menu class could not be patched"**

Your BCU build differs too much from what the installer expects. Nothing was changed. Please
open an issue and say which BCU version you are on.

**"Unit Creator is already installed"**

Run the uninstaller first, then install again. That is also how you upgrade to a newer build
of this tool. If you are using the launcher instead, just replace `unit-creator.jar`.

**Java is not recognised**

Windows cannot find Java on its own. Either install a JRE or JDK, or set `JAVA_HOME` to
point at the one you already use for BCU.

**The launcher window flashes and nothing starts**

Check `unit-creator.log` in your BCU folder. If it is not there at all, Java never started,
which is almost always the problem above.

**No button after installing**

Check `unit-creator.log` in your BCU folder. It should contain:

    *** PATCHED page/MainPage (unit creator button) ***
    UnitCreator: MainPage button added

If you see neither line, the installer did not run against the jar BCU is actually
launching. Make sure you only have one BCU jar in that folder.

## Files this creates

    unit-creator.log                     what the tool did while BCU was running

The installer route also creates:

    BCU-x-x-x-x.jar.unitcreator-backup   full backup of your jar, made before any change
    Unit Creator/install.log             what the installer did and when

If anything ever goes wrong with the installer route, deleting the modified jar and renaming
the `.unitcreator-backup` file back puts you exactly where you started.
