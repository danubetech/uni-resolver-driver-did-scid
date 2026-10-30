package uniresolver.driver.did.scid.srcdereferencers;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Dereferences a 'src' value that is a URL: relative to <code>https://</code>, such as
 * <code>example.com/a/b</code>, or absolute, such as <code>https://example.com/a/b</code>, when its
 * scheme is one of the allowed ones (only <code>https</code> unless configured otherwise).
 *
 * <p>Per the DID SCID Resolution Overview, <code>?src=host/a/b</code> names the source DID
 * <code>did:webvh:{SCID}:host:a:b</code>, so the verifiable history is where did:webvh's DID-to-HTTPS
 * transformation puts it: <code>https://host/a/b/did.jsonl</code>, or
 * <code>https://host/.well-known/did.jsonl</code> when the value has no path. That URL is tried
 * first. The value itself, as a URL, is tried next, so values that already point at the data
 * (a raw gist URL, or a did:webs 'src' document) keep working. An absolute URL is treated the
 * same way, with its own scheme in place of <code>https://</code>.</p>
 */
public class DomainSrcDereferencer implements SrcDereferencer {

    private static final Logger log = LoggerFactory.getLogger(DomainSrcDereferencer.class);

    /** JSON Lines first (a verifiable history), then JSON (other 'src' data). */
    public static final String ACCEPT = "text/jsonl, application/jsonl;q=0.9, application/json;q=0.8, */*;q=0.1";

    /** The schemes an absolute 'src' URL may have, unless configured otherwise. */
    public static final Set<String> DEFAULT_SCHEMES = Set.of("https");

    /** A value that starts with a scheme and "://": an absolute URL (a host with a port, example.com:8443/a, isn't one). */
    private static final Pattern ABSOLUTE = Pattern.compile("^([A-Za-z][A-Za-z0-9+.-]*)://(.*)$", Pattern.DOTALL);

    private final Set<String> schemes;

    public DomainSrcDereferencer() {
        this(DEFAULT_SCHEMES);
    }

    /** @param schemes the schemes an absolute 'src' URL may have, such as "https" (lower case, without ":") */
    public DomainSrcDereferencer(Set<String> schemes) {
        this.schemes = schemes.stream().map(DomainSrcDereferencer::normalizeScheme).collect(Collectors.toUnmodifiableSet());
    }

    /** Schemes from a comma-separated list such as "https, http:": lower case, without ":"; the default when there are none. */
    public static Set<String> parseSchemes(String value) {
        if (value == null) return DEFAULT_SCHEMES;
        Set<String> schemes = Arrays.stream(value.split(",")).map(DomainSrcDereferencer::normalizeScheme).filter(x -> ! x.isEmpty()).collect(Collectors.toUnmodifiableSet());
        return schemes.isEmpty() ? DEFAULT_SCHEMES : schemes;
    }

    private static String normalizeScheme(String scheme) {
        String s = scheme.trim().toLowerCase(Locale.ROOT);
        return s.endsWith(":") ? s.substring(0, s.length() - 1) : s;
    }

    public Set<String> getSchemes() {
        return this.schemes;
    }

    @Override
    public boolean canDereference(String srcValue) {
        return true;
    }

    /** The URLs to try for <code>srcValue</code>, with absolute URLs allowed only for the default schemes (https). */
    public static List<URI> candidateUrls(String srcValue) throws URISyntaxException {
        return candidateUrls(srcValue, DEFAULT_SCHEMES);
    }

    /**
     * The URLs to try for <code>srcValue</code>, in order: the verifiable history's location
     * (<code>{src}/did.jsonl</code>, or <code>{host}/.well-known/did.jsonl</code> without a path),
     * then <code>srcValue</code> itself. Only the latter for a value that already names a
     * <code>.jsonl</code> file. A relative value is relative to <code>https://</code>; an absolute
     * one keeps its scheme, which must be one of <code>schemes</code>.
     */
    public static List<URI> candidateUrls(String srcValue, Set<String> schemes) throws URISyntaxException {
        String base = "https://";
        String rest = srcValue;
        Matcher absolute = ABSOLUTE.matcher(srcValue);
        if (absolute.matches()) {
            String scheme = absolute.group(1).toLowerCase(Locale.ROOT);
            if (! schemes.contains(scheme)) throw new URISyntaxException(srcValue, "the 'src' URL's scheme " + scheme + " isn't allowed (allowed: " + String.join(", ", schemes) + ")");
            base = scheme + "://";
            rest = absolute.group(2);
        }
        List<URI> urls = new ArrayList<>();
        String value = rest;
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        if (! value.endsWith(".jsonl")) {
            String location = value.indexOf('/') < 0 ? value + "/.well-known" : value;
            urls.add(new URI(base + location + "/did.jsonl"));
        }
        urls.add(new URI(base + rest));
        return urls;
    }

    @Override
    public byte[] dereference(String srcValue, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) throws IOException, URISyntaxException {

        List<String> problems = new ArrayList<>();

        for (URI uri : candidateUrls(srcValue, this.schemes)) {

            URL url = uri.toURL();
            if (log.isDebugEnabled()) log.debug("Dereferencing URI: {}", url);

            byte[] srcData;
            try {
                srcData = fetch(url);
            } catch (IOException ex) {
                if (log.isDebugEnabled()) log.debug("Cannot dereference URI {}: {}", url, ex.getMessage());
                problems.add(url + ": " + ex.getMessage());
                continue;
            }

            didResolutionMetadata.put("src.url", url);
            return srcData;
        }

        throw new IOException("Nothing found at " + String.join("; ", problems));
    }

    private static byte[] fetch(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Accept", ACCEPT);
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int status = httpConnection.getResponseCode();
            if (status < 200 || status >= 300) throw new IOException("HTTP " + status);
        }
        try (InputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
    }
}
