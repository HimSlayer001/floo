# floo

A desktop **floor plan designer** written in Java with Swing. Draw rooms on a grid canvas, add doors, windows, furniture and fixtures, then save and reload your plan.

## Features

- Grid canvas with mouse-driven room placement, moving and resizing
- Add, rotate and delete rooms
- Doors and windows attached to room walls
- Furniture library with icons (bed, chair, commode, dining set, shower, sink, sofa, stove, table, wash basin), rotatable once placed
- Fixtures
- Save a plan to a file and load it back
- "New plan" with an unsaved-changes warning

## Run it

Requires a JDK (11+).

```sh
javac FloorPlanner.java
java FloorPlanner
```

Run it from the repository root so the `furniture/` icons are found.

## Project layout

| Path | Description |
| --- | --- |
| `FloorPlanner.java` | The whole application: UI, canvas rendering, room/door/window/furniture models, save and load |
| `furniture/` | PNG icons used by the furniture palette |

## Ideas for next steps

- Split the single large source file into model, view and controller classes
- Export the plan to PNG
- Undo and redo
- Unit tests for the geometry and serialization logic
