package dev.rummage.core;

/** Leader-owned match choices. Fairness and gameplay defaults are unconditional. */
public record MatchRules(boolean suddenDeath, int lookoutSeconds,
                         String biomeFilter, String structureFilter, int radius) {
    public static final MatchRules DEFAULT = new MatchRules(false,0,"","",400);
    public MatchRules {
        if(lookoutSeconds<0||lookoutSeconds>120||radius<100||radius>2000)
            throw new IllegalArgumentException("Lookout: 0–120 seconds; search radius: 100–2000 blocks.");
        if(!biomeFilter.matches("[a-z0-9_:]*")||!structureFilter.matches("[a-z0-9_:]*"))
            throw new IllegalArgumentException("Use Minecraft biome/structure IDs.");
    }
    public String encode(){return String.join(",",""+suddenDeath,""+lookoutSeconds,biomeFilter,structureFilter,""+radius);}
    public static MatchRules decode(String text){
        String[] f=text.split(",",-1);
        // Migrate pre-0.1.4 preferences, discarding the retired optional gameplay flags.
        if(f.length==10){
            for(int i:new int[]{0,1,3,4,5,6})booleanValue(f[i]);
            return new MatchRules(booleanValue(f[1]),Integer.parseInt(f[2]),f[7],f[8],Integer.parseInt(f[9]));
        }
        if(f.length!=5)throw new IllegalArgumentException("Invalid match rules");
        return new MatchRules(booleanValue(f[0]),Integer.parseInt(f[1]),f[2],f[3],Integer.parseInt(f[4]));
    }
    private static boolean booleanValue(String s){
        if(!s.equals("true")&&!s.equals("false"))throw new IllegalArgumentException("Invalid boolean rule");
        return Boolean.parseBoolean(s);
    }
}
