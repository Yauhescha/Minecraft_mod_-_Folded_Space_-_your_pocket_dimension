# Pocket Space capture/restore implementation

Implemented flow:

1. Use `pocket_space_activation_item` only on the origin `pocket_space_border_block`.
2. The activation item finds the farthest `pocket_space_border_block` on X, Y, and Z axes from the clicked origin.
3. The cuboid between those 4 markers is serialized into compressed NBT under the world folder:

```text
<world>/data/heschapocketspace/pocket_spaces/<uuid>.nbt
```

4. The player receives `pocket_space_item` with `heschapocketspace:pocket_space_id` data component.
5. The original blocks, block entities, dropped items, animals and other non-player entities in the cuboid are removed.
6. Right-clicking `pocket_space_item` on a block restores the stored data at the clicked side position and deletes the `.nbt` file.

Notes:

- Maximum size is controlled by `PocketSpaceStorage.MAX_SIZE` and is currently `24`.
- Target blocks and non-player entities in the target cuboid are replaced/removed.
- Players are intentionally not captured or removed.
- Vanilla `BlockEntity` NBT is saved/restored. Some modded block entities or entities may still need extra handling if they store state outside normal NBT.
- The item can only be used once in survival because the storage file is deleted after successful restore.
