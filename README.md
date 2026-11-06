# Rummage

A Minecraft **26.3 Fabric client mod**, inspired by OpenDraft's separate-world architecture. Everyone plays a fresh singleplayer world on the same seed. A small synchronizer shares lobby and round events, never terrain.

Find the target block and **stand on top of it** before the timer expires. Everyone gets the same target. Players who miss it are eliminated; the last survivor wins. Play alone to practice, or with up to eight players.

## Included in 0.1.1

Version 0.1.1 adds biome-aware targets, seed filters, saved preferences, crash recovery, a resizable block-icon HUD, relay invites, latency compensation, lookout, standardized loot, draw voting, world retention, richer standings, fair-play controls, and signed updates. Optional sudden death, a race preset, and fullbright are available in **Rules / Preferences**. See [0.1.1 details](docs/RELEASE_0.1.1.md) and [relay/update deployment](docs/DEPLOYMENT.md).

- Title-screen lobby, display names, invite copying, lobby chat and player standings.
- Fresh survival worlds created automatically on the shared seed.
- Loading barrier and five-second countdown; players are held in spectator mode at spawn until play starts.
- Automatic block detection on the integrated server, with a target/timer/status HUD. Press **B** to open the lobby; rebind it in Controls.
- Leader-controlled seed, round duration (30–1800 seconds) and round count (1–100). Defaults: random seed, five-minute rounds, 20 rounds.
- Curated block pool: common blocks first, more crafting/mining from round 4, Nether/rare resources from round 9. Consecutive targets never repeat.
- Five-second intermission once all surviving players finish a round. Inventory and world progress carry between rounds.
- Forfeit, elimination, shared victory at the round cap, and a draw if everyone fails together.
- Automatic connection retries with a five-minute reserved slot, including recovery after restarting Minecraft while the synchronizer remains online. The clock continues during a disconnect.
- A standalone synchronizer using the same jar, without Minecraft dependencies at runtime.

## Install and play

1. Install Minecraft **26.3**, **Java 25**, Fabric Loader **0.19.5+**, and Fabric API **0.165.0+26.3**.
2. Put `rummage-0.1.1.jar` from `build/libs/` into your Minecraft `mods` directory alongside Fabric API. Every player needs the same mod version.
3. Open **Rummage** at the top right of the title screen. Enter a display name.
4. One player presses **Create lobby**, then **Copy invite**. Others paste the invite and press **Join lobby**.
5. The leader chooses settings and presses **Start match**. Keep the lobby open: each client creates its match world automatically. If you are already in a world, Save and Quit first, then reopen the lobby.
6. Stand on the displayed block. It must support your feet; holding it, looking at it or jumping past it does not count.
7. Press **B** for standings, chat, or forfeit. After the match, the leader presses **Return to lobby**; clients save and leave their match worlds. Start again for a fresh seed/world, or retain the seed in settings.

Names are local lobby identities; Rummage does not request Microsoft credentials. It does not replace Minecraft's launcher or authentication.

### Connections

The host listens on TCP **25570**. A blank host-address field advertises the machine's detected LAN address. If that selects the wrong adapter, enter the correct LAN/VPN address before creating the lobby.

**Direct `BS1-` invites encode an address, port and lobby secret.** For direct play outside your LAN, use a reachable VPN address or port forwarding. Alternatively, configure the same **TLS relay address** on every client, then create a lobby and share its short `RM-` invite. Relay connections use TLS and require no inbound ports on players' routers. A relay server is included, but no public relay is configured or deployed by default. Share invites privately. Direct connections remain unencrypted; the relay operator can see relayed messages.

The host's Minecraft process owns the lobby. If it closes, everyone loses the lobby. To keep it independent, run:

```sh
java -jar build/libs/rummage-0.1.1.jar 25570
# Optionally advertise a particular reachable address:
java -jar build/libs/rummage-0.1.1.jar 25570 your-vpn-or-public-host
```

The terminal prints an invite; the first player to join becomes leader.

## Build and test

Requires JDK 25. The Gradle wrapper downloads Gradle 9.7.1 and dependencies on the first build.

```sh
./gradlew build
# Windows:
.\gradlew.bat build
```

`build/libs/rummage-0.1.1.jar` is both the client mod and standalone synchronizer. `build` runs the deterministic engine tests and local TCP integration tests. To run these tests without downloading Minecraft or Gradle, on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File tools/test.ps1
```

## Scope and current limitations

This implements Rummage adaptations of OpenDraft features, rather than full OpenDraft parity. The original reference is in [docs/OPENDRAFT_REFERENCE.md](docs/OPENDRAFT_REFERENCE.md). No public relay or update feed is deployed; configure those using [the deployment guide](docs/DEPLOYMENT.md). UPnP is not implemented.

- Worlds use vanilla terrain generation. Biome checks sample a grid and are conservative; they do not guarantee a particular block is present or reachable. Standard loot RNG covers block drops, mob loot tables, and piglin bartering, not every random event in Minecraft.
- The synchronizer trusts each mod's completion reports. This is friendly competition, not an anti-cheat system.
- Claims receive up to 300 ms of compensation based on server-measured round-trip time, with a 300 ms deadline settlement window. This is an estimate, not proof of the completion time. Offline claims are not backdated.
- Reconnect credentials and the match-world path persist locally. **Recover session** resumes within the synchronizer's five-minute reservation. Synchronizer state remains in memory: a host/server process restart loses its lobby.
- Match worlds are retained in `saves/Rummage <match-id>`. Retention defaults to **all**. Setting **Keep worlds** above zero enables cleanup on the title screen; only marked, unlocked Rummage worlds are eligible, and protected favourites are kept. The most recent session's world is also protected during automatic cleanup for recovery.
- Waiting and eliminated players are spectators. Pause screens do not pause an active match world; the synchronizer clock keeps running.

The Java sources have been compiled against cached Minecraft 26.3/Fabric APIs, and the engine/network tests have been run locally. In-game rendering, automatic world creation and multi-machine play still require a Minecraft smoke test; compilation alone does not verify them.

### Build verification in this workspace

The initial jar was produced with `java tools/CachedBuild.java build/dependencies` using local copies of the already installed Minecraft/Fabric dependencies. This fallback is specific to the unobfuscated 26.3 APIs; its dependency directory is generated and ignored by Git. It compiles all mod sources and packages only this project's classes/resources. The normal Gradle build reached Loom 1.18.4 but was blocked by a Windows sandbox `AccessDeniedException` opening `minecraft-server.jar`. Use the Gradle wrapper for ordinary builds outside this restriction.

Before treating this as a tested release, launch two Minecraft clients, create/join a lobby, start a match, verify the loading barrier and matching terrain, stand on a target block, let the other player's timer expire, then return to the lobby and start a second match. Also test B, the Players screen, chat, saving/rejoining the match world and disconnect/reconnect during a round. These interactive checks have not been performed here.

## Source layout

- `core/`: deterministic block pool and authoritative match state machine.
- `net/`: bounded message framing, TCP synchronizer, reconnecting client and immutable snapshots.
- `client/`: Fabric integration, world creation, server-side block detection, lobby and HUD.
- `src/test/`: timing boundaries, readiness, claims, ties, progression and TCP integration checks.

New code is MIT licensed; see [NOTICE.md](NOTICE.md) for reference-document attribution.
