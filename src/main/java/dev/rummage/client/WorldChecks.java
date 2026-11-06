package dev.rummage.client;

import java.util.*;
import dev.rummage.core.MatchRules;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

final class WorldChecks {
    record Report(Set<String> excluded,boolean accepted){}
    static Report inspect(ServerLevel level,BlockPos spawn,MatchRules rules){
        Set<String> biomes=new HashSet<>();int radius=rules.radius();
        // Noise sampling avoids generating every chunk in the search area.
        for(int x=-radius;x<=radius;x+=32)for(int z=-radius;z<=radius;z+=32){
            if((long)x*x+(long)z*z>(long)radius*radius)continue;
            for(int y:new int[]{spawn.getY(),64,-32})level.getUncachedNoiseBiome((spawn.getX()+x)>>2,y>>2,(spawn.getZ()+z)>>2).unwrapKey().ifPresent(k->biomes.add(k.identifier().getPath()));
        }
        Set<String> excluded=new HashSet<>();
        if(biomes.stream().noneMatch(b->b.contains("jungle")))excluded.add("minecraft:bamboo_block");
        if(!biomes.contains("lush_caves"))excluded.add("minecraft:moss_block");
        if(biomes.stream().noneMatch(b->b.contains("badlands")))excluded.add("minecraft:terracotta");
        if(biomes.stream().noneMatch(b->b.contains("snow")||b.contains("frozen")||b.contains("grove")))excluded.add("minecraft:snow_block");
        if(biomes.stream().noneMatch(b->b.contains("ocean")&&!b.contains("warm")&&!b.contains("frozen")))excluded.add("minecraft:dried_kelp_block");
        if(biomes.stream().noneMatch(b->b.contains("forest")||b.equals("plains")||b.equals("swamp")))excluded.add("minecraft:oak_planks");
        boolean accepted=rules.biomeFilter().isEmpty()||biomes.contains(rules.biomeFilter().replace("minecraft:",""));
        if(!rules.structureFilter().isEmpty()){
            var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(Identifier.parse(rules.structureFilter()));
            if(holder.isEmpty())accepted=false;
            else{BlockPos found=level.findNearestMapStructure(HolderSet.direct(holder.get()),spawn,Math.max(1,(radius+15)/16),false);
                accepted &= found!=null&&Math.hypot(found.getX()-spawn.getX(),found.getZ()-spawn.getZ())<=radius;}
        }
        return new Report(excluded,accepted);
    }
}
