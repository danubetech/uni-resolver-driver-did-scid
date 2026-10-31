package uniresolver.driver.did.scid.vh1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Verifies the verifiable history of a <code>did:scid:vh:1</code> DID whose log names the
 * did:scid itself as the DID document <code>id</code> in every entry.
 *
 * <p>The log is checked by the did:webvh 1.0 rules (SCID derivation, the entry hash chain,
 * versionId and versionTime, eddsa-jcs-2022 proofs by an active update key, pre-rotation),
 * with two changes, since the DID has no host in it:</p>
 * <ol>
 *     <li>every entry's <code>state.id</code> MUST equal <code>did:scid:vh:1:{SCID}</code> exactly
 *     (no path, query or fragment), where {SCID} is <code>parameters.scid</code> of the first entry;</li>
 *     <li>there is no host check: where the log was fetched from doesn't affect its validity.</li>
 * </ol>
 *
 * <p>So the resolved DID document's <code>id</code> is the DID being resolved, as W3C DID Resolution
 * requires, whichever <code>src</code> the log came from.</p>
 *
 * <p>Logs that turn on witnessing are refused: their witness proofs (did-witness.json) aren't
 * part of what a <code>src</code> gives.</p>
 */
public class ScidVhLog {

    public static final String METHOD = "did:webvh:1.0";
    public static final String DID_PREFIX = "did:scid:vh:1:";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SCID_PLACEHOLDER = "{SCID}";
    private static final Pattern TIME = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$");
    private static final List<String> ENTRY_PROPERTIES = List.of("versionId", "versionTime", "parameters", "state", "proof");
    /** How far ahead of the resolver's clock a versionTime may be (clock skew). */
    public static final long FUTURE_ALLOWANCE_SECONDS = 60;

    /** A log that doesn't verify. */
    public static class InvalidLogException extends Exception {
        public InvalidLogException(String message) {
            super(message);
        }
    }

    /** The latest state of a verified log. */
    public static class Result {
        public final JsonNode didDocument;
        public final String versionId;
        public final String versionTime;
        public final int versionNumber;
        public final String created;
        public final boolean deactivated;
        public final List<String> updateKeys;

        Result(JsonNode didDocument, String versionId, String versionTime, int versionNumber, String created, boolean deactivated, List<String> updateKeys) {
            this.didDocument = didDocument;
            this.versionId = versionId;
            this.versionTime = versionTime;
            this.versionNumber = versionNumber;
            this.created = created;
            this.deactivated = deactivated;
            this.updateKeys = updateKeys;
        }
    }

    /** Whether the first entry of the log in <code>srcData</code> names <code>did:scid:vh:1:{scid}</code> as its document id. */
    public static boolean namesItself(byte[] srcData, String scid) {
        try {
            List<JsonNode> entries = parse(srcData);
            return (DID_PREFIX + scid).equals(entries.get(0).path("state").path("id").asText(null));
        } catch (Exception ex) {
            return false;
        }
    }

    /** The log's entries: one JSON object per line (JSON Lines); blank lines are skipped. */
    public static List<JsonNode> parse(byte[] srcData) throws IOException, InvalidLogException {
        List<JsonNode> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(new String(srcData, StandardCharsets.UTF_8)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                entries.add(objectMapper.readTree(line));
            }
        }
        if (entries.isEmpty()) throw new InvalidLogException("the log is empty");
        return entries;
    }

    /** Verify the log in <code>srcData</code> as the log of <code>did:scid:vh:1:{scid}</code>, at <code>now</code>. */
    public static Result verify(byte[] srcData, String scid, Instant now) throws IOException, InvalidLogException {
        return verify(parse(srcData), scid, now);
    }

    public static Result verify(List<JsonNode> entries, String expectedScid, Instant now) throws InvalidLogException {
        if (entries.isEmpty()) throw new InvalidLogException("the log is empty");
        String did = DID_PREFIX + expectedScid;
        ObjectNode params = objectMapper.createObjectNode();
        List<String> prevNextHashes = Collections.emptyList();
        List<String> prevUpdateKeys = null;
        String prevVersionId = null;
        Instant prevTime = null;
        String created = null;
        JsonNode doc = null;
        String versionId = null;
        String versionTime = null;
        List<String> updateKeys = null;

        for (int i = 0; i < entries.size(); i++) {
            int n = i + 1;
            JsonNode entry = entries.get(i);
            if (entry == null || !entry.isObject()) throw bad(n, "not an object");
            for (Iterator<String> it = entry.fieldNames(); it.hasNext(); ) {
                String k = it.next();
                if (!ENTRY_PROPERTIES.contains(k)) throw bad(n, "unexpected property " + k);
            }
            JsonNode vid = entry.get("versionId");
            JsonNode vtime = entry.get("versionTime");
            JsonNode update = entry.get("parameters");
            JsonNode state = entry.get("state");
            JsonNode proofs = entry.get("proof");
            if (vid == null || !vid.isTextual()) throw bad(n, "missing versionId");
            if (vtime == null || !vtime.isTextual()) throw bad(n, "missing versionTime");
            if (update == null || !update.isObject()) throw bad(n, "missing parameters");
            if (state == null || !state.isObject()) throw bad(n, "missing state");
            if (proofs != null && proofs.isObject()) proofs = objectMapper.createArrayNode().add(proofs);
            if (proofs == null || !proofs.isArray() || proofs.isEmpty()) throw bad(n, "no proof");
            versionId = vid.textValue();
            versionTime = vtime.textValue();

            if (i == 0) {
                if (!METHOD.equals(update.path("method").asText(null))) throw bad(n, "method must be " + METHOD);
            } else {
                if (update.has("scid")) throw bad(n, "scid may only be set in the first entry");
                if (update.path("portable").asBoolean(false)) throw bad(n, "portable may only be turned on in the first entry");
                if (update.has("method") && !update.get("method").equals(params.get("method"))) throw bad(n, "unsupported method " + update.get("method"));
            }
            params.setAll((ObjectNode) update);
            if (witnessOn(params.get("witness"))) throw bad(n, "witnessed logs aren't supported: their witness proofs (did-witness.json) aren't part of the 'src' data");

            List<String> keys = strings(params, "updateKeys", n);
            String scid = params.path("scid").asText(null);
            if (scid == null) throw bad(n, "missing scid");
            if (!scid.equals(expectedScid)) throw bad(n, "the log's SCID is " + scid + ", not " + expectedScid);

            ObjectNode unsigned = ((ObjectNode) entry).deepCopy();
            unsigned.remove("proof");

            if (i == 0) {
                ObjectNode genesis = objectMapper.createObjectNode();
                genesis.put("versionId", SCID_PLACEHOLDER);
                genesis.set("versionTime", vtime);
                ObjectNode genesisParams = ((ObjectNode) update).deepCopy();
                genesisParams.put("scid", SCID_PLACEHOLDER);
                genesis.set("parameters", genesisParams);
                genesis.set("state", replaceAll(state, scid, SCID_PLACEHOLDER));
                if (!multihash(jcs(genesis).getBytes(StandardCharsets.UTF_8)).equals(scid)) throw bad(n, "the SCID doesn't match the first entry");
                if (keys.isEmpty()) throw bad(n, "the first entry needs updateKeys");
                created = versionTime;
            }

            int dash = versionId.indexOf('-');
            if (dash < 0) throw bad(n, "bad versionId " + versionId);
            if (!versionId.substring(0, dash).equals(String.valueOf(n))) throw bad(n, "versionId " + versionId + " should start with " + n + "-");
            ObjectNode chained = unsigned.deepCopy();
            chained.put("versionId", prevVersionId != null ? prevVersionId : scid);
            if (!multihash(jcs(chained).getBytes(StandardCharsets.UTF_8)).equals(versionId.substring(dash + 1))) throw bad(n, "the hash chain is broken");

            Instant t = parseTime(versionTime, n);
            if (prevTime != null && !t.isAfter(prevTime)) throw bad(n, "versionTime must be later than the previous entry's");
            if (now != null && t.isAfter(now.plusSeconds(FUTURE_ALLOWANCE_SECONDS))) throw bad(n, "versionTime is in the future");

            String id = state.path("id").asText(null);
            if (!did.equals(id)) throw bad(n, "document id " + id + " must be " + did);

            List<String> authorized;
            if (prevUpdateKeys != null && prevNextHashes.isEmpty()) {
                authorized = prevUpdateKeys;
            } else {
                if (!prevNextHashes.isEmpty()) {
                    JsonNode uk = update.get("updateKeys");
                    if (uk == null || !uk.isArray()) throw bad(n, "pre-rotation needs updateKeys in every entry");
                    for (JsonNode k : uk) {
                        if (!prevNextHashes.contains(multihash(k.asText("").getBytes(StandardCharsets.UTF_8)))) throw bad(n, "key " + k.asText() + " wasn't committed to by nextKeyHashes");
                    }
                }
                authorized = keys;
            }
            for (JsonNode proof : proofs) {
                String signer;
                try {
                    signer = verifyEntryProof(unsigned, proof);
                } catch (GeneralSecurityException | IllegalArgumentException ex) {
                    throw bad(n, ex.getMessage());
                }
                if (!authorized.contains(signer)) throw bad(n, "signed by " + signer + ", which isn't an update key");
            }

            prevNextHashes = strings(params, "nextKeyHashes", n);
            prevUpdateKeys = keys;
            prevVersionId = versionId;
            prevTime = t;
            doc = state;
            updateKeys = keys;
        }
        boolean deactivated = params.path("deactivated").asBoolean(false);
        return new Result(doc, versionId, versionTime, entries.size(), created, deactivated, updateKeys);
    }

    /*
     * eddsa-jcs-2022 (VC Data Integrity EdDSA Cryptosuites v1.0)
     */

    /** Verify one proof on a log entry: the signer's did:key, as its multikey. */
    static String verifyEntryProof(JsonNode unsigned, JsonNode proof) throws GeneralSecurityException {
        if (proof == null || !proof.isObject()) throw new IllegalArgumentException("a proof must be an object");
        if (!"DataIntegrityProof".equals(proof.path("type").asText(null))) throw new IllegalArgumentException("unsupported proof type");
        if (!"eddsa-jcs-2022".equals(proof.path("cryptosuite").asText(null))) throw new IllegalArgumentException("unsupported cryptosuite " + proof.path("cryptosuite").asText());
        if (!"assertionMethod".equals(proof.path("proofPurpose").asText(null))) throw new IllegalArgumentException("proofPurpose must be assertionMethod");
        String vm = proof.path("verificationMethod").asText("");
        int hash = vm.indexOf('#');
        if (hash < 0) throw new IllegalArgumentException("verificationMethod needs a fragment: " + vm);
        String controller = vm.substring(0, hash);
        String fragment = vm.substring(hash + 1);
        if (!controller.startsWith("did:key:")) throw new IllegalArgumentException("verificationMethod must be a did:key: " + vm);
        String mk = controller.substring("did:key:".length());
        if (!mk.equals(fragment)) throw new IllegalArgumentException("verificationMethod fragment doesn't match its key: " + vm);
        byte[] publicKey = parseMultikey(mk);
        String proofValue = proof.path("proofValue").asText("");
        if (!proofValue.startsWith("z")) throw new IllegalArgumentException("proofValue must be base58btc");
        byte[] signature = b58decode(proofValue.substring(1));
        ObjectNode options = ((ObjectNode) proof).deepCopy();
        options.remove("proofValue");
        byte[] data = concat(sha256(jcs(options).getBytes(StandardCharsets.UTF_8)), sha256(jcs(unsigned).getBytes(StandardCharsets.UTF_8)));
        if (signature.length != 64 || !ed25519Verify(publicKey, data, signature)) throw new IllegalArgumentException("signature doesn't verify");
        return mk;
    }

    /** The raw Ed25519 key in a Multikey (z6Mk…). */
    static byte[] parseMultikey(String mk) {
        if (mk == null || !mk.startsWith("z")) throw new IllegalArgumentException("not a multikey: " + mk);
        byte[] b = b58decode(mk.substring(1));
        if (b.length != 34 || (b[0] & 0xff) != 0xed || b[1] != 0x01) throw new IllegalArgumentException("not an Ed25519 multikey: " + mk);
        return Arrays.copyOfRange(b, 2, 34);
    }

    private static final byte[] ED25519_SPKI_PREFIX = { 0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00 };

    static boolean ed25519Verify(byte[] publicKey, byte[] data, byte[] signature) throws GeneralSecurityException {
        PublicKey key = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(concat(ED25519_SPKI_PREFIX, publicKey)));
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(key);
        verifier.update(data);
        try {
            return verifier.verify(signature);
        } catch (SignatureException ex) {
            // A malformed signature (not a valid encoding) doesn't verify either.
            return false;
        }
    }

    /*
     * Hashes and encodings
     */

    static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** A sha2-256 multihash, base58btc (without the multibase 'z'): how did:webvh writes SCIDs and entry hashes. */
    static String multihash(byte[] data) {
        return b58encode(concat(new byte[] { 0x12, 0x20 }, sha256(data)));
    }

    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger FIFTY_EIGHT = BigInteger.valueOf(58);

    static String b58encode(byte[] bytes) {
        BigInteger n = new BigInteger(1, bytes);
        StringBuilder sb = new StringBuilder();
        while (n.signum() > 0) {
            BigInteger[] qr = n.divideAndRemainder(FIFTY_EIGHT);
            sb.append(ALPHABET.charAt(qr[1].intValue()));
            n = qr[0];
        }
        for (int i = 0; i < bytes.length && bytes[i] == 0; i++) sb.append('1');
        return sb.reverse().toString();
    }

    static byte[] b58decode(String s) {
        BigInteger n = BigInteger.ZERO;
        for (int i = 0; i < s.length(); i++) {
            int v = ALPHABET.indexOf(s.charAt(i));
            if (v < 0) throw new IllegalArgumentException("not base58: " + s.charAt(i));
            n = n.multiply(FIFTY_EIGHT).add(BigInteger.valueOf(v));
        }
        byte[] raw = n.toByteArray();
        if (raw.length > 1 && raw[0] == 0) raw = Arrays.copyOfRange(raw, 1, raw.length);
        if (n.signum() == 0) raw = new byte[0];
        int zeros = 0;
        while (zeros < s.length() && s.charAt(zeros) == '1') zeros++;
        return concat(new byte[zeros], raw);
    }

    static byte[] concat(byte[] a, byte[] b) {
        byte[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /*
     * JSON Canonicalization Scheme (RFC 8785)
     */

    static String jcs(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        writeJcs(node, sb);
        return sb.toString();
    }

    private static void writeJcs(JsonNode node, StringBuilder sb) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            sb.append("null");
        } else if (node.isBoolean()) {
            sb.append(node.booleanValue());
        } else if (node.isTextual()) {
            writeString(node.textValue(), sb);
        } else if (node.isNumber()) {
            sb.append(number(node));
        } else if (node.isArray()) {
            sb.append('[');
            for (int i = 0; i < node.size(); i++) {
                if (i > 0) sb.append(',');
                writeJcs(node.get(i), sb);
            }
            sb.append(']');
        } else if (node.isObject()) {
            // Keys sorted by their UTF-16 code units, which is String's natural order.
            TreeMap<String, JsonNode> sorted = new TreeMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                sorted.put(e.getKey(), e.getValue());
            }
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, JsonNode> e : sorted.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(e.getKey(), sb);
                sb.append(':');
                writeJcs(e.getValue(), sb);
            }
            sb.append('}');
        } else {
            throw new IllegalArgumentException("not JSON: " + node.getNodeType());
        }
    }

    /** A string as ECMAScript's JSON.stringify writes it. */
    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    boolean loneSurrogate = Character.isSurrogate(c) && !(Character.isHighSurrogate(c) && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) && !(Character.isLowSurrogate(c) && i > 0 && Character.isHighSurrogate(s.charAt(i - 1)));
                    if (c < 0x20 || loneSurrogate) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    /** A number as ECMAScript's Number.prototype.toString writes it. */
    static String number(JsonNode node) {
        double d = node.isIntegralNumber() ? node.bigIntegerValue().doubleValue() : node.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) throw new IllegalArgumentException("not a JSON number: " + node);
        if (d == 0) return "0";
        String sign = d < 0 ? "-" : "";
        BigDecimal bd = new BigDecimal(Double.toString(Math.abs(d))).stripTrailingZeros();
        String digits = bd.unscaledValue().toString();
        int k = digits.length();
        int e = k - bd.scale(); // the decimal point is after this many digits
        String out;
        if (k <= e && e <= 21) {
            out = digits + "0".repeat(e - k);
        } else if (0 < e && e <= 21) {
            out = digits.substring(0, e) + "." + digits.substring(e);
        } else if (-6 < e && e <= 0) {
            out = "0." + "0".repeat(-e) + digits;
        } else {
            int x = e - 1;
            String exp = (x < 0 ? "-" : "+") + Math.abs(x);
            out = (k == 1 ? digits : digits.charAt(0) + "." + digits.substring(1)) + "e" + exp;
        }
        return sign + out;
    }

    /*
     * Helpers
     */

    private static JsonNode replaceAll(JsonNode node, String from, String to) {
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(node).replace(from, to));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static boolean witnessOn(JsonNode witness) {
        return witness != null && witness.isObject() && witness.path("threshold").asInt(0) > 0;
    }

    private static List<String> strings(JsonNode params, String key, int n) throws InvalidLogException {
        JsonNode v = params.get(key);
        if (v == null || v.isNull()) return Collections.emptyList();
        if (!v.isArray()) throw bad(n, key + " must be a list of strings");
        List<String> out = new ArrayList<>();
        for (JsonNode x : v) {
            if (!x.isTextual()) throw bad(n, key + " must be a list of strings");
            out.add(x.textValue());
        }
        return out;
    }

    private static Instant parseTime(String t, int n) throws InvalidLogException {
        if (!TIME.matcher(t).matches()) throw bad(n, "bad versionTime " + t);
        try {
            return Instant.parse(t);
        } catch (DateTimeParseException ex) {
            throw bad(n, "bad versionTime " + t);
        }
    }

    private static InvalidLogException bad(int n, String message) {
        return new InvalidLogException("log entry " + n + ": " + message);
    }
}
