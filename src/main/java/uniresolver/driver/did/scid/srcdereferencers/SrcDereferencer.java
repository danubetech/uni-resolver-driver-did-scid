package uniresolver.driver.did.scid.srcdereferencers;

import java.util.Map;

public interface SrcDereferencer {

    boolean canDereference(String srcValue);
    byte[] dereference(String srcValue, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) throws Exception;
}
