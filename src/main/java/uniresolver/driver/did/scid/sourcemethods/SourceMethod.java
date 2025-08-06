package uniresolver.driver.did.scid.sourcemethods;

import foundation.identity.did.DID;
import uniresolver.UniResolver;

import java.util.Map;

public abstract class SourceMethod {

    private final UniResolver uniResolver;
    private final String sourceMethodName;

    public SourceMethod(UniResolver uniResolver, String sourceMethodName) {
        this.uniResolver = uniResolver;
        this.sourceMethodName = sourceMethodName;
    }

    public UniResolver getUniResolver() {
        return this.uniResolver;
    }

    public String getSourceMethodName() {
        return this.sourceMethodName;
    }

    public abstract DID toSourceDid(String scid, byte[] srcData, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) throws Exception;
    public abstract void prepareSrcData(DID sourceDid, String wrapperFilesPath, byte[] srcData, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) throws Exception;
}
