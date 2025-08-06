package uniresolver.driver.did.scid.srcdereferencers;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.driver.did.scid.sourcemethods.WebvhSourceMethod;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

public class DomainSrcDereferencer implements SrcDereferencer {

    private static final Logger log = LoggerFactory.getLogger(DomainSrcDereferencer.class);

    @Override
    public boolean canDereference(String srcValue) {
        return true;
    }

    @Override
    public byte[] dereference(String srcValue, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) throws IOException, URISyntaxException {

        byte[] srcData;

        URL url = new URI("https://" + srcValue).toURL();

        if (log.isDebugEnabled()) log.debug("Dereferencing URI: {}", url);
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(url.openStream());) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(bufferedInputStream, byteArrayOutputStream);
            srcData = byteArrayOutputStream.toByteArray();
        }

        didResolutionMetadata.put("src.url", url);
        return srcData;
    }
}
