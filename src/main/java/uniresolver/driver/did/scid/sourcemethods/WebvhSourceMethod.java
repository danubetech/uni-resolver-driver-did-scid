package uniresolver.driver.did.scid.sourcemethods;

import foundation.identity.did.DID;
import foundation.identity.did.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;

public class WebvhSourceMethod implements SourceMethod {

    public static final String DID_SCID_FORMAT = "vh";
    public static final Integer DID_SCID_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(WebvhSourceMethod.class);

    @Override
    public DID toSourceDID(DID identifier, String httpUrl) {
        String scid = identifier.getMethodSpecificId().substring(identifier.getMethodSpecificId().lastIndexOf(":") + 1);
        DID sourceDid = null;
        try {
            sourceDid = DID.fromString("did:webvh:" + scid + ":" + toMethodSpecificIdentifierPart(httpUrl));
        } catch (ParserException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        if (log.isInfoEnabled()) log.info("For identifier {} and httpUrl {}: {} and {}", identifier, httpUrl, scid, sourceDid);
        return sourceDid;
    }

    @Override
    public void prepareSrcData(byte[] srcData, String wrapperFilesPath) {
        String path = wrapperFilesPath;
        if (! path.endsWith("/")) path += "/";
        try (FileOutputStream fileOutputStream = new FileOutputStream(path)) {
            fileOutputStream.write(srcData);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot write file " + wrapperFilesPath + ": " + ex.getMessage(), ex);
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
        return methodSpecificIdentifierPart;
    }
}
