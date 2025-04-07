package uniresolver.driver.did.scid;

import foundation.identity.did.DID;
import foundation.identity.did.DIDDocument;
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
import uniresolver.driver.did.scid.srcdereferencers.DidUrlSrcDereferencer;
import uniresolver.driver.did.scid.srcdereferencers.DomainSrcDereferencer;
import uniresolver.driver.did.scid.srcdereferencers.SrcDereferencer;
import uniresolver.result.DereferenceResult;
import uniresolver.result.ResolveResult;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DidScidDriver implements Driver {

	private static final Logger log = LoggerFactory.getLogger(DidScidDriver.class);

	public static final Pattern DID_SCID_PATTERN = Pattern.compile("^did:scid:(.+):(.+):(.+)$");

	private Map<String, Object> properties;

	private ClientUniResolver clientUniResolver;
	private ClientUniDereferencer clientUniDeferencer;
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
		if (log.isDebugEnabled()) log.debug("Determined 'src' value: {}", srcValue);

		// dereference "src" value

		List<SrcDereferencer> srcDereferencers = List.of(
				new DidUrlSrcDereferencer(this.getClientUniDeferencer()),
				new DomainSrcDereferencer()
		);

        byte[] srcData = null;
		for (SrcDereferencer srcDereferencer : srcDereferencers) {
			if (! srcDereferencer.canDereference(srcValue)) continue;
			try {
				if (log.isDebugEnabled()) log.debug("Attempting to dereference 'src' value {} with dereferencer {}", srcValue, srcDereferencer.getClass().getSimpleName());
				srcData = srcDereferencer.dereference(srcValue);
				break;
			} catch (NullPointerException | IOException ex) {
				throw new ResolutionException("invalidResolutionOptions", "Cannot dereference 'src' resolution option with dereferencer " + srcDereferencer.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
			}
		}
		if (srcData == null) throw new ResolutionException("No result from dereferencing 'src' value " + srcValue);
		if (log.isInfoEnabled()) log.info("For 'src' value {} dereferenced {} bytes", srcValue, srcData.length);

        // transform to source DID

		Matcher matcher = DID_SCID_PATTERN.matcher(identifier.toString());
		if (! matcher.matches()) {
			throw new ResolutionException(ResolutionException.ERROR_INVALIDDID, "Not a valid did:scid: " + identifier);
		}
		String format = matcher.group(1);
		Integer version = Integer.parseInt(matcher.group(2));
		String scid = matcher.group(3);

		SourceMethod sourceMethod;
		DID sourceDid;

		if (WebvhSourceMethod.DID_SCID_FORMAT.equals(format) && WebvhSourceMethod.DID_SCID_VERSION.equals(version)) {
			sourceMethod = new WebvhSourceMethod();
		} else {
			throw new ResolutionException(ResolutionException.ERROR_INVALIDDID, "Unsupported did:scid format " + format + " and version " + version);
		}

		sourceDid = sourceMethod.toSourceDID(srcData);

		// prepare "src" data according to source method

		sourceMethod.prepareSrcData(srcValue, this.getWrapperFilesPath(), srcData);

		// resolve source DID

		ResolveResult resolveResult = this.getClientUniResolver().resolve(sourceDid.toString());

		// adjust source DID in DID document

		DIDDocument didDocument = resolveResult.getDidDocument();
		didDocument = DIDDocument.fromJson(didDocument.toJson().replace(sourceDid.toString(), identifier.toString()));
		resolveResult.setDidDocument(didDocument);

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
	public DereferenceResult dereference(DIDURL didurl, Map<String, Object> dereferenceOptions) throws DereferencingException, ResolutionException {
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
	public String getWrapperFilesPath() {
		return this.wrapperFilesPath;
	}

	public void setWrapperFilesPath(String wrapperFilesPath) {
		this.wrapperFilesPath = wrapperFilesPath;
	}
}
