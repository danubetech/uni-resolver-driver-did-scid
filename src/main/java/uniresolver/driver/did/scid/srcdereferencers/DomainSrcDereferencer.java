package uniresolver.driver.did.scid.srcdereferencers;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

public class DomainSrcDereferencer implements SrcDereferencer {

    private static final Logger log = LoggerFactory.getLogger(DomainSrcDereferencer.class);

    @Override
    public byte[] dereference(String srcValue) throws IOException {
        byte[] srcData;
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(URI.create("https://" + srcValue).toURL().openStream());) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(bufferedInputStream, byteArrayOutputStream);
            srcData = byteArrayOutputStream.toByteArray();
        }
        if (log.isInfoEnabled()) log.info("For 'src' value {} dereferenced {} bytes", srcValue, srcData.length);
        return srcData;
    }
}
