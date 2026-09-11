package dev.anchorlight.zander.hub.portal;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class PortalAppearanceTest {
    @Test
    void parsesRgbHexWithDefaultAlpha() {
        assertEquals(Optional.of(0x8033CCFF), PortalAppearance.parseColour("#33ccff"));
        assertEquals(Optional.of(0x8033CCFF), PortalAppearance.parseColour("33CCFF"));
    }

    @Test
    void parsesArgbHexKeepingAlpha() {
        assertEquals(Optional.of(0xFF33CCFF), PortalAppearance.parseColour("#FF33CCFF"));
    }

    @Test
    void parsesDyeNamesIncludingSpellingVariants() {
        assertEquals(Optional.of(0x803AB3DA), PortalAppearance.parseColour("light-blue"));
        assertEquals(PortalAppearance.parseColour("light_gray"), PortalAppearance.parseColour("LIGHT_GREY"));
    }

    @Test
    void rejectsInvalidColours() {
        assertTrue(PortalAppearance.parseColour("#12345").isEmpty());
        assertTrue(PortalAppearance.parseColour("chartreuse").isEmpty());
        assertTrue(PortalAppearance.parseColour(null).isEmpty());
    }

    @Test
    void formatRoundTripsThroughParse() {
        int argb = 0x4011AA22;
        assertEquals(Optional.of(argb), PortalAppearance.parseColour(PortalAppearance.formatColour(argb)));
    }

    @Test
    void parsesStylesCaseInsensitively() {
        assertEquals(Optional.of(PortalAppearance.Style.NETHER), PortalAppearance.parseStyle("Nether"));
        assertTrue(PortalAppearance.parseStyle("rainbow").isEmpty());
    }

    @Test
    void portalWithoutAppearanceDefaultsToNone() {
        Portal portal = new Portal("hub", "Hub", true, new PortalRegion("world", 0, 0, 0, 1, 1, 1),
                new ServerPortalDestination("survival"), null, 0L, null, "s", "d");
        assertEquals(PortalAppearance.NONE, portal.appearance());
    }

    @Test
    void repositoryRoundTripsAppearance(@TempDir Path tempDir) {
        File file = tempDir.resolve("portals.yml").toFile();
        PortalRepository repository = new PortalRepository(file, Logger.getLogger("test"), world -> true);
        Portal portal = new Portal("hub", "Hub", true, new PortalRegion("world", 0, 60, 0, 2, 63, 0),
                new ServerPortalDestination("survival"), null, 0L, null, "s", "d",
                new PortalAppearance(PortalAppearance.Style.TINT, 0x6633CCFF));

        repository.save(List.of(portal));

        assertEquals(portal, repository.load().get("hub"));
    }

    @Test
    void serviceNotifiesListenerOnChangesOnly(@TempDir Path tempDir) {
        File file = tempDir.resolve("portals.yml").toFile();
        PortalService service = new PortalService(
                new PortalRepository(file, Logger.getLogger("test"), world -> true), new PortalSpatialIndex());
        List<String> events = new ArrayList<>();
        service.setChangeListener((before, after) ->
                events.add((before == null ? "-" : before.appearance().style()) + ">" + (after == null ? "-" : after.appearance().style())));

        Portal portal = new Portal("hub", "Hub", true, new PortalRegion("world", 0, 60, 0, 2, 63, 0),
                new ServerPortalDestination("survival"), null, 0L, null, "s", "d");
        service.put(portal);
        service.put(portal); // unchanged, no event
        service.put(portal.withAppearance(PortalAppearance.NONE.withStyle(PortalAppearance.Style.NETHER)));
        service.delete("hub");

        assertEquals(List.of("->NONE", "NONE>NETHER", "NETHER>-"), events);
    }
}
