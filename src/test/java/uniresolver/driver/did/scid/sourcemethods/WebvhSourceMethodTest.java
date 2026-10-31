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

public class WebvhSourceMethodTest {

    @Test
    public void testPrepareSrcDataKeepsEveryEntry() throws Exception {
        byte[] log;
        try (InputStream in = WebvhSourceMethodTest.class.getResourceAsStream("/vh1/webvh-portable-move.jsonl")) {
            log = in.readAllBytes();
        }
        String text = new String(log, StandardCharsets.UTF_8);
        assertEquals(2, text.strip().split("\n").length, "a log of two entries");
        String scid = new ObjectMapper().readTree(text.split("\n")[0]).path("parameters").path("scid").asText();

        WebvhSourceMethod sourceMethod = new WebvhSourceMethod(null);
        DID sourceDid = sourceMethod.toSourceDid(scid, log, new HashMap<>(), new HashMap<>());
        Path wrapperFiles = Files.createTempDirectory("wrapper-files");
        Map<String, Object> didResolutionMetadata = new HashMap<>();
        sourceMethod.prepareSrcData(sourceDid, wrapperFiles.toString(), log, didResolutionMetadata, new HashMap<>());

        File written = new File((File) didResolutionMetadata.get("src.path"), "did.jsonl");
        assertEquals(text, Files.readString(written.toPath(), StandardCharsets.UTF_8), "the whole log, not only its first entry");
    }
}
