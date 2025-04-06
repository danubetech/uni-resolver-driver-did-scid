package uniresolver.driver.did.scid.sourcemethods;

import foundation.identity.did.DID;

public interface SourceMethod {

    void prepareSrcData(String srcValue, String wrapperFilesPath, byte[] srcData);
    DID toSourceDID(byte[] srcData);
}
