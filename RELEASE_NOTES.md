Hotfix: Blooming Lily Pad items showed the missing (purple and black) texture.

- In 1.2.0 the item drew the lily pad from the block texture atlas and the flower from the item atlas. Minecraft 26.3 refuses item models that mix two atlases, so all four Blooming Lily Pad items lost their texture. The flower of the item is now in the block atlas too, like the vanilla lily pad.
- Blocks, worlds and configs are not affected. No new blocks: 1.2.0 and 1.2.1 can play together, but the fix only shows on clients with 1.2.1.
