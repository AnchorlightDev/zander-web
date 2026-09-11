# Custom Portals & Server Navigation

## Hub Paper server deployment

The plugin-level Nether/End protection (`dimensions.*` in zander-hub's `config.yml`)
is defence in depth. The Hub Paper server should additionally disable Nether
world loading at the Paper level, in `config/paper-global.yml` (not part of this
repository):

```yaml
misc:
  enable-nether: false
```

This is a server-deployment setting, not something Zander can set for you — apply
it directly on the Hub server's Paper configuration and restart.

## ServerPermissions node requirements

Backend server access is decided exclusively by Velocity via the `ServerPermissions`
plugin, using nodes of the form:

```text
serverpermissions.server.<velocity-server-id>
```

Example grants (exact syntax depends on your permissions plugin, e.g. LuckPerms):

```text
/lp group default permission set serverpermissions.server.survival true
/lp user Notch permission set serverpermissions.server.vip-lounge true
```

A configured portal `permission` (e.g. `zanderhub.portal.use.vip-lounge`) only
gates *portal activation* on Hub — it never substitutes for the
`serverpermissions.server.<id>` check, which Velocity always performs as the
final authority before connecting a player to a server portal's destination.

## Velocity deployment

1. Ensure `hub-bridge.allowed-source-servers` in zander-velocity's `config.yml`
   lists every Paper server name (as registered in Velocity's own config) that is
   allowed to originate `zander:hub` bridge requests — normally just `hub`.
2. Restart Velocity after changing `hub-bridge` settings; there is no reload
   command for this section.

## Manual integration testing checklist

- [ ] Vanilla Nether portal from Hub does not transport the player.
- [ ] `/tp` (or similar) into a Nether world is rejected without `zanderhub.nether.bypass`.
- [ ] Op with `zanderhub.nether.bypass` can still travel to the Nether.
- [ ] Setting `dimensions.end.blocked: true` blocks End travel; `false` allows it.
- [ ] A player force-placed into the Nether is returned to Hub spawn within one tick, once.
- [ ] `/zportal wand` gives a wand; left/right-click block sets pos1/pos2 with feedback.
- [ ] `/zportal create test-portal` fails without a two-point selection, succeeds with one.
- [ ] `/zportal setserver test-portal survival` + `/zportal enable test-portal` lets a permitted
      player walk in and connect to `survival`; a player lacking
      `serverpermissions.server.survival` is denied on Velocity, not Hub.
- [ ] `/zportal setlocation info-portal` + walking in teleports to the saved location with
      correct yaw/pitch.
- [ ] Restarting the Hub server preserves all portal data.
- [ ] Standing inside a portal without moving does not repeatedly trigger it.
- [ ] Two adjacent local portals do not immediately loop into each other.
- [ ] The navigation compass shows only servers the player has access to (or shows them
      locked, depending on `hide-inaccessible`), with live player counts.
- [ ] Rapid repeated compass clicks send exactly one `CONNECT_REQUEST`.
- [ ] A `zander:hub` message sent from a non-allow-listed backend server is rejected and logged.
- [ ] `/zportal reload` reports the correct portal count and rebuilds the spatial index.
- [ ] `/zportal send survival` connects the sender; `/zportal send survival <player>` connects that
      player and reports Velocity's result (started / denied / failed) back to the sender.
- [ ] `/zportal setstyle test-portal nether` fills only empty blocks in the region with nether portal
      blocks, reports how many occupied blocks were left untouched, and the blocks survive
      neighbouring block updates.
- [ ] Standing in a nether-style portal runs the portal's destination, never vanilla Nether travel.
- [ ] `/zportal setstyle test-portal none` removes the nether portal blocks it placed.
- [ ] `/zportal setcolour test-portal #33ccff` (or a dye name like `light_blue`) shows a translucent
      tinted panel visible from both sides; `#AARRGGBB` controls opacity.
- [ ] Tinted panels reappear after their chunk unloads and reloads, and leave no entities behind
      after a server restart.
- [ ] Vanilla portal generation (lighting a frame, exit portals, End platform) never replaces
      existing blocks.

## Portal appearance

| Command | Effect |
| --- | --- |
| `/zportal setstyle <id> none` | Invisible trigger region (default). |
| `/zportal setstyle <id> nether` | Fills empty blocks with frameless nether portal blocks (max 4096 blocks). Always purple. |
| `/zportal setstyle <id> tint` | Translucent panel in the portal's saved colour. |
| `/zportal setcolour <id> <colour>` | Sets the colour (`#RRGGBB`, `#AARRGGBB`, or a dye name) and switches to `tint`. |
| `/zportal send <server-id> [player]` | Sends yourself or an online player to a Velocity server (`zanderhub.portal.send`). |

Nether portal blocks cannot be recoloured without a resource pack, which is why colour uses the
`tint` style instead.
