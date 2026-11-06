# Relay and signed-update setup

The 0.1.1 jar contains both services and their clients. No external server, DNS record, certificate, repository, signing key or release has been created by this change.

## TLS relay

Run on a reachable machine with Java 25 and a TLS certificate valid for its hostname. Import the certificate/private key into a Java-compatible PKCS12 keystore, then start:

```sh
java -Djavax.net.ssl.keyStore=/secure/relay.p12 -Djavax.net.ssl.keyStorePassword=YOUR_KEYSTORE_PASSWORD -cp rummage-0.1.1.jar dev.rummage.net.Relay 25571
```

Keep keystore credentials out of shared shell history and source control; use your service manager's private configuration. Open the relay's TCP port. Players enter `tls://relay.example.com:25571` under **Preferences → Network / Updates → Relay address**, then create/join lobbies. All members need the same endpoint. The host connects outward to the relay; its own game still uses a local synchronizer. The lobby's **Copy invite** switches to an `RM-` code after registration succeeds. Direct `BS1-` invites remain available when no relay is configured.

The relay validates ownership for host reconnects, bounds concurrent connections to 128, reserves disconnected codes for five minutes, uses bounded protocol frames and socket timeouts, and does not persist messages. It terminates TLS, so the operator can read traffic. It is a small friends-group relay, not a hardened public matchmaking service. Host/synchronizer state is not persisted. Both clients and the relay should be supervised as ordinary long-running services where appropriate.

Plain TCP relay endpoints are accepted **only for loopback tests**. Internet endpoints require TLS with normal hostname/certificate verification. Local tests run the relay directly on an ephemeral loopback port.

For a standalone synchronizer independent of a host's Minecraft process:

```sh
java -jar rummage-0.1.1.jar 25570 YOUR_REACHABLE_HOST
```

This command produces a direct invite. To additionally register with the relay, pass its endpoint as the third argument:

```sh
java -jar rummage-0.1.1.jar 25570 YOUR_REACHABLE_HOST tls://relay.example.com:25571
```

## Signed releases

Create an Ed25519 key **outside the repository**, once:

```sh
java tools/SignRelease.java generate /secure/rummage-release-key.pk8
```

The command refuses to overwrite an existing key and prints the public key. Distribute that public key through a trusted channel; users paste it into **Trusted public key**. Never distribute the private key. Back it up separately.

For each release, build the versioned jar and sign its exact bytes and intended HTTPS download URL:

```sh
java tools/SignRelease.java sign /secure/rummage-release-key.pk8 0.1.2 https://example.com/releases/rummage-0.1.2.jar build/libs/rummage-0.1.2.jar build/update.properties
```

Publish the jar and manifest at reachable HTTPS URLs (GitHub release assets are suitable). Configure the manifest URL in **Update manifest URL**. The trusted key is pinned in local preferences and is never taken from the downloaded manifest.

On startup, configured clients check without blocking the lobby. **Updates** also offers a manual check, verified download, and **Install on next game exit**. The installer waits for Minecraft to exit, verifies the staged bytes again, keeps `previous.jar.backup` under `config/rummage-update`, and installs the new version into the original `mods` directory. Automatic installation requires the running mod to be a jar in a folder named `mods`. Development runs can install the verified download manually. Installer errors go to `config/rummage-update/install.log`.

No release signing key or production update URL is bundled. Without configuration, update checks explain the missing setup and leave gameplay available.
