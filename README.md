# Folded Space — HeschaPocketSpace

https://www.curseforge.com/minecraft/mc-mods/folded-space-your-pocket-dimension

A Minecraft Fabric mod that captures a bounded section of a world into an item and restores it at another location. The implementation saves blocks, block-entity data and supported non-player entities in a world-local NBT file.

## Compatibility

The current build targets **Minecraft 26.1.2**, **Java 25**, Fabric Loader **0.19.3** and Fabric API **0.151.0+26.1.2**.

## How it works

1. Place a pocket-space border block at the origin.
2. Place three more border blocks along the X, Y and Z axes from that origin.
3. Use the activation item on the origin block.
4. The selected area is captured, cleared from its original location and represented by a pocket-space item.
5. Use that item on a block face to restore the saved space beside it.

The current maximum inclusive dimensions are **24 × 24 × 24 blocks**. The saved item contains an ID referring to data in that world; it is not a self-contained cross-world export.

For testing with commands enabled:

```text
/give @s heschapocketspace:pocket_space_border_block
/give @s heschapocketspace:pocket_space_activation_item
```

Capture and placement modify the world. Test with a backed-up world, and preserve its saved pocket-space data together with the world.

## Build

Install JDK 25, then run:

```sh
./gradlew build
```

Use `gradlew.bat build` on Windows. The runtime JAR is written to `build/libs/`. Install it with the matching Fabric Loader and Fabric API.

To open a development client:

```sh
./gradlew runClient
```

## Source layout

| Class | Responsibility |
| --- | --- |
| `ActivationItem` | Border detection and capture |
| `ActivatedPocketSpaceItem` | Restore a saved space |
| `PocketSpaceStorage` | NBT storage, capture, clearing and placement |
| `PocketSpaceBounds` | Inclusive dimensions and size validation |
| `ModBlocks`, `ModItems`, `ModComponents` | Registry entries and item data |

The main package is `src/main/java/com/hescha/pocketspace/`. The client entry point is under `src/client/java/`. The repository is archived and includes a [CC0 license](LICENSE).
