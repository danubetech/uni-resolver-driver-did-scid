package uniresolver.driver.did.scid.srcdereferencers;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.driver.did.scid.sourcemethods.WebvhSourceMethod;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

public class DomainSrcDereferencer implements SrcDereferencer {

    private static final Logger log = LoggerFactory.getLogger(DomainSrcDereferencer.class);

    @Override
    public boolean canDereference(String srcValue) {
        return true;
    }

    @Override
    public byte[] dereference(String srcValue) throws IOException {
        byte[] srcData;
        URI uri = URI.create("https://" + srcValue);
        if (log.isDebugEnabled()) log.debug("Dereferencing URI: {}", uri);
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(uri.toURL().openStream());) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(bufferedInputStream, byteArrayOutputStream);
            srcData = byteArrayOutputStream.toByteArray();
        }
        return srcData;
    }
}
