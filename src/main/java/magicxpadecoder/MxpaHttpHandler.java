package magicxpadecoder;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.core.ToolSource;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import java.util.HashMap;
import java.util.Map;

import static burp.api.montoya.http.handler.RequestToBeSentAction.continueWith;
import static burp.api.montoya.http.handler.ResponseReceivedAction.continueWith;
import static burp.api.montoya.http.message.params.HttpParameter.urlParameter;

class MxpaHttpHandler implements HttpHandler {
    private final Logging logging;
    private Map<String, Integer> sessionCounters;

    public MxpaHttpHandler(MontoyaApi api) {
        this.logging = api.logging();
        sessionCounters = new HashMap<String, Integer>();
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        if (!isEnabled(requestToBeSent)) {
            return continueWith(requestToBeSent);
        }

        HttpParameter parameter = requestToBeSent.parameter("SESSION", HttpParameterType.URL);
        if (parameter == null) {
            parameter = requestToBeSent.parameter("SESSION", HttpParameterType.BODY);
        }

        Integer counter = Integer.valueOf(parameter.value());

        String sessionId = requestToBeSent.headerValue("MgxpaRIAglobalUniqueSessionID");
        Integer sessionCounterValue = sessionCounters.get(sessionId);

        if (sessionCounterValue == null) {
            sessionCounters.put(sessionId, counter);
            return continueWith(requestToBeSent);
        }

        if (counter > sessionCounterValue) {
            sessionCounters.put(sessionId, counter);
            return continueWith(requestToBeSent);
        }

        // if (!requestToBeSent.toolSource().isFromTool(ToolType.REPEATER)) {
        // logging.logToOutput("Not modified 4");
        // return continueWith(requestToBeSent);
        // }

        HttpParameter updatedParameter = HttpParameter.parameter("SESSION", String.valueOf(sessionCounterValue + 1),
                parameter.type());

        HttpRequest modifiedRequest = requestToBeSent.withUpdatedParameters(updatedParameter)
                .withAddedHeader(HttpHeader.httpHeader("ExpectedNextSessionCounter", String.valueOf(counter + 1)));

        sessionCounters.put(sessionId, sessionCounterValue + 1);

        return continueWith(modifiedRequest);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        if (!responseReceived.initiatingRequest().hasHeader("ExpectedNextSessionCounter")) {
            return continueWith(responseReceived);
        }

        HttpResponse modifiedResponse = responseReceived.withUpdatedHeader("MgxpaNextSessionCounter",
                responseReceived.initiatingRequest().headerValue("ExpectedNextSessionCounter"));

        return continueWith(modifiedResponse);
    }

    private static boolean isEnabled(HttpRequestToBeSent httpRequestToBeSent) {
        if (httpRequestToBeSent.method().equalsIgnoreCase("OPTIONS")) {
            return false;
        }

        boolean validPath = httpRequestToBeSent.pathWithoutQuery().endsWith("/xparequester");
        boolean hasSessionParameterUrl = httpRequestToBeSent.hasParameter("SESSION", HttpParameterType.URL);
        boolean hasSessionParameterBody = httpRequestToBeSent.hasParameter("SESSION", HttpParameterType.BODY);

        return (validPath && hasSessionParameterUrl) || (validPath && hasSessionParameterBody);
    }
}
