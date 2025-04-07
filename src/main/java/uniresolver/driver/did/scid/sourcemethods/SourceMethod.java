package uniresolver.driver.did.scid.sourcemethods;

import foundation.identity.did.DID;

public interface SourceMethod {

    DID toSourceDID(byte[] srcData);
    void prepareSrcData(String srcValue, String wrapperFilesPath, byte[] srcData);
}
