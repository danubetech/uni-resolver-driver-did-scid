package uniresolver.driver.did.scid.sourcemethods;

import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.identity.did.DID;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebvhSourceMethodTest {

    /** Where prepareSrcData puts the log of the did:webvh in `resource`, relative to the wrapper files' root. */
    private static String writtenAt(String resource) throws Exception {
        byte[] log;
        try (InputStream in = WebvhSourceMethodTest.class.getResourceAsStream(resource)) {
            log = in.readAllBytes();
        }
        String scid = new ObjectMapper().readTree(new String(log, StandardCharsets.UTF_8).split("\n")[0]).path("parameters").path("scid").asText();
        WebvhSourceMethod sourceMethod = new WebvhSourceMethod(null);
        DID sourceDid = sourceMethod.toSourceDid(scid, log, new HashMap<>(), new HashMap<>());
        Path wrapperFiles = Files.createTempDirectory("wrapper-files");
        Map<String, Object> didResolutionMetadata = new HashMap<>();
        sourceMethod.prepareSrcData(sourceDid, wrapperFiles.toString(), log, didResolutionMetadata, new HashMap<>());
        File written = new File((File) didResolutionMetadata.get("src.path"), "did.jsonl");
        assertTrue(written.isFile(), written.toString());
        return wrapperFiles.relativize(written.toPath()).toString();
    }

    @Test
    public void testHostOnlyDidWebvh() throws Exception {
        // did:webvh:{SCID}:example.com: the did:webvh driver reads https://example.com/.well-known/did.jsonl.
        assertEquals(".well-known/did.jsonl", writtenAt("/vh1/webvh-genesis.jsonl"));
    }

    @Test
    public void testDidWebvhWithAPath() throws Exception {
        // did:webvh:{SCID}:example.com%3A8443:dids:{SCID}: https://example.com:8443/dids/{SCID}/did.jsonl, as before.
        assertEquals("dids/QmRuSBaeyv1s2EgvJLM9iLSogwNyqh5CWGaJxBGdpL6k3S/did.jsonl", writtenAt("/vh1/webvh-path.jsonl"));
    }
}
