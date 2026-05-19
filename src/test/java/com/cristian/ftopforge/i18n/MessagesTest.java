package com.cristian.ftopforge.i18n;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MessagesTest {

    @Test
    void getReturnsResolvedStringWithPrefix() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("prefix", "&6[&eFTop&6] ");
        Map<String, Object> calc = new HashMap<>();
        calc.put("starting", "%prefix%&7Calculating...");
        raw.put("calculation", calc);
        Messages m = new Messages(raw, raw);
        String out = m.get("calculation.starting");
        assertTrue(out.contains("Calculating"), "should contain literal text");
        assertFalse(out.contains("%prefix%"), "should have substituted %prefix%");
        assertTrue(out.contains("[") && out.contains("FTop"), "prefix should be embedded");
    }

    @Test
    void getWithArgsSubstitutesNamedPlaceholders() {
        Map<String, Object> raw = new HashMap<>();
        Map<String, Object> cmd = new HashMap<>();
        Map<String, Object> ftop = new HashMap<>();
        ftop.put("line", "&6%rank%. &b%faction_name% &a$%value%");
        cmd.put("ftop", ftop);
        raw.put("command", cmd);
        raw.put("prefix", "");
        Messages m = new Messages(raw, raw);
        String out = m.get("command.ftop.line", "rank", "1", "faction_name", "Sparta", "value", "9999");
        assertTrue(out.contains("1.") && out.contains("Sparta") && out.contains("9999"));
        assertFalse(out.contains("%rank%"));
    }

    @Test
    void missingKeyFallsBackToDefaultLang() {
        Map<String, Object> en = new HashMap<>();
        en.put("prefix", "");
        Map<String, Object> only_en = new HashMap<>();
        only_en.put("starting", "EN starting");
        en.put("calculation", only_en);
        Map<String, Object> es = new HashMap<>();
        es.put("prefix", "");
        // ES is missing the calculation.starting key
        Messages m = new Messages(es, en);
        String out = m.get("calculation.starting");
        assertEquals("EN starting", out);
    }

    @Test
    void missingEverywhereReturnsKeyMarker() {
        Map<String, Object> empty = new HashMap<>();
        Messages m = new Messages(empty, empty);
        String out = m.get("nope.does.not.exist");
        assertTrue(out.contains("nope.does.not.exist"), "should echo missing key for debugging");
    }
}
