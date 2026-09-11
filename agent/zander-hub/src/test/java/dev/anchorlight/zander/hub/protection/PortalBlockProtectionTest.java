package dev.anchorlight.zander.hub.protection;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortalBlockProtectionTest {
    @Test
    void placingIntoAirIsAllowed() {
        assertFalse(PortalBlockProtection.wouldOverwrite(Material.AIR, Material.NETHER_PORTAL));
        assertFalse(PortalBlockProtection.wouldOverwrite(Material.CAVE_AIR, Material.OBSIDIAN));
    }

    @Test
    void replacingFireWhenLightingFrameIsAllowed() {
        assertFalse(PortalBlockProtection.wouldOverwrite(Material.FIRE, Material.NETHER_PORTAL));
        assertFalse(PortalBlockProtection.wouldOverwrite(Material.SOUL_FIRE, Material.NETHER_PORTAL));
    }

    @Test
    void unchangedBlocksAreAllowed() {
        assertFalse(PortalBlockProtection.wouldOverwrite(Material.OBSIDIAN, Material.OBSIDIAN));
    }

    @Test
    void replacingSolidBlocksIsBlocked() {
        assertTrue(PortalBlockProtection.wouldOverwrite(Material.STONE, Material.OBSIDIAN));
        assertTrue(PortalBlockProtection.wouldOverwrite(Material.OAK_PLANKS, Material.AIR));
    }

    @Test
    void replacingFluidsAndPlantsIsBlocked() {
        assertTrue(PortalBlockProtection.wouldOverwrite(Material.WATER, Material.NETHER_PORTAL));
        assertTrue(PortalBlockProtection.wouldOverwrite(Material.SHORT_GRASS, Material.AIR));
    }
}
