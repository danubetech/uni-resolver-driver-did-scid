package uniresolver.driver.did.scid.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.client.ClientUniDereferencer;
import uniresolver.client.ClientUniResolver;
import uniresolver.driver.did.scid.DidScidDriver;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class Configuration {

    private static final Logger log = LoggerFactory.getLogger(Configuration.class);

    public static Map<String, Object> getPropertiesFromEnvironment() {

        if (log.isDebugEnabled()) log.debug("Loading from environment: " + System.getenv());

        Map<String, Object> properties = new HashMap<>();

        try {

            String env_didResolverUri = System.getenv("uniresolver_driver_did_scid_didResolverUri");
            String env_wrapperHttpUrl = System.getenv("uniresolver_driver_did_scid_wrapperHttpUrl");
            String env_wrapperFilesPath = System.getenv("uniresolver_driver_did_scid_wrapperFilesPath");

            if (env_didResolverUri != null) properties.put("didResolverUri", env_didResolverUri);
            if (env_wrapperHttpUrl != null) properties.put("wrapperHttpUrl", env_wrapperHttpUrl);
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

            // parse wrapperHttpUrl

            String prop_wrapperHttpUrl = (String) properties.get("wrapperHttpUrl");

            // parse wrapperFilesPath

            String prop_wrapperFilesPath = (String) properties.get("wrapperFilesPath");

            // configure

            didScidDriver.setClientUniDeferencer(clientUniDereferencer);
            didScidDriver.setWrapperHttpUrl(prop_wrapperHttpUrl);
            didScidDriver.setWrapperFilesPath(prop_wrapperFilesPath);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }
}
