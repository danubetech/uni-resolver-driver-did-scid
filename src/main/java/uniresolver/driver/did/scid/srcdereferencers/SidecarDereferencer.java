package uniresolver.driver.did.scid.srcdereferencers;

import foundation.identity.did.DIDURL;
import foundation.identity.did.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.DereferencingException;
import uniresolver.ResolutionException;
import uniresolver.result.DereferenceResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public class SidecarDereferencer implements SrcDereferencer {

    private static final Logger log = LoggerFactory.getLogger(SidecarDereferencer.class);

    private static final Map<String, Object> SIDECAR_DATA = Map.of(
            "gist.githubusercontent.com/brianorwhatever/9c4633d18eb644f7a47f93a802691626/raw",
            """
            {"versionId":"1-QmRRaLXwc6BjBuBPosSupJwEQ8w9f3znP7yfbpGfwcnLr6","versionTime":"2025-01-23T04:12:36Z","parameters":{"method":"did:webvh:0.5","scid":"QmPEQVM1JPTyrvEgBcDXwjK4TeyLGSX1PxjgyeAisdWM1p","updateKeys":["z6MkhkBLZJSX8ZD6wLKoKVp7xbXzbVXtH3akop4cQBfS57aC"],"portable":false,"nextKeyHashes":[],"witness":{"witnesses":[],"threshold":0},"deactivated":false},"state":{"@context":["https://www.w3.org/ns/did/v1","https://w3id.org/security/multikey/v1"],"id":"did:webvh:QmPEQVM1JPTyrvEgBcDXwjK4TeyLGSX1PxjgyeAisdWM1p:gist.githubusercontent.com:brianorwhatever:9c4633d18eb644f7a47f93a802691626:raw","controller":"did:webvh:QmPEQVM1JPTyrvEgBcDXwjK4TeyLGSX1PxjgyeAisdWM1p:gist.githubusercontent.com:brianorwhatever:9c4633d18eb644f7a47f93a802691626:raw","assertionMethod":["did:webvh:QmPEQVM1JPTyrvEgBcDXwjK4TeyLGSX1PxjgyeAisdWM1p:gist.githubusercontent.com:brianorwhatever:9c4633d18eb644f7a47f93a802691626:raw#QBfS57aC"],"verificationMethod":[{"id":"did:webvh:QmPEQVM1JPTyrvEgBcDXwjK4TeyLGSX1PxjgyeAisdWM1p:gist.githubusercontent.com:brianorwhatever:9c4633d18eb644f7a47f93a802691626:raw#QBfS57aC","controller":"did:webvh:QmPEQVM1JPTyrvEgBcDXwjK4TeyLGSX1PxjgyeAisdWM1p:gist.githubusercontent.com:brianorwhatever:9c4633d18eb644f7a47f93a802691626:raw","type":"Multikey","publicKeyMultibase":"z6MkhkBLZJSX8ZD6wLKoKVp7xbXzbVXtH3akop4cQBfS57aC"}]},"proof":[{"type":"DataIntegrityProof","cryptosuite":"eddsa-jcs-2022","verificationMethod":"did:key:z6MkhkBLZJSX8ZD6wLKoKVp7xbXzbVXtH3akop4cQBfS57aC#z6MkhkBLZJSX8ZD6wLKoKVp7xbXzbVXtH3akop4cQBfS57aC","created":"2025-01-23T04:12:36Z","proofPurpose":"assertionMethod","proofValue":"z3ZQtkAjHfJ7pdLNQw3ytrkYXYFszLYXHUxfgywLKPKLsbPx9ybZgYh4vDntcmCWe9dh38KCfr8iJvZf7ab8exKPz"}]}
            """
    );

    @Override
    public boolean canDereference(String srcValue) {
        return SIDECAR_DATA.containsKey(srcValue);
    }

    @Override
    public byte[] dereference(String srcValue) throws IOException {
        return ((String) SIDECAR_DATA.get(srcValue)).getBytes(StandardCharsets.UTF_8);
    }
}
