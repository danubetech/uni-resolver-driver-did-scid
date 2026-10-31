package uniresolver.driver.did.scid.vh1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Logs made by an independent implementation (src/test/resources/vh1): each is signed properly
 * (SCID, hash chain, times, eddsa-jcs-2022 proofs), so a refused one is refused for its ids alone.
 */
public class ScidVhLogTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SCID = "QmeyXk5HskUDXoEpbDhTmKbE1vpABYpHobnB5fuGMRbYfZ";
    private static final Instant NOW = Instant.parse("2026-10-30T00:00:00Z");

    private static byte[] vector(String name) throws IOException {
        try (InputStream in = ScidVhLogTest.class.getResourceAsStream("/vh1/" + name + ".jsonl")) {
            return in.readAllBytes();
        }
    }

    private static String scidOf(byte[] log) throws Exception {
        return ScidVhLog.parse(log).get(0).path("parameters").path("scid").asText();
    }

    @Test
    public void testThreeEntries() throws Exception {
        ScidVhLog.Result result = ScidVhLog.verify(vector("scid-three-entries"), SCID, NOW);
        assertEquals("did:scid:vh:1:" + SCID, result.didDocument.path("id").asText());
        assertEquals("urn:example:entry-3", result.didDocument.path("alsoKnownAs").get(0).asText(), "the latest entry's document, not the first");
        assertEquals("3-QmQiYUxbbjPc6Cxrq5bX8p1ifTPJNMA2o7smeqDCjsgmHw", result.versionId);
        assertEquals("2026-09-29T00:02:00Z", result.versionTime);
        assertEquals("2026-09-29T00:00:00Z", result.created);
        assertEquals(3, result.versionNumber);
        assertFalse(result.deactivated);
    }

    @Test
    public void testGenesis() throws Exception {
        byte[] log = vector("scid-genesis");
        ScidVhLog.Result result = ScidVhLog.verify(log, scidOf(log), NOW);
        assertEquals(1, result.versionNumber);
        assertTrue(ScidVhLog.namesItself(log, scidOf(log)));
    }

    @Test
    public void testIdsThatAreNotTheDidScid() throws Exception {
        for (String name : List.of("scid-with-query", "scid-with-fragment", "scid-other-scid", "scid-with-host", "scid-moves-to-webvh")) {
            byte[] log = vector(name);
            ScidVhLog.InvalidLogException ex = assertThrows(ScidVhLog.InvalidLogException.class, () -> ScidVhLog.verify(log, scidOf(log), NOW));
            assertTrue(ex.getMessage().contains("must be did:scid:vh:1:"), name + ": " + ex.getMessage());
        }
    }

    @Test
    public void testOtherMethodsAndWitnesses() throws Exception {
        byte[] vhMethod = vector("scid-with-vh-method");
        assertThrows(ScidVhLog.InvalidLogException.class, () -> ScidVhLog.verify(vhMethod, scidOf(vhMethod), NOW));
        byte[] webvh = vector("webvh-genesis");
        assertFalse(ScidVhLog.namesItself(webvh, scidOf(webvh)), "a did:webvh log goes to the did:webvh driver, as before");
        assertThrows(ScidVhLog.InvalidLogException.class, () -> ScidVhLog.verify(webvh, scidOf(webvh), NOW));
        byte[] witnessed = vector("witness-at-genesis");
        ScidVhLog.InvalidLogException ex = assertThrows(ScidVhLog.InvalidLogException.class, () -> ScidVhLog.verify(witnessed, scidOf(witnessed), NOW));
        assertTrue(ex.getMessage().contains("witness"), ex.getMessage());
    }

    @Test
    public void testTheWrongScid() throws Exception {
        byte[] log = vector("scid-three-entries");
        assertThrows(ScidVhLog.InvalidLogException.class, () -> ScidVhLog.verify(log, "QmSomeoneElseSomeoneElseSomeoneElseSomeoneEls", NOW));
        assertFalse(ScidVhLog.namesItself(log, "QmSomeoneElseSomeoneElseSomeoneElseSomeoneEls"));
    }

    @Test
    public void testTampering() throws Exception {
        // A changed document: the hash chain no longer holds.
        assertRefused(edit(2, e -> ((ArrayNode) e.with("state").withArray("alsoKnownAs")).set(0, "urn:example:forged")), "hash chain");
        // A changed signature.
        assertRefused(edit(2, e -> {
            ObjectNode proof = (ObjectNode) e.withArray("proof").get(0);
            String v = proof.get("proofValue").asText();
            proof.put("proofValue", v.substring(0, v.length() - 2) + (v.endsWith("1") ? "22" : "11"));
        }), "signature");
        // An entry left out.
        List<JsonNode> entries = ScidVhLog.parse(vector("scid-three-entries"));
        List<JsonNode> gap = new ArrayList<>(List.of(entries.get(0), entries.get(2)));
        ScidVhLog.InvalidLogException ex = assertThrows(ScidVhLog.InvalidLogException.class, () -> ScidVhLog.verify(gap, SCID, NOW));
        assertTrue(ex.getMessage().contains("should start with 2-"), ex.getMessage());
        // Not yet: in the future.
        ex = assertThrows(ScidVhLog.InvalidLogException.class, () -> ScidVhLog.verify(entries, SCID, Instant.parse("2026-09-29T00:00:30Z")));
        assertTrue(ex.getMessage().contains("future"), ex.getMessage());
    }

    @Test
    public void testJcs() throws Exception {
        JsonNode node = objectMapper.readTree("{\"b\":[1,2.5,1e21,0.000001,1e-7,-0.0,true,null],\"a\":\"€\\u0001\\n\\\"\",\"é\":{\"z\":1,\"Z\":2}}");
        assertEquals("{\"a\":\"€\\u0001\\n\\\"\",\"b\":[1,2.5,1e+21,0.000001,1e-7,0,true,null],\"é\":{\"Z\":2,\"z\":1}}", ScidVhLog.jcs(node));
    }

    @Test
    public void testParseSkipsBlankLines() throws Exception {
        byte[] log = (new String(vector("scid-three-entries"), StandardCharsets.UTF_8) + "\n\n").getBytes(StandardCharsets.UTF_8);
        assertEquals(3, ScidVhLog.parse(log).size());
    }

    private interface Edit {
        void apply(ObjectNode entry);
    }

    private static List<JsonNode> edit(int index, Edit edit) throws IOException, ScidVhLog.InvalidLogException {
        List<JsonNode> entries = ScidVhLog.parse(vector("scid-three-entries"));
        ObjectNode copy = entries.get(index).deepCopy();
        edit.apply(copy);
        entries.set(index, copy);
        return entries;
    }

    private static void assertRefused(List<JsonNode> entries, String why) {
        ScidVhLog.InvalidLogException ex = assertThrows(ScidVhLog.InvalidLogException.class, () -> ScidVhLog.verify(entries, SCID, NOW));
        assertTrue(ex.getMessage().contains(why), ex.getMessage());
    }
}
