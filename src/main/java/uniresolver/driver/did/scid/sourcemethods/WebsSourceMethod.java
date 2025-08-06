package uniresolver.driver.did.scid.sourcemethods;

import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.identity.did.DID;
import foundation.identity.did.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.UniResolver;

import java.io.File;
import java.io.FileWriter;
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
            sourceDid = DID.fromString("did:webs:src-web-server:" + scid);
        } catch (ParserException ex) {
            throw new RuntimeException(ex);
        }
        if (log.isInfoEnabled()) log.info("For 'srcData' {}: {}", srcData.length, sourceDid);
        return sourceDid;
    }

    @Override
    public void prepareSrcData(DID sourceDid, String wrapperFilesPath, byte[] srcData, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) throws IOException {

        String basePath = wrapperFilesPath;
        if (! basePath.endsWith("/")) basePath += "/";

        String didDomainAndPath = sourceDid.getMethodSpecificId().replace(":", "/");
        if (log.isDebugEnabled()) log.debug("For 'sourceDid' {}: didDomainAndPath {}", sourceDid, didDomainAndPath);
        String didPath = didDomainAndPath.substring(didDomainAndPath.indexOf("/") + 1);
        if (log.isDebugEnabled()) log.debug("For 'sourceDid' {}: didPath {}", sourceDid, didPath);
        if (! didPath.endsWith("/")) didPath += "/";

        File path = new File(basePath + didPath);
        if (log.isDebugEnabled()) log.debug("For 'sourceDid' {}: path {}", sourceDid, path);
        boolean mkdir = path.mkdirs();
        if (log.isDebugEnabled()) log.debug("For 'sourceDid' {}: mkdir {}", sourceDid, mkdir);

        Map<String, Object> srcMap = objectMapper.readValue(srcData, Map.class);
        Map<String, Object> srcDidJsonMap = (Map<String, Object>) srcMap.computeIfAbsent("did.json", x -> { throw new IllegalArgumentException("No \"did.json\" property found in 'src' data."); });
        String srcDataDidJsonValue = objectMapper.writeValueAsString(srcDidJsonMap);
        srcDataDidJsonValue = srcDataDidJsonValue.replace((String) srcDidJsonMap.get("id"), sourceDid.toString());

        String srcDataKeriCesrValue = (String) srcMap.computeIfAbsent("keri.cesr", x -> { throw new IllegalArgumentException("No \"keri.cesr\" property found in 'src' data."); });

        File fileDidJson = new File(path, "/did.json");
        if (log.isDebugEnabled()) log.debug("For 'sourceDid' {}: fileDidJson {}", sourceDid, fileDidJson);
        try (FileWriter fileWriter = new FileWriter(fileDidJson)) {
            fileWriter.write(srcDataDidJsonValue);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot write fileDidJson " + fileDidJson + ": " + ex.getMessage(), ex);
        }

        File fileKeriCesr = new File(path, "/keri.cesr");
        if (log.isDebugEnabled()) log.debug("For 'sourceDid' {}: fileKeriCesr {}", sourceDid, fileKeriCesr);
        try (FileWriter fileWriter = new FileWriter(fileKeriCesr)) {
            fileWriter.write(srcDataKeriCesrValue);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot write fileKeriCesr " + fileKeriCesr + ": " + ex.getMessage(), ex);
        }

        didResolutionMetadata.put("src.path", path);
    }
}
