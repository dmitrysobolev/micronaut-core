package org.particleframework.configuration.jackson.parser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.particleframework.core.async.processor.SingleThreadedBufferingProcessor;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A Reactive streams publisher that publishes a {@link JsonNode} once the JSON has been fully consumed.
 * Uses {@link com.fasterxml.jackson.core.json.async.NonBlockingJsonParser} internally allowing the parsing of JSON from an
 * incoming stream of bytes in a non-blocking manner
 *
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class JacksonProcessor extends SingleThreadedBufferingProcessor<byte[], JsonNode> {

    private final NonBlockingJsonParser nonBlockingJsonParser;
    private final ConcurrentLinkedDeque<JsonNode> nodeStack = new ConcurrentLinkedDeque<>();
    private String currentFieldName;

    public JacksonProcessor(JsonFactory jsonFactory)  {
        try {
            this.nonBlockingJsonParser = (NonBlockingJsonParser) jsonFactory.createNonBlockingByteArrayParser();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create non-blocking JSON parser: " + e.getMessage(), e);
        }
    }

    public JacksonProcessor() {
        this(new JsonFactory());
    }


    /**
     * @return Whether more input is needed
     */
    public boolean needMoreInput() {
        return nonBlockingJsonParser.getNonBlockingInputFeeder().needMoreInput();
    }

    @Override
    protected void doOnComplete() {
        if(needMoreInput()) {
            doOnError(new JsonEOFException(nonBlockingJsonParser, JsonToken.NOT_AVAILABLE, "Unexpected end-of-input"));
        }
        else {
            super.doOnComplete();
        }
    }

    @Override
    protected void onMessage(byte[] message) {
        try {
            ByteArrayFeeder byteFeeder = nonBlockingJsonParser.getNonBlockingInputFeeder();
            boolean consumed = false;
            while (!consumed && byteFeeder.needMoreInput()) {
                if (byteFeeder.needMoreInput()) {
                    byteFeeder.feedInput(message, 0, message.length);
                    consumed = true;
                }

                JsonToken event;
                while ((event = nonBlockingJsonParser.nextToken()) != JsonToken.NOT_AVAILABLE) {
                    JsonNode root = asJsonNode(event);
                    if (root != null) {
                        byteFeeder.endOfInput();
                        resolveSubscriber()
                                .ifPresent(subscriber ->
                                        subscriber.onNext(root)
                                );
                        break;
                    }
                }
                if(needMoreInput()) {
                    parentSubscription.request(1);
                }
            }
        } catch (IOException e) {
            onError(e);
        }
    }



    /**
     * @return The root node when the whole tree is built.
     **/
    private JsonNode asJsonNode(JsonToken event) throws IOException {
        switch (event) {
            case START_OBJECT:
                nodeStack.push(node(nodeStack.peekFirst()));
                break;

            case START_ARRAY:
                nodeStack.push(array(nodeStack.peekFirst()));
                break;

            case END_OBJECT:
            case END_ARRAY:
                assert !nodeStack.isEmpty();
                JsonNode current = nodeStack.pop();
                if (nodeStack.isEmpty())
                    return current;
                else
                    return null;

            case FIELD_NAME:
                assert !nodeStack.isEmpty();
                currentFieldName = nonBlockingJsonParser.getCurrentName();
                break;

            case VALUE_NUMBER_INT:
                assert !nodeStack.isEmpty();
                JsonNode intNode = nodeStack.peekFirst();
                if (intNode instanceof ObjectNode) {
                    ((ObjectNode)intNode).put(currentFieldName, nonBlockingJsonParser.getLongValue());
                }
                else {
                    ((ArrayNode)intNode).add(nonBlockingJsonParser.getLongValue());
                }
                break;

            case VALUE_STRING:
                assert !nodeStack.isEmpty();
                JsonNode stringNode = nodeStack.peekFirst();
                if (stringNode instanceof ObjectNode) {
                    ((ObjectNode)stringNode).put(currentFieldName, nonBlockingJsonParser.getValueAsString());
                }
                else {
                    ((ArrayNode)stringNode).add(nonBlockingJsonParser.getValueAsString());
                }
                break;

            case VALUE_NUMBER_FLOAT:
                assert !nodeStack.isEmpty();
                JsonNode floatNode = nodeStack.peekFirst();
                if (floatNode instanceof ObjectNode) {
                    ((ObjectNode)floatNode).put(currentFieldName, nonBlockingJsonParser.getFloatValue());
                }
                else {
                    ((ArrayNode)floatNode).add(nonBlockingJsonParser.getFloatValue());
                }
                break;
            case VALUE_NULL:
                assert !nodeStack.isEmpty();
                JsonNode nullNode = nodeStack.peekFirst();
                if (nullNode instanceof ObjectNode) {
                    ((ObjectNode)nullNode).putNull(currentFieldName);
                }
                else {
                    ((ArrayNode)nullNode).addNull();
                }
                break;

            case VALUE_TRUE:
            case VALUE_FALSE:
                assert !nodeStack.isEmpty();
                JsonNode booleanNode = nodeStack.peekFirst();
                if (booleanNode instanceof ObjectNode) {
                    ((ObjectNode)booleanNode).put(currentFieldName, nonBlockingJsonParser.getBooleanValue());
                }
                else {
                    ((ArrayNode)booleanNode).add(nonBlockingJsonParser.getBooleanValue());
                }
                break;

            default:
                throw new IllegalStateException("Unsupported JSON event: " + event);
        }

        return null;
    }

    private JsonNode array(JsonNode node) {
        if (node instanceof ObjectNode)
            return ((ObjectNode)node).putArray(currentFieldName);
        else if (node instanceof ArrayNode)
            return ((ArrayNode)node).addArray();
        else
            return JsonNodeFactory.instance.arrayNode();
    }

    private JsonNode node(JsonNode node) {
        if (node instanceof ObjectNode)
            return ((ObjectNode)node).putObject(currentFieldName);
        else if (node instanceof ArrayNode)
            return ((ArrayNode)node).addObject();
        else
            return JsonNodeFactory.instance.objectNode();
    }

}
