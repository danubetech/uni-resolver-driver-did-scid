package uniresolver.driver.did.scid;

import foundation.identity.did.DID;
import org.junit.jupiter.api.Test;
import uniresolver.ResolutionException;
import uniresolver.driver.did.scid.srcdereferencers.SrcDereferencer;
import uniresolver.result.ResolveResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DidScidDriverTest {

    private static final String SCID = "QmeyXk5HskUDXoEpbDhTmKbE1vpABYpHobnB5fuGMRbYfZ";
    private static final String DID_SCID = "did:scid:vh:1:" + SCID;

    private static byte[] vector(String name) throws IOException {
        try (InputStream in = DidScidDriverTest.class.getResourceAsStream("/vh1/" + name + ".jsonl")) {
            return in.readAllBytes();
        }
    }

    /** The driver, with a 'src' dereferencer that answers `srcData` for any value. */
    private static DidScidDriver driver(byte[] srcData) throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("didResolverUri", "http://localhost:1/1.0/");
        properties.put("didWebvhDriverUri", "http://localhost:1/1.0/identifiers/");
        properties.put("didWebsDriverUri", "http://localhost:1/1.0/identifiers/");
        properties.put("wrapperFilesPath", Files.createTempDirectory("wrapper-files").toString());
        DidScidDriver driver = new DidScidDriver(properties);
        driver.setSrcDereferencers(List.of(new SrcDereferencer() {
            @Override
            public boolean canDereference(String srcValue) {
                return true;
            }

            @Override
            public byte[] dereference(String srcValue, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) {
                return srcData;
            }
        }));
        return driver;
    }

    @Test
    public void testLogThatNamesTheDidScid() throws Exception {
        ResolveResult resolveResult = driver(vector("scid-three-entries")).resolve(DID.fromString(DID_SCID), Map.of("src", "example.test/" + SCID));
        assertEquals(DID_SCID, resolveResult.getDidDocument().getId().toString(), "the DID being resolved, as W3C DID Resolution requires");
        assertTrue(resolveResult.getDidDocument().toJson().contains("urn:example:entry-3"), "the latest entry's document");
        assertEquals("3-QmQiYUxbbjPc6Cxrq5bX8p1ifTPJNMA2o7smeqDCjsgmHw", resolveResult.getDidDocumentMetadata().get("versionId"));
        assertEquals("2026-09-29T00:00:00Z", resolveResult.getDidDocumentMetadata().get("created"));
        assertEquals("ScidVhLog", resolveResult.getDidResolutionMetadata().get("sourceMethod"));
        assertEquals("example.test/" + SCID, resolveResult.getDidResolutionMetadata().get("srcValue"));
    }

    @Test
    public void testTamperedLog() throws Exception {
        byte[] tampered = new String(vector("scid-three-entries"), StandardCharsets.UTF_8).replace("urn:example:entry-3", "urn:example:forged").getBytes(StandardCharsets.UTF_8);
        ResolutionException ex = assertThrows(ResolutionException.class, () -> driver(tampered).resolve(DID.fromString(DID_SCID), Map.of("src", "example.test/" + SCID)));
        assertEquals(ResolutionException.ERROR_INVALID_DID_DOCUMENT, ex.getErrorType());
    }

    @Test
    public void testIdWithAHostInALaterEntry() throws Exception {
        ResolutionException ex = assertThrows(ResolutionException.class, () -> driver(vector("scid-with-host")).resolve(DID.fromString(DID_SCID), Map.of("src", "example.test/" + SCID)));
        assertEquals(ResolutionException.ERROR_INVALID_DID_DOCUMENT, ex.getErrorType());
    }
}
