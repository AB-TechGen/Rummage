package dev.rummage.core;

import java.util.*;

/** Single authoritative state machine, synchronized by its transport. Times are monotonic milliseconds. */
public final class Match {
    public enum Phase { LOBBY, LOADING, COUNTDOWN, ROUND, INTERMISSION, FINISHED }
    public static final String VERSION = "1.0.0";
    public static final long RESULT_MS = 8000;
    private boolean returnScheduled;
    public static final long RECONNECT_MS = 300_000;
    public static final class Player {
        public final String id, name;
        public boolean connected = true, ready, alive = true, complete;
        public int score, colorIndex;
        public long disconnectedAt;
        public String dimension = "minecraft:overworld";
        public int ping;
        public Player(String id, String name) { this.id = id; this.name = name; }
    }
    public final LinkedHashMap<String, Player> players = new LinkedHashMap<>();
    public Phase phase = Phase.LOBBY;
    public String leader = "", target = "", result = "", matchId = "";
    public long seed, deadline;
    public int round, roundSeconds = 300, maxRounds = 20;
    private boolean solo;
    private long activeMillis, roundStartedAt;
    private final List<String> targetHistory=new ArrayList<>();
    public MatchRules rules = MatchRules.DEFAULT;
    public final Set<String> excluded = new HashSet<>(), drawVotes = new HashSet<>();
    public long drawDeadline;
    public int graceMillis, seedAttempts;
    public boolean randomSeed;
    public boolean worldApproved;
    public void rules(String id, MatchRules value, boolean random) {
        requireLeader(id);
        if(phase!=Phase.LOBBY)throw new IllegalArgumentException("Settings are locked during a match.");
        rules=value; randomSeed=random;
    }
    public void worldReport(String id, String match, Set<String> blocked, boolean accepted, long now) {
        requireLeader(id);
        if(phase!=Phase.LOADING || !matchId.equals(match))return;
        if(!accepted) {
            if(randomSeed && ++seedAttempts<8) { seed=new Random(seed).nextLong(); begin(now); }
            else { phase=Phase.FINISHED; result="Seed filter not met. Change filters or seed and retry."; }
            return;
        }
        for(String block:blocked)if(Set.of("minecraft:bamboo_block","minecraft:moss_block","minecraft:terracotta","minecraft:snow_block","minecraft:oak_planks","minecraft:dried_kelp_block").contains(block))excluded.add(block);
        worldApproved=true;
    }
    public void voteDraw(String id, long now) {
        Player p=player(id);
        if(phase==Phase.LOBBY || phase==Phase.FINISHED || !p.alive || !p.connected)return;
        if(drawVotes.isEmpty())drawDeadline=now+30_000;
        if(!drawVotes.add(id))drawVotes.remove(id);
        checkDraw(now);
    }
    private void checkDraw(long now) {
        if(now>=drawDeadline){drawVotes.clear();return;}
        var voters=players.values().stream().filter(p->p.alive&&p.connected).toList();
        if(!voters.isEmpty()&&voters.stream().allMatch(p->drawVotes.contains(p.id))) {phase=Phase.FINISHED;result="Draw agreed by all active players.";drawVotes.clear();}
    }

    public Player join(String name) {
        if (phase != Phase.LOBBY) throw new IllegalArgumentException("Match in progress; wait for the next lobby.");
        if (players.size() >= 8) throw new IllegalArgumentException("Lobby is full (8 players).");
        name = name.strip().replaceAll("[^\\p{L}\\p{N}_ -]", "");
        if (name.isEmpty() || name.length() > 24) throw new IllegalArgumentException("Use a name of 1 to 24 letters or numbers.");
        String base = name;
        int suffix = 2;
        while (hasName(name)) name = base + " " + suffix++;
        Player p = new Player(UUID.randomUUID().toString(), name);
        while(hasColor(p.colorIndex))p.colorIndex++;
        players.put(p.id, p);
        if (leader.isEmpty()) leader = p.id;
        return p;
    }
    private boolean hasColor(int color){return players.values().stream().anyMatch(p->p.colorIndex==color);}
    private boolean hasName(String name) { return players.values().stream().anyMatch(p -> p.name.equalsIgnoreCase(name)); }
    public void configure(String id, long seed, int seconds, int rounds) {
        requireLeader(id);
        if (phase != Phase.LOBBY) throw new IllegalArgumentException("Settings are locked during a match.");
        if (seconds < 30 || seconds > 1800 || rounds < 1 || rounds > 100) throw new IllegalArgumentException("Round length: 30–1800 seconds. Rounds: 1–100.");
        this.seed = seed; roundSeconds = seconds; maxRounds = rounds;
    }
    public void start(String id, long now) {
        requireLeader(id);
        if (phase != Phase.LOBBY || players.isEmpty()) throw new IllegalArgumentException("Return to the lobby first.");
        if (players.values().stream().anyMatch(p -> !p.connected)) throw new IllegalArgumentException("Wait for away players or remove them by returning to the lobby.");
        begin(now);
    }
    private void begin(long now) {
        activeMillis=0;roundStartedAt=0;targetHistory.clear();
        matchId = UUID.randomUUID().toString();
        returnScheduled=false;
        phase = Phase.LOADING; deadline = now + 300_000; round = 0; target = ""; result = "";
        solo = players.size() == 1;
        excluded.clear(); drawVotes.clear();worldApproved=false;
        players.values().forEach(p -> { p.ready = false; p.alive = true; p.complete = false; p.score = 0; });
    }
    public void ready(String id, String match, long worldSeed) {
        if (phase != Phase.LOADING || !matchId.equals(match) || worldSeed != seed) throw new IllegalArgumentException("Load the new match world with the lobby seed first.");
        player(id).ready = true;
    }
    public void claim(String id, String match, int round, String block, long now) {
        Player p = player(id);
        if (phase != Phase.ROUND || now >= deadline || !matchId.equals(match) || this.round != round || !target.equals(block) || !p.alive || !p.connected || p.complete) return;
        p.complete = true; p.score++;
        drawVotes.clear();
    }
    public void disconnect(String id, long now) { Player p = players.get(id); if (p != null) { p.connected = false; p.disconnectedAt = now; } }
    public void reconnect(String id, long now) {
        Player p = player(id);
        if (p.connected || now - p.disconnectedAt >= RECONNECT_MS) throw new IllegalArgumentException("Reconnect slot expired or already connected.");
        p.connected = true;
    }
    public void leave(String id, long now) {
        if (phase == Phase.LOBBY) players.remove(id);
        else { Player p = player(id); p.alive = false; p.connected = false; p.disconnectedAt = now - RECONNECT_MS; }
        chooseLeader();
        checkWinner();
    }
    public void forfeit(String id) { if (phase != Phase.LOBBY && phase != Phase.FINISHED) { player(id).alive = false; checkWinner(); } }
    public void reset(String id) {
        requireLeader(id);
        if (phase != Phase.FINISHED && phase != Phase.LOADING) throw new IllegalArgumentException("Finish the match first.");
        resetToLobby();
    }
    private void resetToLobby(){
        returnScheduled=false;
        players.values().removeIf(p -> !p.connected);
        phase = Phase.LOBBY; round = 0; deadline = 0; target = ""; result = "";drawVotes.clear();drawDeadline=0;
        players.values().forEach(p -> { p.alive = true; p.ready = false; p.complete = false; p.score = 0; });
        chooseLeader();
    }
    public void tick(long now) {
        for (Player p : new ArrayList<>(players.values())) {
            if (!p.connected && now - p.disconnectedAt >= RECONNECT_MS) {
                if (phase == Phase.LOBBY) players.remove(p.id); else p.alive = false;
            }
        }
        chooseLeader();
        if (phase == Phase.LOBBY) return;
        if (phase == Phase.FINISHED) {
            if(!returnScheduled){returnScheduled=true;deadline=now+RESULT_MS;}
            else if(now>=deadline)resetToLobby();
            return;
        }
        checkDraw(now);
        if(phase==Phase.FINISHED)return;
        if (checkWinner()) return;
        if (phase == Phase.LOADING) {
            if (worldApproved && players.values().stream().anyMatch(p->p.alive&&p.connected) && players.values().stream().filter(p -> p.alive && p.connected).allMatch(p -> p.ready)) { phase = Phase.COUNTDOWN; deadline = now + 5000 + rules.lookoutSeconds()*1000L; }
            else if (now >= deadline) { phase = Phase.FINISHED; result = "World loading timed out. Return to lobby to retry."; }
        } else if (phase == Phase.COUNTDOWN && now >= deadline) nextRound(now);
        else if (phase == Phase.ROUND) {
            if (now >= deadline + graceMillis) { players.values().stream().filter(p -> !p.complete).forEach(p -> p.alive = false); endRound(now); }
            else if (players.values().stream().filter(p -> p.alive).allMatch(p -> p.complete)) endRound(now);
        } else if (phase == Phase.INTERMISSION && now >= deadline) nextRound(now);
    }
    private void nextRound(long now) { round++; target = BlockPool.draw(seed, round, target, excluded, targetHistory, activeMillis); targetHistory.add(target); roundStartedAt=now; players.values().forEach(p -> p.complete = false); phase = Phase.ROUND; int seconds=rules.suddenDeath() && !solo && round>maxRounds ? Math.max(10,roundSeconds/(1+round-maxRounds)) : roundSeconds; deadline = now + seconds * 1000L; }
    private void endRound(long now) {
        activeMillis+=Math.max(0,Math.min(now,deadline)-roundStartedAt);
        if (checkWinner()) return;
        if (round >= maxRounds && (!rules.suddenDeath() || solo)) { finishSurvivors("Completed all rounds: "); return; }
        phase = Phase.INTERMISSION; deadline = now + 5000;
    }
    private boolean checkWinner() {
        if (phase == Phase.LOBBY || phase == Phase.FINISHED) return false;
        long count = players.values().stream().filter(p -> p.alive).count();
        if (count == 0) { phase = Phase.FINISHED; result = "Draw — no surviving players."; return true; }
        if (!solo && count == 1) { finishSurvivors("Winner: "); return true; }
        return false;
    }
    private void finishSurvivors(String prefix) { phase = Phase.FINISHED; result = prefix + String.join(", ", players.values().stream().filter(p -> p.alive).map(p -> p.name).toList()); }
    private void chooseLeader() { if (!players.containsKey(leader) || !players.get(leader).connected) leader = players.values().stream().filter(p -> p.connected).map(p -> p.id).findFirst().orElse(""); }
    private Player player(String id) { Player p = players.get(id); if (p == null) throw new IllegalArgumentException("Unknown player."); return p; }
    private void requireLeader(String id) { if (!leader.equals(id)) throw new IllegalArgumentException("Only the lobby leader can do that."); }
}
