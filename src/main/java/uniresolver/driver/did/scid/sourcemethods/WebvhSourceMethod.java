package uniresolver.driver.did.scid.sourcemethods;

import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.identity.did.DID;
import foundation.identity.did.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.UniResolver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class WebvhSourceMethod extends SourceMethod {

    public static final String DID_SCID_FORMAT = "vh";
    public static final Integer DID_SCID_VERSION = 1;
    public static final String SOURCE_METHOD_NAME = "webvh";

    private static final Logger log = LoggerFactory.getLogger(WebvhSourceMethod.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public WebvhSourceMethod(UniResolver uniResolver) {
        super(uniResolver, SOURCE_METHOD_NAME);
    }

    @Override
    public DID toSourceDid(String scid, byte[] srcData, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) throws ParserException {
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
        if (! id.startsWith("did:webvh:" + scid)) throw new IllegalArgumentException("No 'scid' " + scid + " found in 'id': " + id);
        DID sourceDid = DID.fromString(id);
        if (log.isInfoEnabled()) log.info("For 'srcData' {}: {}", srcData.length, sourceDid);
        return sourceDid;
    }

    @Override
    public void prepareSrcData(DID sourceDid, String wrapperFilesPath, byte[] srcData, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) {
        String pathString = wrapperFilesPath;
        String didDomainAndPathString = sourceDid.getMethodSpecificId().substring(sourceDid.getMethodSpecificId().indexOf(":") + 1);
        String didPathString = didDomainAndPathString.substring(didDomainAndPathString.indexOf(":") + 1);
        if (log.isDebugEnabled()) log.debug("For 'sourceDid' {}: didPathString {}", sourceDid, didPathString);
        String sourceDidPathString = didPathString.replace(":", "/");
        if (log.isDebugEnabled()) log.debug("For 'sourceDid' {}: sourceDidPathString {}", sourceDid, sourceDidPathString);
        if (! pathString.endsWith("/")) pathString += "/";
        if (! sourceDidPathString.endsWith("/")) sourceDidPathString += "/";
        File path = new File(pathString + sourceDidPathString);
        if (log.isDebugEnabled()) log.debug("For 'sourceDid' {}: path {}", sourceDid, path);
        didResolutionMetadata.put("srcData.path", path);
        boolean mkdir = path.mkdirs();
        if (log.isDebugEnabled()) log.debug("For 'sourceDid' {}: mkdir {}", sourceDid, mkdir);
        File file = new File(path, "/did.jsonl");
        if (log.isDebugEnabled()) log.debug("For 'sourceDid' {}: file {}", sourceDid, file);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(srcData);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot write file " + file + ": " + ex.getMessage(), ex);
        }
    }
}
