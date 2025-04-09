package uniresolver.driver.did.scid;

import uniresolver.driver.did.scid.srcdereferencers.HederaUriDereferencer;

import java.util.Collections;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println(new HederaUriDereferencer().canDereference("hedera:testnet:0.0.5815674"));
        System.out.println(new HederaUriDereferencer().dereference("hedera:testnet:0.0.5815674", new HashMap(), new HashMap()));
    }
}
