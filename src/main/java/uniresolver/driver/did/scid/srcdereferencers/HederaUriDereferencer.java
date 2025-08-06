package uniresolver.driver.did.scid.srcdereferencers;

import com.hedera.hashgraph.sdk.*;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
    public byte[] dereference(String srcValue, Map<String, Object> didResolutionMetadata, Map<String, Object> didDocumentMetadata) {

        Matcher matcher = HEDERA_URI_PATTERN.matcher(srcValue);
        if (! matcher.matches()) return null;
        String hederaNetwork = matcher.group(1);
        String hederaTopicId = matcher.group(2);
        if (log.isDebugEnabled()) log.debug("Dereferencing Hedera URI: {} and {}", hederaNetwork, hederaTopicId);

        Client client = Client.forName(hederaNetwork);
        try {
            client.setMirrorNetwork(Collections.singletonList(hederaNetwork + ".mirrornode.hedera.com:443"));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        TopicId topicId = TopicId.fromString(hederaTopicId);
        TopicMessageQuery topicMessageQuery = new TopicMessageQuery();
        topicMessageQuery.setStartTime(Instant.EPOCH);
        topicMessageQuery.setEndTime(Instant.now());
        topicMessageQuery.setTopicId(topicId);

        CompletableFuture<TopicMessage> topicMessageCompletableFuture = new CompletableFuture<>();
        StringBuffer stringBuffer = new StringBuffer();
        SubscriptionHandle subscriptionHandle = topicMessageQuery.subscribe(client, topicMessage -> {
            if (log.isDebugEnabled()) log.debug("Topic message received for " + hederaTopicId + ": " + topicMessage);
            stringBuffer.append(new String(topicMessage.contents, StandardCharsets.UTF_8));
            if (stringBuffer.toString().endsWith("}")) {
                topicMessageCompletableFuture.complete(topicMessage);
            }
        });

        TopicMessage topicMessage;
        try {
            topicMessage = topicMessageCompletableFuture.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        subscriptionHandle.unsubscribe();

        didResolutionMetadata.put("src.hedera.network", hederaNetwork);
        didResolutionMetadata.put("src.hedera.topicId", hederaTopicId);
        didResolutionMetadata.put("src.hedera.transactionId", topicMessage.transactionId.toString());
        didResolutionMetadata.put("src.hedera.sequenceNumber", topicMessage.sequenceNumber);
        didResolutionMetadata.put("src.hedera.consensusTimestamp", topicMessage.consensusTimestamp.toString());
        didResolutionMetadata.put("src.hedera.runningHash", Hex.encodeHexString(topicMessage.runningHash));
        didResolutionMetadata.put("src.hedera.chunks.length", Arrays.asList(topicMessage.chunks).size());
        return stringBuffer.toString().getBytes(StandardCharsets.UTF_8);
    }
}
