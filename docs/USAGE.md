# Using Unit Creator

Open it from the "Unit Creator" button on the BCU main menu.

## The screen

| Where | What |
| --- | --- |
| Left column | Unit 1, the base. Keeps its own skeleton, its own animation and all of its own parts. Nothing of unit 1 is ever replaced or hidden. |
| Right column | Unit 2, the parts source. Units and enemies can both be used, and you can swap unit 2 as often as you like without losing what you already attached. |
| Middle | Both units drawn side by side at one shared zoom, never overlapping. Scroll to zoom, drag to pan. |

Under the preview: Walk / Idle / Attack / Hitback, play and pause, a single frame step, and
a 60 fps / 30 fps switch. Drop to 30 fps if heavy glow effects make the preview stutter.

## Making a graft

1. Click a part in unit 1's list, or click the figure, to choose where the new part hangs.
2. Drag a part from unit 2's list onto it.

The part you dragged and everything under it comes across, with its sprites and its
keyframes, attached as a new child of the part you picked.

Part names are shown in English. Japanese and Korean names from the original data are
translated on screen only. Saved files always keep the original names.

## The sliders

| Slider | What it does |
| --- | --- |
| Move X, Move Y | where the branch sits |
| Size | how big it is |
| Rotate | how it is turned |
| Depth | in front of or behind the parts around it |
| Speed | 50% to 200% of the branch's own pace |

Every slider has a box next to it for typing an exact number, and a `[?]` you can hover for
a full explanation. The tip stays up for a minute.

The sliders follow the part you selected last, so click a grafted part to adjust it. Each
slider stores a separate value per animation, so Walk, Idle, Attack and Hitback are tuned
independently.

On the Speed track there are small marks showing the speed at which the branch would finish
exactly in step with unit 1's own Walk, Idle or Attack loop.

| Control | What it does |
| --- | --- |
| Copy animation | Turn off to take only the sprites, with no movement of their own |
| Mirror | Flip the branch, including its rotation, in place |
| Remove graft | Take this branch back off |
| Reset | Put this branch's sliders back to default |

## Saving

"Save as new Unit" opens a summary listing everything that went into the build:

| Entry | Contributes |
| --- | --- |
| Base | skeleton, animation and stats |
| Unit donor | parts and stats |
| Enemy donor | parts only, because enemies carry no unit stats |

Under the list is a tick box, **Give the new unit the combined stats of these units**.

- On: the new unit takes the best of every unit listed. Highest health, knockbacks, speed,
  cost and respawn, attack scaled up to the strongest donor, and all of their abilities,
  traits and special effects.
- Off: the new unit keeps unit 1's stats exactly and only gains the new body parts.

The box is greyed out when only enemy parts were used, since there are no unit stats to
merge.

Saving creates a new animation and a new unit in a pack called `UnitCreator`. The animation
appears in the Sprite Editor and the unit works everywhere in BCU. You can keep attaching
parts and save again for another variant.

## Update stats

"Update stats" reopens any unit you already made and rebuilds its stats.

Pick the unit on the left. The base unit and its components fill themselves in on the right,
because each save records what it was built from. Adjust the tick box and press Apply.

The stats are always rebuilt from the base unit and then merged, never added on top of what
is there now, so running it twice gives the same answer both times. Only stats change. The
animation, the icon and the name are left alone.

If a unit was made before this tool recorded origins, or a pack it used has since been
removed, the dialog says so and you pick the components by hand. It records them when you
apply, so you only do that once.

## If something looks wrong

**The branch is invisible.** It was attached to a part that is hidden in every animation.
Unit Creator moves it up to the nearest visible part and tells you when it does.

**The branch flickers or swaps sprites.** The base animation is much shorter than the
donor's, so the copied motion is compressed. Turn Copy animation off, or lower Speed.

**The preview stutters.** Switch to 30 fps. Glow layers are expensive to draw.

Everything is written to `unit-creator.log` in your BCU folder.
