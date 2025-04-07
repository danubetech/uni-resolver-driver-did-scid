package uniresolver.driver.did.scid.srcdereferencers;

import java.io.IOException;

public interface SrcDereferencer {

    boolean canDereference(String srcValue);
    byte[] dereference(String srcValue) throws IOException;
}
