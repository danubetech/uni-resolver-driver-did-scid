package uniresolver.driver.did.scid;

import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.identity.did.DID;
import foundation.identity.did.DIDDocument;
import foundation.identity.did.DIDURL;
import foundation.identity.did.representations.Representations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.DereferencingException;
import uniresolver.ResolutionException;
import uniresolver.driver.Driver;
import uniresolver.driver.did.scid.config.Configuration;
import uniresolver.driver.did.scid.sourcemethods.SourceMethod;
import uniresolver.driver.did.scid.srcdereferencers.SrcDereferencer;
import uniresolver.driver.did.scid.sourcemethods.WebvhSourceMethod;
import uniresolver.driver.did.scid.vh1.ScidVhLog;
import uniresolver.result.DereferenceResult;
import uniresolver.result.ResolveResult;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DidScidDriver implements Driver {

	private static final Logger log = LoggerFactory.getLogger(DidScidDriver.class);

	public static final Pattern DID_SCID_PATTERN = Pattern.compile("^did:scid:(.+):(.+):(.+)$");

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private Map<String, Object> properties;

	private String wrapperFilesPath;

    private Map<String, Map<Integer, SourceMethod>> sourceMethods;
    private List<SrcDereferencer> srcDereferencers;

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
		if (srcValue == null) throw new ResolutionException(ResolutionException.ERROR_INVALID_OPTIONS, "Missing 'src' resolution option");
		if (log.isDebugEnabled()) log.debug("Determined 'src' value: {}", srcValue);

		// prepare metadata

		Map<String, Object> didResolutionMetadata = new LinkedHashMap<>();
		Map<String, Object> didDocumentMetadata = new LinkedHashMap<>();

		// dereference "src" value

        SrcDereferencer srcDereferencer = this.getSrcDereferencers().stream().filter(x -> x.canDereference(srcValue)).findFirst().orElse(null);
        if (srcDereferencer == null) throw new ResolutionException("No 'src' dereferencer for " + srcValue);

        byte[] srcData;
        try {
            if (log.isDebugEnabled()) log.debug("Attempting to dereference 'src' value {} with dereferencer {}", srcValue, srcDereferencer.getClass().getSimpleName());
            srcData = srcDereferencer.dereference(srcValue, didResolutionMetadata, didDocumentMetadata);
        } catch (Exception ex) {
            throw new ResolutionException(ResolutionException.ERROR_INVALID_OPTIONS, "Cannot dereference 'src' resolution option " + srcValue + " with dereferencer " + srcDereferencer.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
        }
		if (srcData == null) throw new ResolutionException("No result from dereferencing 'src' value using 'src' dereferencer " + srcDereferencer.getClass().getSimpleName() + ": " + srcValue);
		if (log.isInfoEnabled()) log.info("For 'src' value {} dereferenced {} bytes", srcValue, srcData.length);

        // parse did:scid

		Matcher matcher = DID_SCID_PATTERN.matcher(identifier.toString());
		if (! matcher.matches()) {
			throw new ResolutionException(ResolutionException.ERROR_INVALID_DID, "Not a valid did:scid: " + identifier);
		}

		String format = matcher.group(1);
		Integer version = Integer.parseInt(matcher.group(2));
		String scid = matcher.group(3);

		// a vh:1 log that names the did:scid itself in every entry: verified here, since there's no source DID to resolve

		if (WebvhSourceMethod.DID_SCID_FORMAT.equals(format) && WebvhSourceMethod.DID_SCID_VERSION.equals(version) && ScidVhLog.namesItself(srcData, scid)) {
			didResolutionMetadata.put("srcDereferencer", srcDereferencer.getClass().getSimpleName());
			didResolutionMetadata.put("srcValue", srcValue);
			return this.resolveScidVhLog(identifier, format, version, scid, srcData, didResolutionMetadata, didDocumentMetadata);
		}

        // transform to source DID

        SourceMethod sourceMethod = this.getSourceMethods().getOrDefault(format, Collections.emptyMap()).get(version);
		if (sourceMethod == null) throw new ResolutionException(ResolutionException.ERROR_INVALID_DID, "Unsupported did:scid format " + format + " and version " + version);

        DID sourceDid;
        try {
            sourceDid = sourceMethod.toSourceDid(scid, srcData, didResolutionMetadata, didDocumentMetadata);
        } catch (Exception ex) {
            throw new ResolutionException(ResolutionException.ERROR_INVALID_OPTIONS, "Cannot determine source DID " + srcValue + " with source method " + sourceMethod.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
        }
        if (! sourceMethod.getSourceMethodName().equals(sourceDid.getMethodName())) throw new ResolutionException(ResolutionException.ERROR_INVALID_OPTIONS, "Unexpected source DID method" + sourceDid.getMethodName() + " (expected: " + sourceMethod.getSourceMethodName() + ")");

		// prepare "src" data according to source method

        try {
            sourceMethod.prepareSrcData(sourceDid, this.getWrapperFilesPath(), srcData, didResolutionMetadata, didDocumentMetadata);
        } catch (Exception ex) {
            throw new ResolutionException(ResolutionException.ERROR_INVALID_OPTIONS, "Cannot prepare 'src' data for source DID " + sourceDid + " with source method " + sourceMethod.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
        }

        // resolve source DID

		ResolveResult resolveResult = sourceMethod.getUniResolver().resolve(sourceDid.toString());

		// adjust source DID in DID document

		DIDDocument didDocument = resolveResult.getDidDocument();
		didDocument = DIDDocument.fromJson(didDocument.toJson().replace(sourceDid.toString(), identifier.toString()));
		resolveResult.setDidDocument(didDocument);

		// DID RESOLUTION METADATA

		didResolutionMetadata.put("contentType", Representations.DEFAULT_MEDIA_TYPE);
        didResolutionMetadata.put("format", format);
        didResolutionMetadata.put("version", version);
        didResolutionMetadata.put("scid", scid);
        didResolutionMetadata.put("sourceMethod", sourceMethod.getClass().getSimpleName());
        didResolutionMetadata.put("srcDereferencer", srcDereferencer.getClass().getSimpleName());
		didResolutionMetadata.put("srcValue", srcValue);
		didResolutionMetadata.put("sourceDid", sourceDid);
		didResolutionMetadata.put("srcData.length", srcData.length);
		resolveResult.getDidResolutionMetadata().putAll(didResolutionMetadata);

		// DID DOCUMENT METADATA

		resolveResult.getDidDocumentMetadata().putAll(didDocumentMetadata);

		// done

		return resolveResult;
	}

	/**
	 * Resolve a did:scid:vh:1 from a log whose entries name the did:scid itself as the DID document id
	 * (ScidVhLog): the did:webvh 1.0 rules, except that every id is exactly did:scid:vh:1:{SCID} and there
	 * is no host check. The resolved DID document's id is the DID being resolved, whichever 'src' the log came from.
	 */
	private ResolveResult resolveScidVhLog(DID identifier, String format, Integer version, String scid, byte[] srcData, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) throws ResolutionException {

		ScidVhLog.Result result;
		try {
			result = ScidVhLog.verify(srcData, scid, Instant.now());
		} catch (ScidVhLog.InvalidLogException ex) {
			throw new ResolutionException(ResolutionException.ERROR_INVALID_DID_DOCUMENT, "Invalid verifiable history for " + identifier + ": " + ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new ResolutionException(ResolutionException.ERROR_INVALID_OPTIONS, "Cannot read 'src' data as a verifiable history (JSON Lines): " + ex.getMessage(), ex);
		}
		if (log.isInfoEnabled()) log.info("Verified the log of {}: {} entries, now at {}", identifier, result.versionNumber, result.versionId);

		DIDDocument didDocument;
		try {
			didDocument = DIDDocument.fromJson(objectMapper.writeValueAsString(result.didDocument));
		} catch (IOException ex) {
			throw new ResolutionException(ResolutionException.ERROR_INVALID_DID_DOCUMENT, "Cannot read the DID document of " + identifier + ": " + ex.getMessage(), ex);
		}

		// DID RESOLUTION METADATA

		didResolutionMetadata.put("contentType", Representations.DEFAULT_MEDIA_TYPE);
		didResolutionMetadata.put("format", format);
		didResolutionMetadata.put("version", version);
		didResolutionMetadata.put("scid", scid);
		didResolutionMetadata.put("sourceMethod", ScidVhLog.class.getSimpleName());
		didResolutionMetadata.put("srcData.length", srcData.length);

		// DID DOCUMENT METADATA

		didDocumentMetadata.put("created", result.created);
		didDocumentMetadata.put("updated", result.versionTime);
		didDocumentMetadata.put("versionId", result.versionId);
		didDocumentMetadata.put("versionTime", result.versionTime);
		if (result.deactivated) didDocumentMetadata.put("deactivated", Boolean.TRUE);

		return ResolveResult.build(didResolutionMetadata, didDocument, didDocumentMetadata);
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
	public String getWrapperFilesPath() {
		return this.wrapperFilesPath;
	}

	public void setWrapperFilesPath(String wrapperFilesPath) {
		this.wrapperFilesPath = wrapperFilesPath;
	}

    public Map<String, Map<Integer, SourceMethod>> getSourceMethods() {
        return this.sourceMethods;
    }

    public void setSourceMethods(Map<String, Map<Integer, SourceMethod>> sourceMethods) {
        this.sourceMethods = sourceMethods;
    }

    public List<SrcDereferencer> getSrcDereferencers() {
        return this.srcDereferencers;
    }

    public void setSrcDereferencers(List<SrcDereferencer> srcDereferencers) {
        this.srcDereferencers = srcDereferencers;
    }
}
