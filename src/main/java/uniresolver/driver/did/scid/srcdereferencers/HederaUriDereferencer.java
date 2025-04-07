package uniresolver.driver.did.scid.srcdereferencers;

import com.hedera.hashgraph.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HederaUriDereferencer implements SrcDereferencer {

    private static final Logger log = LoggerFactory.getLogger(HederaUriDereferencer.class);

    public static final Pattern HEDERA_URI_PATTERN = Pattern.compile("^hedera:(.+):(.+)$");

    @Override
    public boolean canDereference(String srcValue) {
        return HEDERA_URI_PATTERN.matcher(srcValue).matches();
    }

    @Override
    public byte[] dereference(String srcValue) throws IOException {

        Matcher matcher = HEDERA_URI_PATTERN.matcher(srcValue);
        String hederaNetwork = matcher.group(1);
        String hederaTopicId = matcher.group(2);
        if (log.isDebugEnabled()) log.debug("Dereferencing Hedera URI: {} and {}", hederaNetwork, hederaTopicId);

        Client testnetClient = Client.forName(hederaNetwork);
        TopicId topicId = TopicId.fromString(hederaTopicId);
        TopicMessageQuery topicMessageQuery = new TopicMessageQuery();
        topicMessageQuery.setTopicId(topicId);
        topicMessageQuery.setLimit(1);

        AtomicReference<TopicMessage> topicMessageReference = new AtomicReference<>();

        SubscriptionHandle subscriptionHandle = topicMessageQuery.subscribe(testnetClient, topicMessage -> {
            if (log.isDebugEnabled()) log.debug("Topic message received for " + hederaTopicId + ": " + topicMessage);
            topicMessageReference.set(topicMessage);
            topicMessageQuery.notify();
        });

        try {
            topicMessageQuery.wait();
        } catch (InterruptedException ex) {
            throw new RuntimeException("Cannot query topic message " + topicId + ": " + ex.getMessage(), ex);
        } finally {
            subscriptionHandle.unsubscribe();
        }

        return topicMessageReference.get().contents;
    }
}
