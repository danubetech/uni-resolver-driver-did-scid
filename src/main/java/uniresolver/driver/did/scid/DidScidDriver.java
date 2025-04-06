package uniresolver.driver.did.scid;

import foundation.identity.did.DID;
import foundation.identity.did.DIDURL;
import foundation.identity.did.representations.Representations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.DereferencingException;
import uniresolver.ResolutionException;
import uniresolver.client.ClientUniDereferencer;
import uniresolver.client.ClientUniResolver;
import uniresolver.driver.Driver;
import uniresolver.driver.did.scid.config.Configuration;
import uniresolver.driver.did.scid.sourcemethods.SourceMethod;
import uniresolver.driver.did.scid.sourcemethods.WebvhSourceMethod;
import uniresolver.driver.did.scid.srcdereferencers.DomainSrcDereferencer;
import uniresolver.driver.did.scid.srcdereferencers.SrcDereferencer;
import uniresolver.result.DereferenceResult;
import uniresolver.result.ResolveResult;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DidScidDriver implements Driver {

	private static final Logger log = LoggerFactory.getLogger(DidScidDriver.class);

	public static final Pattern DID_SCID_PATTERN = Pattern.compile("^did:scid:(.+):(.+):(.+)$");

	private Map<String, Object> properties;

	private ClientUniResolver clientUniResolver;
	private ClientUniDereferencer clientUniDeferencer;
	private String wrapperHttpUrl;
	private String wrapperFilesPath;

	public DidScidDriver() {
		this(Configuration.getPropertiesFromEnvironment());
	}

	public DidScidDriver(Map<String, Object> properties) {
		this.setProperties(properties);
	}

	@Override
	public ResolveResult resolve(DID identifier, Map<String, Object> resolutionOptions) throws ResolutionException {

		// parse resolution options

		String srcValue = (String) resolutionOptions.get("src");
		if (srcValue == null) throw new ResolutionException("invalidResolutionOptions", "Missing 'src' property");

		// dereference "src" value

		SrcDereferencer srcDereferencer = new DomainSrcDereferencer();

        byte[] srcData;
        try {
            srcData = srcDereferencer.dereference(srcValue);
        } catch (IOException ex) {
            throw new ResolutionException("invalidResolutionOptions", "Cannot dereference 'src' resolution option ': " + ex.getMessage(), ex);
        }

        // transform to source method

		Matcher matcher = DID_SCID_PATTERN.matcher(identifier.toString());
		String format = matcher.group(1);
		Integer version = Integer.parseInt(matcher.group(2));

		SourceMethod sourceMethod;
		DID sourceDid;

		if (WebvhSourceMethod.DID_SCID_FORMAT.equals(format) && WebvhSourceMethod.DID_SCID_VERSION.equals(version)) {
			sourceMethod = new WebvhSourceMethod();
		} else {
			throw new ResolutionException(ResolutionException.ERROR_INVALIDDID, "Unsupported did:scid format " + format + " and version " + version);
		}

		sourceDid = sourceMethod.toSourceDID(identifier, this.getWrapperHttpUrl());

		// prepare "src" data according to source method

		sourceMethod.prepareSrcData(srcData, this.getWrapperFilesPath());

		// resolve source DID

		ResolveResult resolveResult = this.getClientUniResolver().resolve(sourceDid.toString());

		// DID RESOLUTION METADATA

		Map<String, Object> didResolutionMetadata = new LinkedHashMap<>();
		didResolutionMetadata.put("contentType", Representations.DEFAULT_MEDIA_TYPE);
		resolveResult.getDidResolutionMetadata().putAll(didResolutionMetadata);

		// DID DOCUMENT METADATA

		Map<String, Object> didDocumentMetadata = new LinkedHashMap<>();
		didDocumentMetadata.put("srcValue", srcValue);
		didDocumentMetadata.put("sourceDid", sourceDid);
		resolveResult.getDidDocumentMetadata().putAll(didDocumentMetadata);

		// done

		return resolveResult;
	}

	@Override
	public DereferenceResult dereference(DIDURL didurl, Map<String, Object> map) throws DereferencingException, ResolutionException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Map<String, Object> properties() {
		return this.getProperties();
	}

	/*
	 * Getters and setters
	 */

	public Map<String, Object> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
		Configuration.configureFromProperties(this, properties);
	}

	public ClientUniResolver getClientUniResolver() {
		return this.clientUniResolver;
	}

	public void setClientUniResolver(ClientUniResolver clientUniResolver) {
		this.clientUniResolver = clientUniResolver;
	}

	public ClientUniDereferencer getClientUniDeferencer() {
		return this.clientUniDeferencer;
	}

	public void setClientUniDeferencer(ClientUniDereferencer clientUniDeferencer) {
		this.clientUniDeferencer = clientUniDeferencer;
	}

	public String getWrapperHttpUrl() {
		return this.wrapperHttpUrl;
	}

	public void setWrapperHttpUrl(String wrapperHttpUrl) {
		this.wrapperHttpUrl = wrapperHttpUrl;
	}

	public String getWrapperFilesPath() {
		return this.wrapperFilesPath;
	}

	public void setWrapperFilesPath(String wrapperFilesPath) {
		this.wrapperFilesPath = wrapperFilesPath;
	}
}
