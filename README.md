# Rummage

**Same seed. Separate worlds. Find your block. Beat the clock.**

Rummage is a Minecraft Fabric mod for **1–8 players**. Everyone explores their own fresh singleplayer world on the same seed. Each round gives everyone the same target block: stand on it before time runs out or get eliminated. The last survivor wins.

| Current version | Minecraft | Java | Players |
| --- | --- | --- | --- |
| **0.2.0** | **26.3** | **25** | **1–8**, including solo practice |

[Install](#install) · [Play](#play) · [Controls](#controls) · [Settings](#settings) · [Play with friends](#play-with-friends) · [Recovery](#disconnects-and-recovery) · [Troubleshooting](#troubleshooting)

## Install

| You need | Version / action |
| --- | --- |
| Minecraft | **26.3** |
| Java | **25** |
| Fabric Loader | **0.19.5 or newer** |
| Fabric API | **0.165.0+26.3** |
| Rummage | **rummage-0.2.0.jar**, the same version for every player |

1. Install Fabric Loader for Minecraft 26.3 and select that profile in your launcher.
2. Get the Rummage jar from a release, a successful GitHub **Actions → Build → Artifacts → rummage** download, or build it using the commands below. Extract the artifact ZIP if using GitHub Actions.
3. Put **Rummage and Fabric API** in the Minecraft instance's `mods` folder. Remove older Rummage jars; use the regular jar, not the `-sources.jar`.
4. Launch the Fabric profile. The **Rummage** button appears at the top right of the title screen.

| Launcher / OS | Default `mods` folder |
| --- | --- |
| Windows | `%appdata%\.minecraft\mods` |
| macOS | `~/Library/Application Support/minecraft/mods` |
| Linux | `~/.minecraft/mods` |
| Launcher with separate instances | The selected instance's own `mods` folder |

Display names identify players inside a lobby. Rummage does not ask for Microsoft credentials; launch Minecraft through your usual launcher.

## Play

| Step | Host / leader | Other players |
| --- | --- | --- |
| **1. Open** | Title screen → **Rummage**; enter a display name. | Open Rummage and enter a display name. |
| **2. Connect** | Press **Create lobby**, wait for the connection note, then **Copy invite**. Setup happens automatically. | Paste the invite and press **Join lobby**. |
| **3. Configure** | Open **Edit Match Settings**, choose the match and world options, then save. | Check the player list and agreed rules. |
| **4. Start** | Press **Start match** and keep the lobby screen open. | Keep the lobby screen open so your match world can load. |
| **5. Find** | Stand on the displayed target block before the timer reaches zero. | The same target and deadline apply to everyone. |
| **6. Play again** | Results remain visible for eight seconds, then everyone returns to the lobby automatically. Start another match when ready. | Your client saves and leaves the match world automatically. |

**Already in a world?** Save and Quit first, then reopen Rummage. For solo practice, create a lobby and start without inviting anyone.

### What counts as finding a block?

| Counts | Does not count |
| --- | --- |
| Standing on the target with your feet supported by it. | Holding it, looking at it, or jumping past it. |
| Finding a naturally generated block or placing one yourself and standing on it. | Standing on a different block with a similar appearance. |
| Completing during the active round. | Completing during loading, lookout, countdown or intermission. |

The HUD turns green and shows **Block found!** when your completion is confirmed. Its timer warning turns red for the final ten seconds.

### How rounds end

| Situation | Result |
| --- | --- |
| All surviving players find the block early | Five-second intermission, then the next target. |
| Time runs out | Players who have not completed are eliminated. |
| One player survives in multiplayer | That player wins. |
| Everyone fails the same round | Draw. |
| Several players reach the round cap | Shared victory, unless sudden death is enabled. |
| Solo player reaches the cap | Practice complete. |
| Players agree to a draw | The match ends in a draw. |

The compact target HUD sits at the top right. Loading covers the world until everyone is ready, the final countdown appears in the centre, and results appear as a banner. **B** opens match actions over the world; **Lobby / Chat** opens the social lobby. Forfeit requires a second press within four seconds. When the game ends, everyone spectates during the eight-second result countdown, then returns to the lobby automatically—even if the original leader disconnected.

Inventory and world progress carry between rounds. A new match creates fresh worlds. Common targets come first; more crafting and mining targets enter the pool at **round 4**, with Nether and rarer targets from **round 9**. Consecutive targets never repeat.

**The clock keeps running while menus are open or a player is disconnected.** Waiting and eliminated players are spectators.

## Controls

Rebind these under Minecraft **Controls → Key Binds**, in the Rummage categories.

| Default key / button | Action |
| --- | --- |
| **B** | Open the match panel during play; open the lobby outside a match. |
| **P** | Toggle the player standings screen: score, completion/elimination status, dimension and ping. |
| **H** | Show or hide the target HUD. |
| **G** | Toggle fullbright, if the leader allows it. |
| **Enter / numpad Enter / Send** | Send lobby chat, including solo lobbies. Enter also joins from the invite field. |
| **Tab (hold)** | Show standings, dimension icons, scores and ping during play. |
| **Join match world** | Reopen your existing match world after Save and Quit. |
| **Forfeit** | Give up your place in the current match. |
| **Offer / accept draw** | Start or accept a draw vote; press again to withdraw. |

Draw votes last **30 seconds** and need every connected surviving player's agreement. A successful block completion cancels the vote.

## Settings

Only the **leader** can open **Edit Match Settings** in a connected, pre-game lobby. **Match** contains round time, round limit, sudden death and lookout; **World** contains the seed, optional biome/structure filters and search radius. Changes apply with **Save Settings** and lock when the match starts.

Personal controls stay separate: **H** toggles the target HUD and **G** toggles fullbright (both rebindable in Minecraft Controls). The **B** match panel contains the HUD size slider. **Player chat** and **Old worlds** are local controls on the lobby itself.

### Match and world rules

| Setting | Default | Choices / effect |
| --- | --- | --- |
| Seed | Random | Leave blank for a new random seed, or enter a numeric seed to reuse it. |
| Seconds | **300** | **30–1800** seconds per ordinary round. |
| Rounds | **20** | **1–100** rounds before shared victory or sudden death. |
| Sudden death | **Off** | Continue past the cap with shorter rounds until a winner or draw. |
| Lookout seconds | **0** | **0–120** seconds of aerial preview before the five-second ground countdown. |
| Required biome | Any | Choose a named biome, or enter a custom ID. |
| Required structure | Any | Choose a named structure, or enter a custom ID. |
| Search radius | **400** blocks | **100–2000** blocks around spawn for biome checks and requested filters. |

**Seed filtering:** the leader checks a generated world. Random seeds retry up to **eight worlds**; a typed seed that fails is rejected without replacement. These attempts load real worlds and can take time. Blank filters accept any seed. Biomes are sampled on a grid, so the checks cannot guarantee every target block is nearby or reachable.

Biome-aware selection covers **oak planks, dried kelp, bamboo, moss, terracotta and snow blocks**. Bamboo, moss, terracotta and snow targets enter from round 4.

### Optional modes

| Mode | What changes |
| --- | --- |
| **Sudden death** | With multiple survivors at the cap, extra rounds last `base seconds / (1 + extra rounds)`, with a **10-second minimum**. For a 300-second base, the first extra round is 150 seconds, then 100, then 75. Everyone failing still draws. Solo practice ends at the normal cap. |
| **Always-on gameplay** | Ordinary Nether fog is reduced; fluid, blindness and darkness fog remain. Barters **36, 72, 108 and 144** each yield **3 pearls**. No starting supplies are given. Terrain and portal placement remain vanilla. |
| **Fullbright** | Press **G** to toggle the lighting effect in a match world. It starts off locally and is always available in match worlds. No potion effect is added. |
| **Lookout** | A fixed spectator view **20 blocks above the highest sampled terrain near spawn**. Everyone returns to the ground for the last five seconds. Finding blocks starts counting only when the round begins. |

### Local preferences

| Setting | Default | What to enter |
| --- | --- | --- |
| Show HUD | **On** | Toggle with **H**, or rebind it in Minecraft Controls. |
| Player chat in world | **On** | Relay world chat to the lobby and show lobby messages while playing. |
| HUD scale (%) | **100** | **50–200** in the **B** match panel; automatically clamped to fit the screen. |
| Fullbright enabled | **Off** | Your local choice, available in every match. |
| Keep worlds | **0 — keep all** | Cycle the **Old worlds** lobby button to keep a limited number of newest eligible worlds. |

Names, match settings, display choices and recovery details are saved in `config/rummage.properties`. That file contains private reconnect credentials; keep it private.

### Shared chat and advancements

Lobby chat works in solo and multiplayer lobbies. Press Enter (including numpad Enter) or click Send. World chat also goes to the lobby when Player chat is on. Your name is cyan; other players use OpenDraft's red/yellow/green/etc. palette, with stable assignments across reconnects. Colours also appear in the lobby and standings.

New announced advancements are shared once with the lobby, using the player's colour and vanilla task/goal/challenge styling. Loading or recovering an existing world does not replay old advancements, and your local vanilla announcement is replaced by the shared coloured message while connected. Relay interruptions and restorations use discreet grey notices; repeated retry notices are limited.

### Always-on gameplay rules

Every match uses biome-aware targets, standardized supported loot RNG, reduced ordinary Nether fog, guaranteed pearl trades and fair-play restrictions. Difficulty stays Normal, and commands, LAN publishing and rule edits are restricted. Fullbright is always available as a personal toggle. No starting kit is given.

### What standardized loot RNG covers

| Shared sequences | Still dependent on other factors |
| --- | --- |
| Block drops, by block type; stone and deepslate ores share a sequence. | Fortune, Looting and the context in which a drop happens. |
| Mob loot tables and piglin bartering. | Mob spawn timing, food effects, golem damage and equipment drops outside loot tables. |
| Sequences saved with the world, so reopening does not reset them. | Terrain access and the order in which you encounter resources. |

Chest loot uses vanilla generation. Matching seeds and supported sequences help keep the race fair, but do not make every random event identical.

## Play with friends

**Create lobby → wait for the connection note → Copy invite.** Friends paste the code and press **Join lobby**. No relay address, Cloudflare account, certificate or router setup is needed for the normal flow.

Rummage uses the connection system from OpenDraft, including its built-in Cloudflare Worker endpoint. Your game tries these steps automatically, in order:

| Order | What the game does | What you see |
| --- | --- | --- |
| **1. Cloudflare** | Opens a WebSocket connection to the built-in Worker and obtains a short invite. The synchronizer stays in the host's game. | A code like **K7QM-2D9X** and **Friends anywhere can join with this code.** |
| **2. Router / UPnP** | If the Worker cannot be reached, asks the home router to forward the synchronizer's TCP port. | A longer invite containing the LAN and public addresses. Joining tries both simultaneously. |
| **3. Same network** | If automatic forwarding fails, still makes a direct invite and explains the restriction. If the router reports a private/provider address, treats it as CGNAT. | **Only people on your network can join**, with the port number and an explanation. |

| Automatic behavior | Details |
| --- | --- |
| Port selection | Tries TCP **25570**, then another free port if it is occupied. The invite contains the actual port. |
| Address discovery | Uses the router's local/public addresses when available; otherwise finds a LAN address and uses `api.ipify.org` for the public-address lookup when needed. |
| Router compatibility | Discovers UPnP Internet Gateway Device v1/v2, supports WANIPConnection/WANPPPConnection, and retries a rejected permanent mapping with a one-day lease. |
| Leaving / quitting | Removes the automatic router mapping. A shutdown hook also handles normal game exit. |
| Temporary Worker disconnect | Tries to reclaim the **same code** using its private host key for up to **five minutes**. Joining players reconnect automatically. |
| Expired Worker code | Tells the host to leave and create a new lobby. It does not silently replace an established short code with another connection method. |
| Several Worker addresses | Hosting tries them in order; joining asks all of them at once and uses the one with that lobby. |
| Manual joining | The join field also accepts `host`, `host:port`, or `[IPv6]:port`, for a VPN/tunnel address or a manually forwarded port. |
| Older invites | Existing `BS1-` direct invites remain supported. Older `RM-` invites need their original custom TLS endpoint; use a new automatic invite for the normal flow. |

**Share invites privately.** They provide access to your lobby. Direct connections are unencrypted. Cloudflare connections use TLS, but the Worker operator can read the relayed messages. A port mapping does not guarantee access through an ISP's CGNAT, and a public-IP lookup does not prove a port is reachable; the connection note stays conservative in those cases.

If both automatic internet methods fail:

| Situation | Fallback |
| --- | --- |
| Friends on the same LAN / Wi-Fi | Use the direct invite. The network must allow devices to reach each other; some dorm/public networks block this. |
| Router has UPnP disabled | Enable UPnP, or manually forward the displayed TCP port to the host. The long invite already includes available addresses. |
| ISP uses CGNAT / phone hotspot / restricted network | Use a reachable VPN or tunnel, such as Tailscale, ZeroTier or playit.gg. Friends enter its host address and the displayed port in the join field. |
| Wrong LAN adapter / address | Join using the correct reachable address and port directly. |
| macOS asks for local-network access | Allow Minecraft to discover devices so it can reach the router. |

The host's Minecraft process owns the synchronizer. Closing it ends the lobby even when Cloudflare is carrying the connection. Run the standalone option below if the lobby should be independent of a player's game.

<details>
<summary><strong>Run a standalone synchronizer</strong></summary>

Needs **Java 25**, but no Minecraft installation or Fabric dependencies at runtime:

```sh
java -jar build/libs/rummage-0.2.0.jar
```

The terminal prepares the connection automatically and prints its invite. The **first player to join becomes leader**; everyone joins through Rummage's lobby screen.

| Optional flag | Effect |
| --- | --- |
| `--port 25570` | Prefer a particular listening port; fall back to a free one if occupied. |
| `--relay https://worker.example.com,https://backup.example.com` | Override the built-in Worker list. |
| `--no-relay` | Skip Cloudflare and try UPnP/direct connection. |

Closing the synchronizer ends the lobby and removes its router mapping.

</details>

<details>
<summary><strong>Advanced: optional Worker override or deployment</strong></summary>

Players can use the built-in endpoint without deploying anything:

```text
https://opendraft-relay.opendraftmcserver.workers.dev
```

To use alternative compatible Workers, set `relay=https://your-worker.example.com` in `config/rummage.properties`. Several comma-separated addresses are supported. An empty `relay=` uses the built-in endpoint. To disable Cloudflare explicitly, use `relay=off`. Everyone using an override must use the same Worker list. Restart Minecraft after changing this advanced file setting; normal players need no relay configuration.

The optional Worker source is in `relay/src/index.js`. To deploy a separate copy with Node.js and your own Cloudflare account:

```sh
cd relay
npx wrangler login
npx wrangler deploy
```

Deployment prints the Worker URL. Put it in the advanced setting above, or change `CloudRelay.DEFAULT_URL` when distributing a build with a different default. The deployment config uses a separate `rummage-relay` name; it does not redeploy the existing OpenDraft Worker.

The inherited Worker uses Durable Objects and WebSocket hibernation, hashes host recovery keys, reserves disconnected rooms for five minutes, and limits each room to 32 relay connections (Rummage's game limit remains eight). Worker availability and account limits can cause automatic fallback. Rummage does not download or run a `cloudflared` binary; it connects directly over WebSockets.

</details>

## Disconnects and recovery

| What happened? | What to do / expect |
| --- | --- |
| Brief network drop | Rummage retries automatically and reserves your slot for **five minutes**. The clock continues. |
| Minecraft crashed or restarted | Reopen Rummage and press **Recover session** within five minutes. Saved credentials restore your slot and reopen your recorded match world if the synchronizer is still running. |
| You used Save and Quit mid-match | Open the lobby, select **Match**, then **Join Match World** to return to the same save. |
| You pressed **Leave lobby** | You leave immediately and clear the saved recovery credentials. |
| You were away for five minutes | Your reserved slot expires. |
| The host/synchronizer stopped | Its lobby state is lost; start a new lobby. |

Completions are estimated using server-measured ping, with up to **300 ms** of compensation and a **300 ms** deadline settlement window. This is an estimate of when a message was sent. **Offline completions are not backdated**: after reconnecting, the target must still be underfoot in the same active round.

## Saved worlds

Match worlds live in `saves/Rummage <match-id>`. **Old worlds** on the Rummage start screen lists marked match saves and lets you **Protect / Unprotect** favourites.

| Retention choice | Effect |
| --- | --- |
| **Keep worlds = 0** | Keep everything. This is the default. |
| **Keep worlds = 1–1000** | On the title screen, remove older eligible saves beyond that many newest unprotected worlds. |
| **Protect** | Keep the world regardless of retention. |
| **Clean old worlds** | Explicit cleanup with confirmation, available when no singleplayer world is open and retention is enabled. |

Cleanup only considers **marked Rummage worlds**. Ordinary saves, unmarked worlds, locked/open worlds and protected favourites are skipped. Automatic cleanup also keeps the world recorded for recovery, so the actual total can exceed your retention count. Manually cleaning old worlds can remove an unprotected recovery world; protect it first if you want to keep it.

## Updates

Updates are manual: close Minecraft, replace the old Rummage jar in the instance's `mods` folder with the new release, then restart. Everyone in a lobby needs the same version.

The old Updates screen configured an optional signed update feed. No production feed or trusted signing key was bundled, so that screen and startup update checks have been removed. Existing update preferences are ignored. Maintainer signing and verification utilities remain in the repository for future use.

## Troubleshooting

| Problem | Try this |
| --- | --- |
| Rummage button is missing | Check the Fabric profile, Minecraft version, Fabric API and the selected instance's `mods` folder. |
| Wrong invite or version | Make sure everyone has **0.2.0**, remove duplicate jars, and copy a fresh invite. |
| Friends cannot join a direct invite | Check the displayed port, host firewall and address reachability. The fallback note explains whether only LAN access is available. |
| Short invite will not connect | Wait if the host is recovering its connection. If the code expired, create a new lobby. Check any advanced Worker override matches on all clients. |
| Hosting is preparing an invite | Let the automatic Worker/UPnP checks finish. An occupied TCP 25570 is handled automatically. |
| Match world does not open | Save and Quit your current world, reopen the lobby, select **Match**, then **Join Match World**. |
| Loading or seed search takes a long time | Wait for the slowest client; loading has a five-minute timeout per attempt. Relax filters or leave them blank. Random filters try up to eight worlds. |
| The HUD is missing or too large | Press **H**, check **Show HUD**, or reduce **HUD scale (%)**. |
| Fullbright does nothing | Use **G** in a match world; check the fullbright key bind in Minecraft Controls. |
| Recovery fails | Confirm the same synchronizer is online, the five-minute reservation has not expired, and you did not intentionally leave. |
| Old saves are not being removed | Retention starts disabled. Check **Keep worlds**, favourites, world locks and recovery protection. Unmarked saves are intentionally excluded. |

## Build and test

Requires **JDK 25**. The wrapper downloads Gradle **9.7.1** and dependencies on the first build.

| Platform | Build command |
| --- | --- |
| Linux / macOS | `chmod +x gradlew && ./gradlew build` |
| Windows PowerShell | `.\gradlew.bat build` |

| Output / command | Purpose |
| --- | --- |
| `build/libs/rummage-0.2.0.jar` | Installable client mod, standalone synchronizer and relay. |
| `build/libs/rummage-0.2.0-sources.jar` | Source archive; do not install as the mod. |
| `./gradlew test` | Run the custom engine/integration suite through the standard test task. |
| `powershell -ExecutionPolicy Bypass -File tools/test.ps1` | Run engine/network tests on Windows without downloading Minecraft or Gradle. |

The custom `engineTest` runner currently passes **446 assertions**, covering rounds, networking, recovery, TLS relays, update verification and protected-world cleanup. The standard Gradle `test` task runs that suite and allows the expected absence of JUnit tests. Separate descriptor checks cover **17 mixin targets and shadow fields**. An optional live Worker probe is available with `java -cp build/libs/rummage-0.2.0.jar tools/ProbeCloudRelay.java`; it creates and closes a temporary lobby. This environment blocked that outbound probe with `EACCES`; local WebSocket/UPnP fallback tests passed. Rendering, automatic world creation and multi-machine gameplay still need in-game smoke testing.

<details>
<summary><strong>Developer checks before publishing</strong></summary>

Test two Minecraft clients through the automatic connection: inspect rules on both, try a failing random-seed filter, start with lookout, toggle the HUD/fullbright, check the HUD size slider, confirm no starting supplies are given and complete a block. Let a deadline expire, vote a draw, and reach sudden death. Restart a non-host client and recover its world. Confirm ordinary worlds are unaffected by the lighting, loot and fair-play hooks.

| Source folder | Responsibility |
| --- | --- |
| `src/main/java/dev/rummage/core/` | Match state, block pool, settings, retention and update verification. |
| `src/main/java/dev/rummage/net/` | Message framing, synchronizer, relay, reconnecting client and snapshots. |
| `src/main/java/dev/rummage/client/` | Fabric integration, match worlds, lobby, HUD and gameplay hooks. |
| `src/test/` | Engine, TCP/TLS integration, persistence and update checks. |

</details>

## Project notes

| Topic | Details |
| --- | --- |
| Fair play | Completion reports are trusted. Local restrictions help enforce agreed rules but are not an anti-cheat system. |
| Hosting | Match state lives in synchronizer memory. A server restart ends its lobby. The built-in OpenDraft Cloudflare Worker is used automatically; no production update feed is preconfigured. |
| Version history | Each released version has its own file under `changelogs/`. This README describes current gameplay. |
| License and credits | New code is MIT licensed. The architecture was inspired by OpenDraft; its preserved reference retains its original authorship. MIT-licensed OpenDraft connection code is included, with its license packaged under `META-INF/licenses/`. No Draftout goal art or panorama assets are bundled. Minecraft and Fabric dependencies retain their own licenses and are not bundled in the distributable jar. |
