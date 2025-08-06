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

## Build and Run (Docker)

```
docker compose build
docker compose up
```

## Example DIDs and DID URLs

```
curl -X GET http://localhost:8080/1.0/identifiers/did:webvh:QmPEQVM1JPTyrvEgBcDXwjK4TeyLGSX1PxjgyeAisdWM1p:gist.githubusercontent.com:brianorwhatever:9c4633d18eb644f7a47f93a802691626:raw
curl -X GET http://localhost:8080/1.0/identifiers/did:scid:vh:1:QmPEQVM1JPTyrvEgBcDXwjK4TeyLGSX1PxjgyeAisdWM1p?src=gist.githubusercontent.com%2Fbrianorwhatever%2F9c4633d18eb644f7a47f93a802691626%2Fraw
curl -X GET http://localhost:8080/1.0/identifiers/did:scid:vh:1:QmPEQVM1JPTyrvEgBcDXwjK4TeyLGSX1PxjgyeAisdWM1p?src=gist.githubusercontent.com%2Fpeacekeeper%2F2a0e3e9b87819a38555273a3f2c5bd2c%2Fraw
curl -X GET http://localhost:8080/1.0/identifiers/did:scid:vh:1:QmPEQVM1JPTyrvEgBcDXwjK4TeyLGSX1PxjgyeAisdWM1p?src=did:cheqd:testnet:3ba8fcbb-45c8-4b20-b5a2-b4155199a1ac/resources/4c3f5d83-03f0-466f-8455-4f8d2c760ef0
curl -X GET http://localhost:8080/1.0/identifiers/did:scid:vh:1:QmPEQVM1JPTyrvEgBcDXwjK4TeyLGSX1PxjgyeAisdWM1p?src=hedera:testnet:0.0.5815515
```

```
curl -X GET http://localhost:8080/1.0/identifiers/did:webs:peacekeeper.github.io:did-webs-iiw37-tutorial:EKYGGh-FtAphGmSZbsuBs_t4qpsjYJ2ZqvMKluq9OxmP
curl -X GET http://localhost:8080/1.0/identifiers/did:scid:ke:1:EKYGGh-FtAphGmSZbsuBs_t4qpsjYJ2ZqvMKluq9OxmP?src=gist.githubusercontent.com%2Fpeacekeeper%2F01fa6c0a2c00c161b9a56f1d094b081f%2Fraw
curl -X GET http://localhost:8080/1.0/identifiers/did:scid:ke:1:EKYGGh-FtAphGmSZbsuBs_t4qpsjYJ2ZqvMKluq9OxmP?src=did:cheqd:testnet:3ba8fcbb-45c8-4b20-b5a2-b4155199a1ac/resources/4c3f5d83-03f0-466f-8455-4f8d2c760ef0
curl -X GET http://localhost:8080/1.0/identifiers/did:scid:ke:1:EKYGGh-FtAphGmSZbsuBs_t4qpsjYJ2ZqvMKluq9OxmP?src=hedera:testnet:0.0.5815515
```

## Build (native Java)

	mvn clean install
	
## Driver Environment Variables

The driver recognizes the following environment variables:

### `uniresolver_driver_did_scid_didResolverUri`

 * TODO

## Additional (theoretical) examples

```
did:scid:vh:1:QmfGEUAcMpzo25kF2Rhn8L5FAXysfGnkzjwdKoNPi615XQ?src=example.com
did:scid:vh:1:QmfGEUAcMpzo25kF2Rhn8L5FAXysfGnkzjwdKoNPi615XQ?src=newlocation.com
did:scid:vh:1:QmfGEUAcMpzo25kF2Rhn8L5FAXysfGnkzjwdKoNPi615XQ?src=bluesky.com
did:scid:ke:1:Ew-o5dU5WjDrxDBK4b4HrF82_rYb6MX6xsegjq4n0Y7M?src=example.com
```
