# Target selection in 1.0.0

## What 0.2.0 did

Rounds 1–3 drew uniformly from 12 unique starter blocks. Round 4 added 16 medium blocks (28 total); round 9 added 10 hard blocks (38 total). It excluded only the previous target and biome-filtered targets. The duplicated cobblestone entry was deduplicated, so it had no extra weight.

This allowed fast completions to unlock expensive targets before players had spent meaningful time gathering resources. It also allowed dirt → furnace → dirt and kept roughly a third of late draws in the starter pool.

## Current tiers

A tier opens only when **both** its round gate and accumulated active-play gate are met. The synchronizer owns the clock, history and selection. Every client gets the same target.

| Tier | Gate | Blocks |
| --- | --- | --- |
| Starter (12) | Round 1 | dirt, grass_block, stone, cobblestone, gravel, sand, oak_planks, crafting_table, furnace, polished_andesite, polished_diorite, polished_granite |
| Crafted (12) | Round 4 and 5 active minutes | glass, stone_bricks, smooth_stone, bricks, white_wool, mossy_cobblestone, sandstone, dried_kelp_block, bamboo_block, moss_block, terracotta, snow_block |
| Resources / Nether access (9) | Round 9 and 15 active minutes | bookshelf, iron_block, coal_block, copper_block, gold_block, lapis_block, redstone_block, obsidian, netherrack |
| Rare / further processing (5) | Round 16 and 30 active minutes | nether_bricks, quartz_block, glowstone, amethyst_block, diamond_block |

Active-play time sums actual round durations, ending at early completion or the deadline. Loading, lookout, intermission and the deadline settlement grace are excluded. Menus and brief disconnections do not stop an active round's timer. The active-time gates still apply in short-round practice and sudden death; rushing round numbers alone does not unlock harder tiers.

## Drawing a target

1. Collect the unlocked tiers and apply the existing biome exclusions.
2. Remove the immediately previous target.
3. If any eligible target has not appeared in this match, choose only among those unused targets.
4. Otherwise, avoid the last three targets if alternatives remain.
5. Pick a nonempty tier using the weights below, then pick uniformly within that tier. Empty tiers are skipped and the remaining weights are renormalized.

| Highest unlocked tier | Starter | Crafted | Resources | Rare |
| --- | --- | --- | --- | --- |
| Starter | 100 | — | — | — |
| Crafted | 25 | 75 | — | — |
| Resources | 15 | 50 | 35 | — |
| Rare | 10 | 35 | 40 | 15 |

These are tier weights, not guaranteed percentages of an entire match: unused-target priority and biome exclusions change which tiers have candidates. Draws remain deterministic from seed, round, active time, exclusions and target history. A new match clears the clock and history.

Biome sampling is still a nearby-biome heuristic. It cannot guarantee a particular block exists within reach or account for every alternative crafting route.
