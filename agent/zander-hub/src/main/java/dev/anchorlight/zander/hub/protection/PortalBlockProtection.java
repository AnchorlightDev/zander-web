package dev.anchorlight.zander.hub.protection;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Stops vanilla portal generation (lit Nether frames, paired exit portals, End
 * platforms) from replacing blocks that already exist in the world. If any block
 * the portal would place differs from a non-empty block already there, the whole
 * portal creation is cancelled rather than partially carving through builds.
 */
public class PortalBlockProtection implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        for (BlockState planned : event.getBlocks()) {
            Material existing = planned.getBlock().getType();
            if (wouldOverwrite(existing, planned.getType())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /// Blocks a portal may freely replace. Fire is included because lighting a
    /// frame replaces it with portal blocks.
    private static final Set<Material> EMPTY = EnumSet.of(
            Material.AIR, Material.CAVE_AIR, Material.VOID_AIR, Material.FIRE, Material.SOUL_FIRE);

    /// A placement only counts as overwriting when it changes a block that isn't
    /// empty space.
    static boolean wouldOverwrite(Material existing, Material planned) {
        return existing != planned && !EMPTY.contains(existing);
    }
}
