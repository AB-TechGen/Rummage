package dev.rummage.net;

import dev.rummage.core.Match;
import java.util.*;

public record Snapshot(String phase, String leader, String matchId, long seed, int seconds, int maxRounds,
                       int round, String block, long remaining, String result, List<Player> players, long receivedAt,
                       dev.rummage.core.MatchRules rules, Set<String> drawVotes, long drawRemaining) {
    public record Player(String id, String name, boolean connected, boolean ready, boolean alive, boolean complete, int score, String dimension, int ping) {}
    public static String[] encode(Match m, long now) {
        List<String> f = new ArrayList<>(List.of("STATE", m.phase.name(), m.leader, m.matchId, ""+m.seed, ""+m.roundSeconds, ""+m.maxRounds, ""+m.round, m.target, ""+Math.max(0, m.deadline-now), m.result));
        f.add(m.rules.encode()); f.add(String.join(",",m.drawVotes)); f.add(""+Math.max(0,m.drawDeadline-now));
        for (Match.Player p : m.players.values()) f.add(Wire.pack(p.id, p.name, ""+p.connected, ""+p.ready, ""+p.alive, ""+p.complete, ""+p.score,p.dimension,""+p.ping));
        return f.toArray(String[]::new);
    }
    public static Snapshot decode(String[] f) {
        List<Player> players = new ArrayList<>();
        for (int i=14; i<f.length; i++) { String[] p = Wire.unpack(f[i]); players.add(new Player(p[0],p[1],Boolean.parseBoolean(p[2]),Boolean.parseBoolean(p[3]),Boolean.parseBoolean(p[4]),Boolean.parseBoolean(p[5]),Integer.parseInt(p[6]),p[7],Integer.parseInt(p[8]))); }
        return new Snapshot(f[1], f[2], f[3], Long.parseLong(f[4]), Integer.parseInt(f[5]), Integer.parseInt(f[6]), Integer.parseInt(f[7]), f[8], Long.parseLong(f[9]), f[10], List.copyOf(players), now(),dev.rummage.core.MatchRules.decode(f[11]),f[12].isEmpty()?Set.of():Set.of(f[12].split(",")),Long.parseLong(f[13]));
    }
    public long secondsLeft() { return Math.max(0, (remaining - (now()-receivedAt) + 999)/1000); }
    public Player player(String id) { return players.stream().filter(p -> p.id.equals(id)).findFirst().orElse(null); }
    public static long now() { return System.nanoTime()/1_000_000; }
}
