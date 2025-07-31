# Universal Resolver Driver: did:scid

This is a [Universal Resolver](https://github.com/decentralized-identity/universal-resolver/) driver for **did:scid** identifiers.

(work in progress)

## Specifications

* [Decentralized Identifiers](https://www.w3.org/TR/did-1.0/)
* [DID Method Specification](https://lf-toip.atlassian.net/wiki/spaces/HOME/pages/88572360/DID+SCID+Method+Specification)

## Architecture

This driver depends on other drivers of did:scid "source DID methods". At the moment, these are:
- did:webvh driver [source](https://github.com/decentralized-identity/uni-resolver-driver-did-webvh) [image](https://github.com/decentralized-identity/uni-resolver-driver-did-webvh/pkgs/container/uni-resolver-driver-did-webvh)
- did:webs driver [source](https://github.com/GLEIF-IT/did-webs-resolver) [image](https://hub.docker.com/r/gleif/did-webs-resolver-service)

This driver also depends on "?src" dereferencers, which can process values of the `?src` DID parameter used by `did:scid`:
- [DidUrlSrcDereferencer](https://github.com/danubetech/uni-resolver-driver-did-scid/blob/main/src/main/java/uniresolver/driver/did/scid/srcdereferencers/DidUrlSrcDereferencer.java)
- [DomainSrcDereferencer](https://github.com/danubetech/uni-resolver-driver-did-scid/blob/main/src/main/java/uniresolver/driver/did/scid/srcdereferencers/DomainSrcDereferencer.java)
- [HederaUriDereferencer](https://github.com/danubetech/uni-resolver-driver-did-scid/blob/main/src/main/java/uniresolver/driver/did/scid/srcdereferencers/HederaUriDereferencer.java)

```
      /-------------------\      /-------------------\
      | did:webs driver   |      | did:webvh driver  |      ...
      \-------------------/      \-------------------/
                ^                          ^
                |                          |
   /--------------------------------------------------------------\
   | source DID method drivers                                    |
/====================================================================\
|  did:sicd driver                                                   |
\====================================================================/
   | "?src" dereferencers                                         |
   |--------------------------------------------------------------|
   | DID URL            | Domain Name        | Hedera URI         |
   | e.g. did:cheqd:... | e.g. exampe.com    | e.g. hedera:...    |
   \--------------------------------------------------------------/
```

## Example DIDs

```
 did:scid:vh:1:QmfGEUAcMpzo25kF2Rhn8L5FAXysfGnkzjwdKoNPi615XQ
 did:scid:TODO
 did:scid:TODO
```

## Build and Run (Docker)

```
docker build -f ./docker/Dockerfile . -t universalresolver/driver-did-scid
docker run -p 8080:8080 universalresolver/driver-did-scid
curl -X GET http://localhost:8080/1.0/identifiers/did:scid:TODO
```

## Build (native Java)

	mvn clean install
	
## Driver Environment Variables

The driver recognizes the following environment variables:

### `uniresolver_driver_did_scid_didUrlDereferencerUrl`

 * TODO