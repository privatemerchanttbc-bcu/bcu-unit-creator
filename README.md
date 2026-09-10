# Unit Creator for BCU

Build a new unit by grafting body parts from one unit onto another.

Pick a base unit, pick a parts source, drag any part from the source onto any part of the
base, and the whole branch comes across with its animation. Save produces a real new unit
with a real new animation that shows up in the Sprite Editor and works everywhere in BCU.

Adds one "Unit Creator" button to the BCU main menu.

## What it does

- Drag any part from unit 2 onto any part of unit 1. The whole branch comes with it, with
  its animation. Unit 1 keeps every part it already had, nothing is replaced or hidden.
- Japanese and Korean part names are shown in English, with an EN / JP toggle. Saved files
  keep the original names.
- On save you choose whether the new unit takes the combined stats of every unit that went
  into it. That list is stored, so you can rebuild the stats later.

## Install

There are two ways in. The first changes nothing about your BCU.

### 1. Drop in and play (recommended)

1. Download the `Unit Creator` folder from
   [Releases](https://github.com/privatemerchanttbc-bcu/bcu-unit-creator/releases),
   or take `download/Unit Creator` from this repo.
2. Put that folder inside your BCU folder, next to `BCU-x-x-x-x.jar`.
3. Run `Play BCU with Unit Creator.bat` instead of starting BCU the usual way.

Your BCU jar is never touched. Unit Creator is attached while the game is running and is
gone the moment you launch BCU normally again. Deleting the folder removes it completely,
and a BCU update cannot break it, because nothing was installed in the first place.

### Running alongside other BCU patches

Some patches share a launcher called `BCU New Features.bat`, which starts BCU with every
registered feature at once. Run `Add to BCU New Features.bat` to join it, and use that
launcher from then on. The jar still is not modified, and
`Remove from BCU New Features.bat` takes Unit Creator back out without touching anything
else that is registered.

### 2. Install into the jar (fallback)

If the launcher does not suit you, or you would rather keep using your own shortcut, run
`Install Unit Creator.bat` in the same folder. That writes Unit Creator into your BCU jar,
so the button is there however you start the game.

The trade-off: it modifies the jar, and a BCU update replaces the jar, so you have to run
the installer again after each update. A full backup is made before anything is written,
and `Uninstall Unit Creator.bat` puts everything back.

See [docs/INSTALL.md](docs/INSTALL.md) for the detailed version and troubleshooting, and
[docs/USAGE.md](docs/USAGE.md) for how to use the tool.

## Any BCU version

Neither route targets a specific BCU build. Both find `page/MainPage`, append two hook calls
to its constructor and its resize method, and go from there. The launcher does that in
memory as the game starts; the installer does it once, on disk. If a future BCU changes
those methods beyond recognition, the launcher says so in the log and the installer stops
with a clear message and changes nothing.

## What the installer changes, exactly

The launcher changes nothing on disk at all. The installer touches one class inside your
BCU jar:

    page/MainPage.class

Your original copy of it is stored in the jar as `unitcreator/MainPage.class.orig`, and a
full backup of the jar is written next to it as `BCU-x-x-x-x.jar.unitcreator-backup`.

Everything this tool adds lives under `unitcreator/` in the jar. The installer records the
exact list of files it wrote, so `Uninstall Unit Creator.bat` removes those and nothing
else. Units you already made live in your BCU packs, not in the jar, so they survive
uninstalling.

## Build from source

You need JDK 17 or newer and a copy of the BCU jar.

    git clone https://github.com/privatemerchanttbc-bcu/bcu-unit-creator
    cd bcu-unit-creator
    build\build.bat "C:\path\to\BCU-0-5-8-8.jar"

The result lands in `download\Unit Creator\unit-creator.jar`. That single jar is both
the launcher agent and the installer. The BCU jar is used
only to compile against; nothing from it ends up in the output.

## Author

Private Merchant - [YouTube](https://www.youtube.com/channel/UCDyvXQBtpDc3eLassyvxxQw)

Bug reports and ideas are welcome in
[Issues](https://github.com/privatemerchanttbc-bcu/bcu-unit-creator/issues).
