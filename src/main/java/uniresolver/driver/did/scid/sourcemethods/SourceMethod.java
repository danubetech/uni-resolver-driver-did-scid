package uniresolver.driver.did.scid.sourcemethods;

import foundation.identity.did.DID;

import java.util.Map;

public interface SourceMethod {

    DID toSourceDid(byte[] srcData, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata);
    void prepareSrcData(DID sourceDid, String wrapperFilesPath, byte[] srcData, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata);
}
