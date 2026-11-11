package dev.rummage.core;

/** OpenDraft palette: cyan locally, stable join-assigned colours for other players. */
public final class PlayerColors {
    public static final int SELF=0xFF00BCD4;
    private static final int[] OTHERS={0xFFE53935,0xFFFDD835,0xFF43A047,0xFFD81B60,0xFFFB8C00,0xFF8E24AA,0xFF1E88E5,0xFFEEEEEE};
    public static int color(int index,boolean self){return self?SELF:OTHERS[Math.floorMod(index,OTHERS.length)];}
    private PlayerColors(){}
}
