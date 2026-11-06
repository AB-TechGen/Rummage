package dev.rummage.core;

/** Lobby-owned rules, validated before any state is changed. */
public record MatchRules(boolean biomeTargets, boolean suddenDeath, int lookoutSeconds,
                         boolean standardDrops, boolean race, boolean fullbright, boolean fairPlay,
                         String biomeFilter, String structureFilter, int radius) {
    public static final MatchRules DEFAULT = new MatchRules(true,false,0,true,false,true,true,"","",400);
    public MatchRules {
        if (lookoutSeconds < 0 || lookoutSeconds > 120 || radius < 100 || radius > 2000)
            throw new IllegalArgumentException("Lookout: 0–120 seconds; search radius: 100–2000 blocks.");
        if (!biomeFilter.matches("[a-z0-9_:]*") || !structureFilter.matches("[a-z0-9_:]*"))
            throw new IllegalArgumentException("Use Minecraft biome/structure IDs.");
    }
    public String encode() { return String.join(",", ""+biomeTargets,""+suddenDeath,""+lookoutSeconds,""+standardDrops,""+race,""+fullbright,""+fairPlay,biomeFilter,structureFilter,""+radius); }
    public static MatchRules decode(String text) {
        String[] f=text.split(",",-1);
        if(f.length!=10)throw new IllegalArgumentException("Invalid match rules");
        for(int i:new int[]{0,1,3,4,5,6})if(!f[i].equals("true")&&!f[i].equals("false"))throw new IllegalArgumentException("Invalid boolean rule");
        return new MatchRules(Boolean.parseBoolean(f[0]),Boolean.parseBoolean(f[1]),Integer.parseInt(f[2]),Boolean.parseBoolean(f[3]),Boolean.parseBoolean(f[4]),Boolean.parseBoolean(f[5]),Boolean.parseBoolean(f[6]),f[7],f[8],Integer.parseInt(f[9]));
    }
}
