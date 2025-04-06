package uniresolver.driver.did.scid.sourcemethods;

import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.identity.did.DID;
import foundation.identity.did.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class WebvhSourceMethod implements SourceMethod {

    public static final String DID_SCID_FORMAT = "vh";
    public static final Integer DID_SCID_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(WebvhSourceMethod.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DID toSourceDID(byte[] srcData) {
        Map<String, Object> initialLineMap;
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(srcData)) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream, StandardCharsets.UTF_8)) {
                try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                    initialLineMap = objectMapper.readValue(bufferedReader.readLine(), Map.class);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(new RuntimeException());
        }
        Map<String, Object> stateMap = initialLineMap == null ? null : (Map<String, Object>) initialLineMap.get("state");
        String id = stateMap == null ? null : (String) stateMap.get("id");
        if (id == null) throw new IllegalArgumentException("No 'id' found in initial line: " + initialLineMap);
        DID sourceDid;
        try {
            sourceDid = DID.fromString(id);
        } catch (ParserException ex) {
            throw new RuntimeException(ex);
        }
        if (log.isInfoEnabled()) log.info("For 'srcData' {}: {}", srcData.length, sourceDid);
        return sourceDid;
    }

    @Override
    public void prepareSrcData(String srcValue, String wrapperFilesPath, byte[] srcData) {
        String pathString = wrapperFilesPath;
        String srcValuePathString = srcValue.contains("/") ? srcValue.substring(srcValue.indexOf("/") + 1) : "";
        if (! pathString.endsWith("/")) pathString += "/";
        if (! srcValuePathString.endsWith("/")) srcValuePathString += "/";
        File path = new File(pathString + srcValuePathString);
        if (log.isDebugEnabled()) log.info("For 'srcValue' {}: path {}", srcValue, path);
        boolean mkdir = path.mkdirs();
        if (log.isDebugEnabled()) log.info("For 'srcValue' {}: mkdir {}", srcValue, mkdir);
        File file = new File(path, "/did.jsonld");
        if (log.isDebugEnabled()) log.info("For 'srcValue' {}: file {}", srcValue, file);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(srcData);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot write file " + file + ": " + ex.getMessage(), ex);
        }
    }

    /*
     * Helper methods
     */

    private static String toMethodSpecificIdentifierPart(String httpUrl) {
        String methodSpecificIdentifierPart = httpUrl;
        if (methodSpecificIdentifierPart.startsWith("http")) methodSpecificIdentifierPart = methodSpecificIdentifierPart.substring("http".length());
        if (methodSpecificIdentifierPart.startsWith("https")) methodSpecificIdentifierPart = methodSpecificIdentifierPart.substring("https".length());
        if (methodSpecificIdentifierPart.startsWith("/")) methodSpecificIdentifierPart = methodSpecificIdentifierPart.substring(1);
        if (methodSpecificIdentifierPart.endsWith("/")) methodSpecificIdentifierPart = methodSpecificIdentifierPart.substring(0, methodSpecificIdentifierPart.length() - 1);
        methodSpecificIdentifierPart = methodSpecificIdentifierPart.replaceAll("/", ":");
        if (log.isInfoEnabled()) log.info("For httpUrl {}: {}", httpUrl, methodSpecificIdentifierPart);
        return methodSpecificIdentifierPart;
    }
}
