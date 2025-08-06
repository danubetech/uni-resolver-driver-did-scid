package uniresolver.driver.did.scid.srcdereferencers;

import foundation.identity.did.DIDURL;
import foundation.identity.did.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.DereferencingException;
import uniresolver.ResolutionException;
import uniresolver.result.DereferenceResult;
import uniresolver.w3c.DIDURLDereferencer;

import java.io.IOException;
import java.util.Map;

public class DidUrlSrcDereferencer implements SrcDereferencer {

    private static final Logger log = LoggerFactory.getLogger(DidUrlSrcDereferencer.class);

    private DIDURLDereferencer didUrlDereferencer;

    public DidUrlSrcDereferencer(DIDURLDereferencer didurlDereferencer) {
        this.didUrlDereferencer = didurlDereferencer;
    }

    @Override
    public boolean canDereference(String srcValue) {
        try {
            DIDURL.fromString(srcValue);
            return true;
        } catch (ParserException ignored) {
            return false;
        }
    }

    @Override
    public byte[] dereference(String srcValue, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) throws IOException {

        byte[] srcData;
        if (log.isDebugEnabled()) log.debug("Dereferencing DID URL: {}", srcValue);

        try {
            DereferenceResult dereferenceResult = this.getDidUrlDereferencer().dereference(srcValue, Map.of("accept", "*/*"));
            srcData = dereferenceResult.getContent();
        } catch (ResolutionException | DereferencingException ex) {
            throw new IOException(ex.getMessage(), ex);
        }

        return srcData;
    }

    public DIDURLDereferencer getDidUrlDereferencer() {
        return this.didUrlDereferencer;
    }

    public void setDidUrlDereferencer(DIDURLDereferencer didUrlDereferencer) {
        this.didUrlDereferencer = didUrlDereferencer;
    }
}
