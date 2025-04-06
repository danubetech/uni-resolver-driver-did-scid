# Universal Resolver Driver: did:scid

This is a [Universal Resolver](https://github.com/decentralized-identity/universal-resolver/) driver for **did:scid** identifiers.

(work in progress)

## Specifications

* [Decentralized Identifiers](https://www.w3.org/TR/did-1.0/)
* [DID Method Specification](https://lf-toip.atlassian.net/wiki/spaces/HOME/pages/88572360/DID+SCID+Method+Specification)

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