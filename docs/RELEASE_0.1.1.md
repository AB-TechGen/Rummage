# Rummage 0.1.1

All players must use 0.1.1. Install `build/libs/rummage-0.1.1.jar` with Fabric Loader 0.19.5+, Fabric API 0.165.0+26.3, Minecraft 26.3 and Java 25. Remove the previous Rummage jar from the mods folder.

## Playing and settings

Open **Rummage** from the title screen. **Preferences** is available before joining; **Rules / Preferences** appears in the lobby. Only the leader can change match rules, and rules lock at match start. Local display and network settings remain editable. Preferences are saved in `config/rummage.properties`.

| Addition | Behavior |
| --- | --- |
| Biome-aware targets | Enabled by default. Samples biomes around the shared spawn and excludes oak, kelp, bamboo, moss, terracotta and snow targets when their associated biome families are not detected. The pool now includes bamboo, moss, terracotta and snow from round 4. |
| Seed filters | Optional biome ID and structure ID, with a 100–2000 block radius. Examples: `minecraft:plains`, `minecraft:village_plains`. Blank filters accept any seed. The leader checks a generated world. Random seeds retry up to eight worlds; a typed seed that fails is rejected without replacement. These are actual world-load attempts, so filtering is slower than a terrain-free seed finder. Failed attempts can be retained/cleaned like other match worlds. |
| Lookout | 0–120 optional seconds in a fixed spectator view 20 blocks above the highest sampled terrain near spawn, followed by the existing five-second ground countdown. Completion is disabled until the round starts. |
| Standard loot RNG | Enabled by default. Persisted, world-seeded sequences for block types, mob loot tables and piglin bartering; stone and deepslate ores share their block-drop sequence. Chest loot remains vanilla. Fortune, Looting and other context still affect outcomes. Food effects, mob spawn timing, equipment drops outside loot tables and golem damage are not standardized. |
| Sudden death | Optional. With multiple survivors at the round cap, continue with `base seconds / (1 + extra rounds)`, down to a ten-second minimum. All failing simultaneously still draws. Solo practice finishes at the configured cap. |
| Race preset | Optional. Each player receives one lava bucket, water bucket, flint and steel, and 16 bread at the first active round. Reduced ordinary Nether fog; water/lava fog is preserved. Barters 36, 72, 108 and 144 yield three pearls, guaranteeing at least four pearl trades by 144 ingots. The preset does not replace vanilla terrain or relocate portals. |
| Fullbright | **G**, rebindable. A client lighting effect permitted by the leader's **Allow fullbright** rule. Only applies in a match world; no potion effect is added. |
| Target HUD | Block icon, target name, completion state and red final-ten-second warning. **H** toggles visibility. Scale 50–200% in Preferences, clamped to the viewport. |
| Player standings | **P**, rebindable, or **Players** in the lobby. Shows score, status, dimension and measured round-trip ping. |
| Draw voting | **Offer / accept draw** during a match. All connected surviving players must agree within 30 seconds. Press again to withdraw; a successful block completion cancels the vote. |
| Latency compensation | Server probes every two seconds; completion time is estimated by subtracting half the measured RTT, capped at 300 ms. Deadline resolution waits 300 ms. This does not enable offline completion claims. |
| Recovery | **Recover session** uses locally persisted credentials and reopens the saved match world after a client restart. Requires the same live synchronizer and an unexpired five-minute slot. Leaving the lobby intentionally clears the credentials. A host crash still stops an embedded synchronizer; use the standalone synchronizer for independence. |
| World retention | **Old worlds** lists marked worlds and lets you protect favourites. Default: retain all. **Keep worlds** enables automatic retention, or run the explicit cleanup action. Locked, ordinary, unmarked and protected worlds are skipped. Automatic cleanup also preserves the world recorded for recovery. |
| Fair-play controls | Enabled by default. Match worlds stay on Normal difficulty, commands and LAN publishing are blocked, and game-mode/rule changes are restricted. The engine still trusts completion reports; these are local guardrails, not anti-cheat. |
| Relay invites | The included TLS relay gives `RM-` codes. All players configure the same relay address. Host reconnections reserve code ownership for five minutes. A public relay requires deployment. |
| Signed updates | HTTPS manifest checks, Ed25519 verification, SHA-256 download verification, version comparison and an exit-time installer with rollback backup. A release URL and trusted public key must be configured. Failures do not block play. |

## Validation

The offline build compiles the Minecraft/Fabric client and packages the standalone services. Engine tests cover the original game, sudden death, draw votes, filtering/retry limits, compensation boundaries, persistence, protected/locked world cleanup, release signatures, real TCP relay tunneling and reconnect identity.

Compilation and automated tests do not validate rendering or mixin behavior in a running Minecraft client. Before publishing, test two clients: host/join through the relay, inspect rules on both, try a failing random filter, start a match with lookout, test HUD/fullbright, receive the race kit, complete a target, allow a deadline to expire, vote a draw, and play through sudden death. Restart a non-host client and recover its world. Confirm ordinary worlds are unaffected by the lighting, loot and fair-play hooks. Test update installation on a disposable mods directory using a signed newer release.
