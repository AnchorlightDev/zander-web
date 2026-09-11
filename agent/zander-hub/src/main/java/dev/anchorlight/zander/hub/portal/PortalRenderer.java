package dev.anchorlight.zander.hub.portal;

import dev.anchorlight.zander.hub.protection.PortalBlockProtection;
import net.kyori.adventure.text.Component;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws each portal's {@link PortalAppearance} into the world and keeps it intact.
 * <p>
 * {@code NETHER} portals place real nether portal blocks, but only into empty space —
 * existing blocks are never replaced. Those blocks have no obsidian frame, so physics
 * updates that would break them are cancelled, and vanilla Nether travel from them is
 * suppressed so only the portal's own destination fires.
 * <p>
 * {@code TINT} portals are two non-persistent text displays (one per face) whose
 * backgrounds form a translucent coloured panel. They vanish with their chunk and are
 * respawned when it loads again, so nothing is ever left behind in the world save.
 */
public class PortalRenderer implements Listener {
    /// Upper bound on blocks a single nether-style portal may fill.
    public static final long MAX_NETHER_VOLUME = 4096;

    /// Gap between the two tint panels so they never z-fight.
    private static final double PANEL_OFFSET = 0.01;
    /// Keeps the far panel's anchor inside the region's own chunk.
    private static final double ANCHOR_EPSILON = 0.001;

    private final PortalService portalService;
    private final PortalSpatialIndex index;
    private final Map<String, List<TextDisplay>> tintPanels = new HashMap<>();

    public PortalRenderer(PortalService portalService, PortalSpatialIndex index) {
        this.portalService = portalService;
        this.index = index;
    }

    public void renderAll() {
        for (Portal portal : portalService.all()) {
            apply(null, portal);
        }
    }

    /// Removes tint panels on shutdown. Nether portal blocks are world blocks and stay.
    public void removeAllTints() {
        for (List<TextDisplay> panels : tintPanels.values()) {
            panels.forEach(TextDisplay::remove);
        }
        tintPanels.clear();
    }

    public void apply(Portal before, Portal after) {
        if (before != null) {
            removeTint(before);
            boolean stillSameNether = after != null
                    && after.appearance().style() == PortalAppearance.Style.NETHER
                    && after.region().equals(before.region());
            if (before.appearance().style() == PortalAppearance.Style.NETHER && !stillSameNether) {
                clearNether(before.region());
            }
        }
        if (after != null) {
            switch (after.appearance().style()) {
                case NETHER -> fillNether(after.region());
                case TINT -> spawnTint(after);
                case NONE -> { }
            }
        }
    }

    /// Counts blocks in the region that a nether fill would leave untouched because they're occupied.
    public int countOccupied(PortalRegion region) {
        World world = Bukkit.getWorld(region.world());
        if (world == null) {
            return 0;
        }
        int occupied = 0;
        for (int x = region.minX(); x <= region.maxX(); x++) {
            for (int y = region.minY(); y <= region.maxY(); y++) {
                for (int z = region.minZ(); z <= region.maxZ(); z++) {
                    Material type = world.getBlockAt(x, y, z).getType();
                    if (PortalBlockProtection.wouldOverwrite(type, Material.NETHER_PORTAL)) {
                        occupied++;
                    }
                }
            }
        }
        return occupied;
    }

    private void fillNether(PortalRegion region) {
        World world = Bukkit.getWorld(region.world());
        if (world == null || region.volume() > MAX_NETHER_VOLUME) {
            return;
        }
        Orientable data = (Orientable) Material.NETHER_PORTAL.createBlockData();
        data.setAxis(region.sizeX() >= region.sizeZ() ? Axis.X : Axis.Z);

        for (int x = region.minX(); x <= region.maxX(); x++) {
            for (int y = region.minY(); y <= region.maxY(); y++) {
                for (int z = region.minZ(); z <= region.maxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!PortalBlockProtection.wouldOverwrite(block.getType(), Material.NETHER_PORTAL)) {
                        block.setBlockData(data, false);
                    }
                }
            }
        }
    }

    private void clearNether(PortalRegion region) {
        World world = Bukkit.getWorld(region.world());
        if (world == null || region.volume() > MAX_NETHER_VOLUME) {
            return;
        }
        for (int x = region.minX(); x <= region.maxX(); x++) {
            for (int y = region.minY(); y <= region.maxY(); y++) {
                for (int z = region.minZ(); z <= region.maxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.NETHER_PORTAL) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private void removeTint(Portal portal) {
        List<TextDisplay> panels = tintPanels.remove(PortalIdValidator.normalise(portal.id()));
        if (panels != null) {
            panels.forEach(TextDisplay::remove);
        }
    }

    private void spawnTint(Portal portal) {
        PortalRegion region = portal.region();
        World world = Bukkit.getWorld(region.world());
        if (world == null) {
            return;
        }

        // A text display's background spans x in [-0.05, 0.075] and y in [0, 0.25] blocks at scale 1;
        // scaling by (8w, 4h) and shifting by 0.4w maps it exactly onto [0, w] x [0, h].
        List<Location> anchors = new ArrayList<>(2);
        int width;
        if (region.sizeX() >= region.sizeZ()) {
            width = region.sizeX();
            double z = region.minZ() + region.sizeZ() / 2.0;
            anchors.add(new Location(world, region.minX(), region.minY(), z + PANEL_OFFSET, 0f, 0f));
            anchors.add(new Location(world, region.maxX() + 1 - ANCHOR_EPSILON, region.minY(), z - PANEL_OFFSET, 180f, 0f));
        } else {
            width = region.sizeZ();
            double x = region.minX() + region.sizeX() / 2.0;
            anchors.add(new Location(world, x - PANEL_OFFSET, region.minY(), region.minZ(), 90f, 0f));
            anchors.add(new Location(world, x + PANEL_OFFSET, region.minY(), region.maxZ() + 1 - ANCHOR_EPSILON, -90f, 0f));
        }

        // Only spawn into loaded chunks; ChunkLoadEvent retries once the rest are loaded.
        for (Location anchor : anchors) {
            if (!world.isChunkLoaded(anchor.getBlockX() >> 4, anchor.getBlockZ() >> 4)) {
                return;
            }
        }

        int height = region.sizeY();
        Color colour = Color.fromARGB(portal.appearance().argb());
        List<TextDisplay> panels = new ArrayList<>(anchors.size());
        for (Location anchor : anchors) {
            panels.add(world.spawn(anchor, TextDisplay.class, panel -> {
                panel.setPersistent(false);
                panel.text(Component.space());
                panel.setDefaultBackground(false);
                panel.setBackgroundColor(colour);
                panel.setShadowed(false);
                panel.setBillboard(Display.Billboard.FIXED);
                panel.setBrightness(new Display.Brightness(15, 15));
                panel.setTransformation(new Transformation(
                        new Vector3f(0.4f * width, 0f, 0f), new AxisAngle4f(),
                        new Vector3f(8f * width, 4f * height, 1f), new AxisAngle4f()));
            }));
        }
        tintPanels.put(PortalIdValidator.normalise(portal.id()), panels);
    }

    private boolean tintIntact(Portal portal) {
        List<TextDisplay> panels = tintPanels.get(PortalIdValidator.normalise(portal.id()));
        return panels != null && panels.stream().allMatch(TextDisplay::isValid);
    }

    private boolean isNetherPortalBlock(World world, int x, int y, int z) {
        for (Portal portal : index.candidatesFor(world.getName(), x >> 4, z >> 4)) {
            if (portal.appearance().style() == PortalAppearance.Style.NETHER && portal.region().contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Portal portal : index.candidatesFor(event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ())) {
            if (portal.appearance().style() == PortalAppearance.Style.TINT && !tintIntact(portal)) {
                removeTint(portal);
                spawnTint(portal);
            }
        }
    }

    /// Frameless portal blocks would otherwise pop the moment a neighbouring block updates.
    @EventHandler(ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.NETHER_PORTAL
                && isNetherPortalBlock(block.getWorld(), block.getX(), block.getY(), block.getZ())) {
            event.setCancelled(true);
        }
    }

    /// Stops vanilla Nether travel so the portal's configured destination is the only thing that fires.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPortalEnter(EntityPortalEnterEvent event) {
        Location location = event.getLocation();
        if (event.getPortalType() == PortalType.NETHER && location.getWorld() != null
                && isNetherPortalBlock(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
            event.setCancelled(true);
        }
    }
}
