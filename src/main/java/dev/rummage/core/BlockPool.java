package dev.rummage.core;

import java.util.*;

/** Curated survival-obtainable full blocks; progression avoids rare early objectives. */
public final class BlockPool {
    private static final List<String> EASY = List.of("dirt", "grass_block", "stone", "cobblestone", "gravel", "sand", "oak_planks", "crafting_table", "furnace", "cobblestone", "polished_andesite", "polished_diorite", "polished_granite");
    private static final List<String> MEDIUM = List.of("glass", "stone_bricks", "smooth_stone", "bricks", "bookshelf", "white_wool", "iron_block", "coal_block", "copper_block", "mossy_cobblestone", "sandstone", "dried_kelp_block", "bamboo_block", "moss_block", "terracotta", "snow_block");
    private static final List<String> HARD = List.of("gold_block", "lapis_block", "redstone_block", "obsidian", "netherrack", "nether_bricks", "quartz_block", "glowstone", "amethyst_block", "diamond_block");
    public static String draw(long seed, int round, String previous) {
        return draw(seed, round, previous, Set.of());
    }
    public static String draw(long seed, int round, String previous, Set<String> excluded) {
        List<String> pool = new ArrayList<>(new LinkedHashSet<>(EASY));
        if (round >= 4) pool.addAll(MEDIUM);
        if (round >= 9) pool.addAll(HARD);
        pool.remove(previous.replace("minecraft:", ""));
        pool.removeIf(b -> excluded.contains("minecraft:"+b));
        if(pool.isEmpty())throw new IllegalArgumentException("No eligible targets");
        return "minecraft:" + pool.get(new Random(seed ^ (0x9E3779B97F4A7C15L * round)).nextInt(pool.size()));
    }
    private BlockPool() {}
}
