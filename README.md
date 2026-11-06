# Rummage

**Same seed. Separate worlds. Find your block. Beat the clock.**

Rummage is a Minecraft Fabric mod for **1–8 players**. Everyone explores their own fresh singleplayer world on the same seed. Each round gives everyone the same target block: stand on it before time runs out or get eliminated. The last survivor wins.

| Current version | Minecraft | Java | Players |
| --- | --- | --- | --- |
| **0.1.1** | **26.3** | **25** | **1–8**, including solo practice |

[Install](#install) · [Play](#play) · [Controls](#controls) · [Settings](#settings) · [Play with friends](#play-with-friends) · [Recovery](#disconnects-and-recovery) · [Troubleshooting](#troubleshooting)

## Install

| You need | Version / action |
| --- | --- |
| Minecraft | **26.3** |
| Java | **25** |
| Fabric Loader | **0.19.5 or newer** |
| Fabric API | **0.165.0+26.3** |
| Rummage | **rummage-0.1.1.jar**, the same version for every player |

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
| **2. Connect** | Choose a connection method below, then **Create lobby → Copy invite**. | Paste the invite and press **Join lobby**. |
| **3. Configure** | Set seed, seconds and rounds. Open **Rules / Preferences** for optional modes, then save. | Check the player list and agreed rules. |
| **4. Start** | Press **Start match** and keep the lobby screen open. | Keep the lobby screen open so your match world can load. |
| **5. Find** | Stand on the displayed target block before the timer reaches zero. | The same target and deadline apply to everyone. |
| **6. Play again** | After the result, press **Return to lobby**, then start another match. | Your client saves and leaves the world when the leader returns to the lobby. |

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

Inventory and world progress carry between rounds. A new match creates fresh worlds. Common targets come first; more crafting and mining targets enter the pool at **round 4**, with Nether and rarer targets from **round 9**. Consecutive targets never repeat.

**The clock keeps running while menus are open or a player is disconnected.** Waiting and eliminated players are spectators.

## Controls

Rebind these under Minecraft **Controls → Key Binds**, in the Rummage categories.

| Default key / button | Action |
| --- | --- |
| **B** | Open or close the lobby for chat, match actions and standings. |
| **P** | Toggle the player standings screen: score, completion/elimination status, dimension and ping. |
| **H** | Show or hide the target HUD. |
| **G** | Toggle fullbright, if the leader allows it. |
| **Players** | Open standings from the lobby. |
| **Send** | Send the text entered in the lobby chat box. |
| **Join match world** | Reopen your existing match world after Save and Quit. |
| **Forfeit** | Give up your place in the current match. |
| **Offer / accept draw** | Start or accept a draw vote; press again to withdraw. |

Draw votes last **30 seconds** and need every connected surviving player's agreement. A successful block completion cancels the vote.

## Settings

Open **Preferences** before joining, or **Rules / Preferences** inside a lobby. Use **Previous / Next** to switch pages, then **Save**. Only the leader can change match rules, and those rules lock when a match starts. Local display preferences remain editable.

### Match and world rules

| Setting | Default | Choices / effect |
| --- | --- | --- |
| Seed | Random | Leave blank for a new random seed, or enter a numeric seed to reuse it. |
| Seconds | **300** | **30–1800** seconds per ordinary round. |
| Rounds | **20** | **1–100** rounds before shared victory or sudden death. |
| Biome-aware targets | **On** | Exclude biome-dependent targets when their associated biomes are not detected near spawn. |
| Sudden death | **Off** | Continue past the cap with shorter rounds until a winner or draw. |
| Lookout seconds | **0** | **0–120** seconds of aerial preview before the five-second ground countdown. |
| Standard loot RNG | **On** | Use saved, world-seeded random sequences for supported drops and bartering. |
| Race preset | **Off** | Give starting supplies, reduce ordinary Nether fog and guarantee early pearl trades. |
| Allow fullbright | **On** | Allow each player to toggle brighter match-world lighting with **G**. |
| Fair-play controls | **On** | Keep Normal difficulty and restrict commands, LAN publishing, game-mode changes and rule edits. |
| Required biome ID | Blank | Optional filter, for example `minecraft:plains`. |
| Required structure ID | Blank | Optional filter, for example `minecraft:village_plains`. |
| Search radius | **400** blocks | **100–2000** blocks around spawn for biome checks and requested filters. |

**Seed filtering:** the leader checks a generated world. Random seeds retry up to **eight worlds**; a typed seed that fails is rejected without replacement. These attempts load real worlds and can take time. Blank filters accept any seed. Biomes are sampled on a grid, so the checks cannot guarantee every target block is nearby or reachable.

Biome-aware selection covers **oak planks, dried kelp, bamboo, moss, terracotta and snow blocks**. Bamboo, moss, terracotta and snow targets enter from round 4.

### Optional modes

| Mode | What changes |
| --- | --- |
| **Sudden death** | With multiple survivors at the cap, extra rounds last `base seconds / (1 + extra rounds)`, with a **10-second minimum**. For a 300-second base, the first extra round is 150 seconds, then 100, then 75. Everyone failing still draws. Solo practice ends at the normal cap. |
| **Race preset** | At the first active round, each player gets **one lava bucket, one water bucket, flint and steel, and 16 bread**. Ordinary Nether fog is reduced; fluid, blindness and darkness fog are preserved. Barters **36, 72, 108 and 144** each yield **3 pearls**, guaranteeing at least four pearl trades by 144 ingots. Terrain and portal placement remain vanilla. |
| **Fullbright** | Press **G** to toggle the lighting effect in a match world. It starts off locally and requires **Allow fullbright** to be on. No potion effect is added. |
| **Lookout** | A fixed spectator view **20 blocks above the highest sampled terrain near spawn**. Everyone returns to the ground for the last five seconds. Finding blocks starts counting only when the round begins. |

### Local preferences

| Setting | Default | What to enter |
| --- | --- | --- |
| Show HUD | **On** | Toggle here or with **H**. |
| HUD scale (%) | **100** | **50–200**; automatically clamped to fit the screen. |
| Fullbright enabled | **Off** | Your local choice, subject to the leader's rule. |
| Keep worlds | **0 — keep all** | **1–1000** to enable retention and keep that many newest eligible worlds. |
| Relay address | Blank | The same `tls://hostname:port` for everyone using a relay. |
| Update manifest URL | Blank | An HTTPS update-feed address supplied by the release maintainer. |
| Trusted public key | Blank | The maintainer's trusted Ed25519 public key. |

Names, match settings, display choices and recovery details are saved in `config/rummage.properties`. That file contains private reconnect credentials; keep it private.

### What “standard loot RNG” covers

| Shared sequences | Still dependent on other factors |
| --- | --- |
| Block drops, by block type; stone and deepslate ores share a sequence. | Fortune, Looting and the context in which a drop happens. |
| Mob loot tables and piglin bartering. | Mob spawn timing, food effects, golem damage and equipment drops outside loot tables. |
| Sequences saved with the world, so reopening does not reset them. | Terrain access and the order in which you encounter resources. |

Chest loot uses vanilla generation. Matching seeds and supported sequences help keep the race fair, but do not make every random event identical.

## Play with friends

Choose the connection method **before creating the lobby**.

| Method | Host setup | Friend setup | Invite |
| --- | --- | --- | --- |
| **Same LAN / Wi-Fi** | Leave **Host address** blank, or enter the correct LAN address. Allow the host's TCP port through its firewall. | Be on a network that permits devices to reach each other. | `BS1-…` |
| **VPN** | Enter the reachable VPN address in **Host address**. | Join the same reachable VPN network. | `BS1-…` |
| **Port forwarding** | Forward TCP **25570** to the host and enter its reachable public hostname/address. | Use the copied invite. | `BS1-…` |
| **TLS relay** | Configure a reachable relay under **Preferences → Network / Updates → Relay address**. | Configure the **same relay address**. | `RM-…` |

**No public relay is bundled or configured.** A relay server is included for someone to host; setup is provided below. Rummage does not automatically open router ports with UPnP.

| Connection detail | What to know |
| --- | --- |
| Host port | In-game hosting listens on TCP **25570**. |
| Blank host address | Uses the machine's detected LAN address. If it chooses the wrong adapter, enter the correct address before hosting. |
| Direct invites | Contain the address, port and lobby secret. Share privately. Direct traffic is unencrypted. |
| Relay invites | **Copy invite** switches to an `RM-` code once relay registration succeeds. Players need no inbound router port. TLS protects each connection to the relay; the operator can read relayed messages. |
| Host closes Minecraft | The embedded synchronizer closes and everyone loses the lobby. Use the standalone option below to keep it independent. |

<details>
<summary><strong>Run a standalone synchronizer</strong></summary>

Needs **Java 25**, but no Minecraft installation or Fabric dependencies at runtime. From the directory containing the built jar:

```sh
java -jar build/libs/rummage-0.1.1.jar 25570 YOUR_REACHABLE_HOST
```

Replace `YOUR_REACHABLE_HOST` with the LAN, VPN or public address friends can reach. The terminal prints a direct invite. The **first player to join becomes leader**; everyone joins through the Rummage lobby screen.

To also register with a relay:

```sh
java -jar build/libs/rummage-0.1.1.jar 25570 YOUR_REACHABLE_HOST tls://relay.example.com:25571
```

Wait for the terminal to print the `RM-` invite. Every client must configure that relay endpoint. Closing the synchronizer still ends the lobby.

</details>

<details>
<summary><strong>Host your own TLS relay</strong></summary>

Use a reachable machine with **Java 25**, a hostname and a TLS certificate valid for that hostname. Import the certificate and private key into a Java-compatible **PKCS12 keystore**, open the relay's TCP port, then start:

```sh
java -Djavax.net.ssl.keyStore=/secure/relay.p12 -Djavax.net.ssl.keyStorePassword=YOUR_KEYSTORE_PASSWORD -cp build/libs/rummage-0.1.1.jar dev.rummage.net.Relay 25571
```

Replace the example paths and password with your deployment's values. Keep credentials in private service configuration rather than shared history or source control. Supervise the relay process if it should stay available.

| Relay property | Behavior |
| --- | --- |
| Client setting | `tls://relay.example.com:25571`, identical for all lobby members. |
| Certificates | Normal certificate and hostname verification; remote connections require TLS. |
| Router setup | Only the relay needs a reachable inbound port. Players connect outward. |
| Host reconnects | Code ownership is reserved for **five minutes** after disconnect. |
| Capacity | Up to **128 concurrent connections**, with bounded frames and socket timeouts. |
| Persistence | Messages and match state are not persisted by the relay. |
| Privacy | TLS ends at the relay, so the operator can read traffic. |
| Intended use | A small friends-group service; it is not hardened public matchmaking. |

Plain `tcp://` relay endpoints are supported only on loopback for local tests. A relay does not preserve a lobby whose synchronizer has stopped.

</details>

## Disconnects and recovery

| What happened? | What to do / expect |
| --- | --- |
| Brief network drop | Rummage retries automatically and reserves your slot for **five minutes**. The clock continues. |
| Minecraft crashed or restarted | Reopen Rummage and press **Recover session** within five minutes. Saved credentials restore your slot and reopen your recorded match world if the synchronizer is still running. |
| You used Save and Quit mid-match | Open the lobby and choose **Join match world** to return to the same save. |
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

To update manually, close Minecraft, remove the older Rummage jar from `mods`, put the new jar there, and restart. Everyone in a lobby needs the same mod version.

For signed updates, obtain the **HTTPS manifest URL** and **trusted public key** from the release maintainer, then enter them under **Preferences → Network / Updates**.

| Step | Action |
| --- | --- |
| **1. Check** | Open **Updates → Check for updates**. Configured clients also check on startup. |
| **2. Download** | Choose **Download verified update**. The signature and downloaded file hash must match. |
| **3. Schedule** | Choose **Install on next game exit**. |
| **4. Restart** | Save, quit Minecraft, then start it again. |

Automatic installation requires a running Rummage jar inside a folder named `mods`. The installer waits for Minecraft to exit, rechecks the download, and keeps a rollback copy at `config/rummage-update/previous.jar.backup`. Downloaded jars and `install.log` are in the same update directory. Development runs can install the verified download manually.

**No production update feed or signing key is preconfigured.** Missing configuration or a failed check leaves gameplay available.

<details>
<summary><strong>Maintainers: create and publish signed releases</strong></summary>

Generate an Ed25519 private key **outside the repository**, once:

```sh
java tools/SignRelease.java generate /secure/rummage-release-key.pk8
```

The command refuses to overwrite an existing key and prints its public key. Back up the private key and keep it private. Give users the public key through a trusted channel.

After building a new version, sign its exact jar bytes and intended HTTPS download URL. This example is for a future **0.1.2** release:

```sh
java tools/SignRelease.java sign /secure/rummage-release-key.pk8 0.1.2 https://example.com/releases/rummage-0.1.2.jar build/libs/rummage-0.1.2.jar build/update.properties
```

Publish the jar and manifest at reachable HTTPS URLs, such as release assets, and distribute the manifest URL. Clients verify the version, URL and SHA-256 together using the pinned public key; the downloaded manifest cannot supply its own trusted key.

</details>

## Troubleshooting

| Problem | Try this |
| --- | --- |
| Rummage button is missing | Check the Fabric profile, Minecraft version, Fabric API and the selected instance's `mods` folder. |
| Wrong invite or version | Make sure everyone has **0.1.1**, remove duplicate jars, and copy a fresh invite. |
| Friends cannot join a direct invite | Check the advertised address, firewall and TCP 25570 reachability. Use a VPN or relay if direct access is unavailable. |
| `RM-` invite will not connect | Everyone needs the same reachable relay endpoint with a valid TLS certificate. |
| Hosting fails | Check whether another lobby/process is already using TCP 25570. |
| Match world does not open | Save and Quit your current world, reopen the lobby, then choose **Join match world**. |
| Loading or seed search takes a long time | Wait for the slowest client; loading has a five-minute timeout per attempt. Relax filters or leave them blank. Random filters try up to eight worlds. |
| The HUD is missing or too large | Press **H**, check **Show HUD**, or reduce **HUD scale (%)**. |
| Fullbright does nothing | Check both your local toggle (**G**) and the leader's **Allow fullbright** rule. |
| Recovery fails | Confirm the same synchronizer is online, the five-minute reservation has not expired, and you did not intentionally leave. |
| Old saves are not being removed | Retention starts disabled. Check **Keep worlds**, favourites, world locks and recovery protection. Unmarked saves are intentionally excluded. |
| Update installation fails | Read `config/rummage-update/install.log`, then install the verified jar manually with Minecraft closed. |

## Build and test

Requires **JDK 25**. The wrapper downloads Gradle **9.7.1** and dependencies on the first build.

| Platform | Build command |
| --- | --- |
| Linux / macOS | `chmod +x gradlew && ./gradlew build` |
| Windows PowerShell | `.\gradlew.bat build` |

| Output / command | Purpose |
| --- | --- |
| `build/libs/rummage-0.1.1.jar` | Installable client mod, standalone synchronizer and relay. |
| `build/libs/rummage-0.1.1-sources.jar` | Source archive; do not install as the mod. |
| `./gradlew test` | Run the custom engine/integration suite through the standard test task. |
| `powershell -ExecutionPolicy Bypass -File tools/test.ps1` | Run engine/network tests on Windows without downloading Minecraft or Gradle. |

The custom `engineTest` runner currently passes **376 assertions**, covering rounds, networking, recovery, TLS relays, update verification and protected-world cleanup. The standard Gradle `test` task runs that suite and allows the expected absence of JUnit tests. Separate descriptor checks cover **15 mixin targets and shadow fields**. Rendering, automatic world creation and multi-machine gameplay still need in-game smoke testing.

<details>
<summary><strong>Developer checks before publishing</strong></summary>

Test two Minecraft clients through the relay: inspect rules on both, try a failing random-seed filter, start with lookout, toggle the HUD/fullbright, check the race kit and complete a block. Let a deadline expire, vote a draw, and reach sudden death. Restart a non-host client and recover its world. Confirm ordinary worlds are unaffected by the lighting, loot and fair-play hooks. Exercise a signed update against a disposable mods directory.

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
| Hosting | Match state lives in synchronizer memory. A server restart ends its lobby. No public relay or update service is deployed by this repository. |
| Version history | Each released version has its own file under `changelogs/`. This README describes current gameplay. |
| License and credits | New code is MIT licensed. The architecture was inspired by OpenDraft; its preserved reference retains its original authorship. No OpenDraft code, Draftout goal art or panorama assets are bundled. Minecraft and Fabric dependencies retain their own licenses and are not bundled in the distributable jar. |
