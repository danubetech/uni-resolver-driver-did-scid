package uniresolver.driver.did.scid.sourcemethods;

import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.identity.did.DID;
import foundation.identity.did.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.UniResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class WebsSourceMethod extends SourceMethod {

    public static final String DID_SCID_FORMAT = "ke";
    public static final Integer DID_SCID_VERSION = 1;
    public static final String SOURCE_METHOD_NAME = "webs";

    private static final Logger log = LoggerFactory.getLogger(WebsSourceMethod.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public WebsSourceMethod(UniResolver uniResolver) {
        super(uniResolver, SOURCE_METHOD_NAME);
    }

    @Override
    public DID toSourceDid(String scid, byte[] srcData, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) {
        DID sourceDid;
        try {
            sourceDid = DID.fromString("did:webs:dummy.com:" + scid);
        } catch (ParserException ex) {
            throw new RuntimeException(ex);
        }
        if (log.isInfoEnabled()) log.info("For 'srcData' {}: {}", srcData.length, sourceDid);
        return sourceDid;
    }

    @Override
    public void prepareSrcData(DID sourceDid, String wrapperFilesPath, byte[] srcData, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) {
        String pathString = wrapperFilesPath;
        String didDomainAndPathString = sourceDid.getMethodSpecificId().substring(0, sourceDid.getMethodSpecificId().indexOf(":") + 1);
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
        File file = new File(path, "/did.cesr");
        if (log.isDebugEnabled()) log.debug("For 'sourceDid' {}: file {}", sourceDid, file);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(srcData);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot write file " + file + ": " + ex.getMessage(), ex);
        }
    }
}
