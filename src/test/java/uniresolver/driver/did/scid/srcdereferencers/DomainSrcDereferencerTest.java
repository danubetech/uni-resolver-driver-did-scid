package uniresolver.driver.did.scid.srcdereferencers;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DomainSrcDereferencerTest {

    @Test
    public void testHostAndPath() throws Exception {
        assertEquals(List.of(
                URI.create("https://bengo.pub/QmYAPKv4iCFsMx3s4EPRP3duFJtcnmWLn9VdTn3ziEAB38/did.jsonl"),
                URI.create("https://bengo.pub/QmYAPKv4iCFsMx3s4EPRP3duFJtcnmWLn9VdTn3ziEAB38")
        ), DomainSrcDereferencer.candidateUrls("bengo.pub/QmYAPKv4iCFsMx3s4EPRP3duFJtcnmWLn9VdTn3ziEAB38"));
    }

    @Test
    public void testHostOnly() throws Exception {
        assertEquals(List.of(
                URI.create("https://example.com/.well-known/did.jsonl"),
                URI.create("https://example.com")
        ), DomainSrcDereferencer.candidateUrls("example.com"));
    }

    @Test
    public void testHostWithPort() throws Exception {
        assertEquals(URI.create("https://example.com:8443/dids/x/did.jsonl"), DomainSrcDereferencer.candidateUrls("example.com:8443/dids/x").get(0));
    }

    @Test
    public void testRawGistUrl() throws Exception {
        // The Resolution Overview's example: the log is the gist's did.jsonl file; the value itself is still tried after.
        assertEquals(List.of(
                URI.create("https://gist.githubusercontent.com/brianorwhatever/9c4633d18eb644f7a47f93a802691626/raw/did.jsonl"),
                URI.create("https://gist.githubusercontent.com/brianorwhatever/9c4633d18eb644f7a47f93a802691626/raw")
        ), DomainSrcDereferencer.candidateUrls("gist.githubusercontent.com/brianorwhatever/9c4633d18eb644f7a47f93a802691626/raw"));
    }

    @Test
    public void testValueThatIsAlreadyALog() throws Exception {
        assertEquals(List.of(URI.create("https://example.com/a/did.jsonl")), DomainSrcDereferencer.candidateUrls("example.com/a/did.jsonl"));
    }
}
