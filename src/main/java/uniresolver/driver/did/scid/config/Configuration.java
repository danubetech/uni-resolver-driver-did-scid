package uniresolver.driver.did.scid.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.client.ClientUniDereferencer;
import uniresolver.client.ClientUniResolver;
import uniresolver.driver.did.scid.DidScidDriver;
import uniresolver.driver.did.scid.sourcemethods.SourceMethod;
import uniresolver.driver.did.scid.sourcemethods.WebsSourceMethod;
import uniresolver.driver.did.scid.sourcemethods.WebvhSourceMethod;
import uniresolver.driver.did.scid.srcdereferencers.DidUrlSrcDereferencer;
import uniresolver.driver.did.scid.srcdereferencers.DomainSrcDereferencer;
import uniresolver.driver.did.scid.srcdereferencers.HederaUriDereferencer;
import uniresolver.driver.did.scid.srcdereferencers.SrcDereferencer;
import uniresolver.driver.http.HttpDriver;
import uniresolver.local.LocalUniResolver;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {

    private static final Logger log = LoggerFactory.getLogger(Configuration.class);

    public static Map<String, Object> getPropertiesFromEnvironment() {

        if (log.isDebugEnabled()) log.debug("Loading from environment: " + System.getenv());

        Map<String, Object> properties = new HashMap<>();

        try {

            String env_didResolverUri = System.getenv("uniresolver_driver_did_scid_didResolverUri");
            String env_didWebvhDriverUri = System.getenv("uniresolver_driver_did_scid_didWebvhDriverUri");
            String env_didWebsDriverUri = System.getenv("uniresolver_driver_did_scid_didWebsDriverUri");
            String env_wrapperFilesPath = System.getenv("uniresolver_driver_did_scid_wrapperFilesPath");

            if (env_didResolverUri != null) properties.put("didResolverUri", env_didResolverUri);
            if (env_didWebvhDriverUri != null) properties.put("didWebvhDriverUri", env_didWebvhDriverUri);
            if (env_didWebsDriverUri != null) properties.put("didWebsDriverUri", env_didWebsDriverUri);
            if (env_wrapperFilesPath != null) properties.put("wrapperFilesPath", env_wrapperFilesPath);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }

        return properties;
    }

    public static void configureFromProperties(DidScidDriver didScidDriver, Map<String, Object> properties) {

        if (log.isDebugEnabled()) log.debug("Configuring from properties: " + properties);

        try {

            // parse didResolverUri

            String prop_didResolverUri = (String) properties.get("didResolverUri");

            ClientUniResolver clientUniResolver = ClientUniResolver.create(URI.create(prop_didResolverUri));
            ClientUniDereferencer clientUniDereferencer = ClientUniDereferencer.create(URI.create(prop_didResolverUri));

            // parse didWebvhDriverUri

            String prop_didWebvhDriverUri = (String) properties.get("didWebvhDriverUri");

            HttpDriver didWebvhDriver = new HttpDriver();
            didWebvhDriver.setPattern("^(did:webvh:.+)$");
            didWebvhDriver.setResolveUri(URI.create(prop_didWebvhDriverUri));

            // parse didWebsDriverUri

            String prop_didWebsDriverUri = (String) properties.get("didWebsDriverUri");

            HttpDriver didWebsDriver = new HttpDriver();
            didWebsDriver.setPattern("^(did:webs:.+)$");
            didWebsDriver.setResolveUri(URI.create(prop_didWebsDriverUri));
            didWebsDriver.setAcceptHeaderValue("");

            // parse wrapperFilesPath

            String prop_wrapperFilesPath = (String) properties.get("wrapperFilesPath");

            // configure

            Map<String, Map<Integer, SourceMethod>> sourceMethods = Map.of(
                    WebvhSourceMethod.DID_SCID_FORMAT, Map.of(
                            WebvhSourceMethod.DID_SCID_VERSION, new WebvhSourceMethod(new LocalUniResolver(Collections.singletonList(didWebvhDriver)))
                    ),
                    WebsSourceMethod.DID_SCID_FORMAT, Map.of(
                            WebsSourceMethod.DID_SCID_VERSION, new WebsSourceMethod(new LocalUniResolver(Collections.singletonList(didWebsDriver)))
                    )
            );

            List<SrcDereferencer> srcDereferencers = List.of(
                    new DidUrlSrcDereferencer(clientUniDereferencer),
                    new HederaUriDereferencer(),
                    new DomainSrcDereferencer()
            );

            didScidDriver.setSourceMethods(sourceMethods);
            didScidDriver.setSrcDereferencers(srcDereferencers);
            didScidDriver.setWrapperFilesPath(prop_wrapperFilesPath);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }
}
