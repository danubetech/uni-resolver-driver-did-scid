package uniresolver.driver.did.scid.srcdereferencers;

import java.io.IOException;

public interface SrcDereferencer {

    byte[] dereference(String srcValue) throws IOException;
}
