package com.cristian.ftopforge.core;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TopCacheTest {

    private FactionSnapshot snap(String id, long total) {
        // Use the Builder, splitting `total` arbitrarily across slots
        return FactionSnapshot.builder(id, id + "_name")
                .leader("uuid-" + id, "leader-" + id)
                .addChunksValue(total)
                .build();
    }

    @Test
    void rebuildSortsDescByTotalValue() {
        TopCache cache = new TopCache();
        cache.rebuild(Arrays.asList(snap("a", 100), snap("b", 300), snap("c", 200)));
        List<FactionSnapshot> top = cache.top(10);
        assertEquals(3, top.size());
        assertEquals("b", top.get(0).factionId());
        assertEquals("c", top.get(1).factionId());
        assertEquals("a", top.get(2).factionId());
    }

    @Test
    void topNRespectsLimit() {
        TopCache cache = new TopCache();
        cache.rebuild(Arrays.asList(snap("a", 1), snap("b", 2), snap("c", 3), snap("d", 4)));
        assertEquals(2, cache.top(2).size());
        assertEquals("d", cache.top(2).get(0).factionId());
    }

    @Test
    void rankOfReturnsOneBasedIndexOrZero() {
        TopCache cache = new TopCache();
        cache.rebuild(Arrays.asList(snap("a", 100), snap("b", 300)));
        assertEquals(1, cache.rankOf("b"));
        assertEquals(2, cache.rankOf("a"));
        assertEquals(0, cache.rankOf("c")); // not present
    }

    @Test
    void getByIdReturnsCorrectSnapshot() {
        TopCache cache = new TopCache();
        cache.rebuild(Arrays.asList(snap("a", 100)));
        assertNotNull(cache.byId("a"));
        assertEquals(100, cache.byId("a").totalValue());
        assertNull(cache.byId("nope"));
    }
}
