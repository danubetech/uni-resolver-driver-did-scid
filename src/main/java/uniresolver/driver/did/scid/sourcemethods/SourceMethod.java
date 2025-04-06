package uniresolver.driver.did.scid.sourcemethods;

import foundation.identity.did.DID;

public interface SourceMethod {

    void prepareSrcData(byte[] srcData, String wrapperFilesPath);
    DID toSourceDID(DID identifier, String httpUrl);
}
