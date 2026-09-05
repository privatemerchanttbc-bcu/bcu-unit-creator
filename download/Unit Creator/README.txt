UNIT CREATOR for BCU
====================

Build a new unit by grafting body parts from one unit onto another.
Adds one "Unit Creator" button to the BCU main menu.


QUICK START
-----------

1. Put this "Unit Creator" folder INSIDE your BCU folder - the
   folder that contains BCU-0-5-8-8.jar or similar.
2. Run "Play BCU with Unit Creator.bat".

That is all. Your BCU jar is not modified, not even opened for
writing. Delete this folder and every trace is gone.

Use that launcher whenever you want Unit Creator. Start BCU the
normal way and you get plain BCU, unchanged.


IF YOU WANT THE BUTTON PERMANENTLY
----------------------------------

If you would rather not use a second launcher, run

    Install Unit Creator.bat

That writes Unit Creator into your BCU jar, so the button is
there no matter how you start the game.

Two things to know:

  - It modifies the jar. A full backup is made first, kept as
    BCU-0-5-8-8.jar.unitcreator-backup
  - A BCU update replaces the jar, so you have to run the
    installer again after every update. The launcher above does
    not have that problem.

"Uninstall Unit Creator.bat" puts the jar back exactly as it was.


WHICH ONE SHOULD I USE
----------------------

                        Launcher        Installer
  Modifies BCU          no              yes
  Survives BCU update   yes             no, re-run it
  Start BCU normally    plain BCU       button is there
  Removing it           delete folder   run uninstaller

The launcher is the safer choice. The installer is there for
people who want one shortcut and no thinking.


WHAT THE INSTALLER CHANGES
--------------------------

One class inside your BCU jar:

    page/MainPage.class

Two hook calls are appended to its constructor and its resize
method - one to add the button, one to place it. Everything else
this tool adds lives under unitcreator/ in the jar.

Your original copy of that class is stored in the jar as
    unitcreator/MainPage.class.orig

The launcher does the same two hook calls in memory as the game
starts, and writes nothing at all.


ANY BCU VERSION
---------------

Neither route targets one BCU build. Both read whatever
page/MainPage is in your jar and append the hook calls. If a
future BCU changes those methods beyond recognition, nothing is
damaged - the installer refuses and the launcher says so in the
log.


TROUBLESHOOTING
---------------

"No BCU jar found"
    This folder is not inside your BCU folder. Move it next to
    your BCU jar and try again.

Java is not recognised
    Install Java, or set JAVA_HOME to the one you use for BCU.

The launcher window closes and nothing happens
    Check unit-creator.log in your BCU folder.

No button after installing
    Check unit-creator.log in your BCU folder.


FULL GUIDE, SOURCE AND ISSUES
-----------------------------

    https://github.com/privatemerchanttbc-bcu/bcu-unit-creator

MIT licensed. Contains no BCU code - it only works with the copy
of BCU already on your machine.

unit-creator.jar also carries the ASM library, which is what
rewrites the main menu class. NOTICE.txt in this folder is its
copyright line, kept there because its licence asks for it.
