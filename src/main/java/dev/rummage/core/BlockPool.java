package dev.rummage.core;

import java.util.*;

/** Deterministic, progression-gated targets with tier weights and match-history repeat protection. */
public final class BlockPool {
    private static final List<List<String>> TIERS=List.of(
        List.of("dirt","grass_block","stone","cobblestone","gravel","sand","oak_planks","crafting_table","furnace","polished_andesite","polished_diorite","polished_granite"),
        List.of("glass","stone_bricks","smooth_stone","bricks","white_wool","mossy_cobblestone","sandstone","dried_kelp_block","bamboo_block","moss_block","terracotta","snow_block"),
        List.of("bookshelf","iron_block","coal_block","copper_block","gold_block","lapis_block","redstone_block","obsidian","netherrack"),
        List.of("nether_bricks","quartz_block","glowstone","amethyst_block","diamond_block")
    );
    private static final int[][] WEIGHTS={{100},{25,75},{15,50,35},{10,35,40,15}};
    /** Both conditions must be met; early completions cannot rush resource/Nether targets. */
    public static int stage(int round,long activeMillis){
        if(round>=16&&activeMillis>=30*60_000L)return 3;
        if(round>=9&&activeMillis>=15*60_000L)return 2;
        if(round>=4&&activeMillis>=5*60_000L)return 1;
        return 0;
    }
    public static String draw(long seed,int round,String previous){return draw(seed,round,previous,Set.of());}
    /** Convenience for callers without a match clock: assumes full default-length preceding rounds. */
    public static String draw(long seed,int round,String previous,Set<String> excluded){
        return draw(seed,round,previous,excluded,List.of(),Math.max(0,round-1)*300_000L);
    }
    public static String draw(long seed,int round,String previous,Set<String> excluded,List<String> history,long activeMillis){
        int stage=stage(round,activeMillis);
        Set<String> eligible=new LinkedHashSet<>();
        for(int tier=0;tier<=stage;tier++)for(String block:TIERS.get(tier)){
            String id="minecraft:"+block;if(!id.equals(previous)&&!excluded.contains(id))eligible.add(id);
        }
        if(eligible.isEmpty())throw new IllegalArgumentException("No eligible targets");
        Set<String> unused=new LinkedHashSet<>(eligible);unused.removeAll(history);
        if(!unused.isEmpty())eligible=unused;
        else{
            Set<String> recent=new HashSet<>(history.subList(Math.max(0,history.size()-3),history.size()));
            Set<String> fresh=new LinkedHashSet<>(eligible);fresh.removeAll(recent);if(!fresh.isEmpty())eligible=fresh;
        }
        var choices=new ArrayList<List<String>>();int total=0;
        for(int tier=0;tier<=stage;tier++){
            List<String> pool=new ArrayList<>();for(String block:TIERS.get(tier)){String id="minecraft:"+block;if(eligible.contains(id))pool.add(id);}
            choices.add(pool);if(!pool.isEmpty())total+=WEIGHTS[stage][tier];
        }
        Random random=new Random(seed^(0x9E3779B97F4A7C15L*round));int pick=random.nextInt(total);
        for(int tier=0;tier<=stage;tier++)if(!choices.get(tier).isEmpty()){
            pick-=WEIGHTS[stage][tier];if(pick<0){var pool=choices.get(tier);return pool.get(random.nextInt(pool.size()));}
        }
        throw new IllegalStateException("Target selection failed");
    }
    private BlockPool(){}
}
