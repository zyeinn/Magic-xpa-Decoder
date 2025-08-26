package magicxpadecoder;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.EditorMode;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.utilities.URLUtils;
import java.lang.IllegalArgumentException;

import java.awt.*;

class MxpaHttpRequestEditor implements ExtensionProvidedHttpRequestEditor {
    // private final RawEditor requestEditor;
    private final HttpRequestEditor requestEditor;
    private HttpRequestResponse requestResponse;

    private final MontoyaApi api;

    MxpaHttpRequestEditor(MontoyaApi api, EditorCreationContext creationContext) {
        this.api = api;

        if (creationContext.editorMode() == EditorMode.READ_ONLY) {
            // requestEditor = api.userInterface().createRawEditor(EditorOptions.READ_ONLY,
            // EditorOptions.WRAP_LINES);
            requestEditor = api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
        } else {
            // requestEditor =
            // api.userInterface().createRawEditor(EditorOptions.WRAP_LINES);
            requestEditor = api.userInterface().createHttpRequestEditor();
        }
    }

    @Override
    public HttpRequest getRequest() {
        HttpRequest request = requestResponse.request();

        if (requestEditor.isModified()) {
            // reserialize data
            // System.out.println("REQUEST MODIFIED");
            try {
                URLUtils urlUtils = api.utilities().urlUtils();
                ScrambleLocation scrambleLoc = null;

                String value = requestResponse.request().parameterValue("DATA", HttpParameterType.URL);

                if (value != null) {
                    scrambleLoc = ScrambleLocation.URL;
                } else {
                    scrambleLoc = ScrambleLocation.BODY;
                }

                // Utils.DecodeResult result = Utils.Encode(requestEditor.getContents());
                Utils.DecodeResult result = Utils.encode(requestEditor.getRequest().body());

                if (scrambleLoc == ScrambleLocation.URL) {
                    request = requestResponse.request().withUpdatedParameters(
                            HttpParameter.urlParameter("DATA", urlUtils.encode(result.getResult().toString())));
                } else if (scrambleLoc == ScrambleLocation.BODY) {
                    request = requestResponse.request().withUpdatedParameters(
                            HttpParameter.bodyParameter("DATA", urlUtils.encode(result.getResult().toString())));
                }
            } catch (IllegalArgumentException ex) {
                this.api.logging().logToError(ex.toString());
            } catch (Exception ex) {
                this.api.logging().logToError(ex.toString());
            }
        }

        return request;
    }

    private static enum ScrambleLocation {
        URL,
        BODY
    }

    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        this.requestResponse = requestResponse;

        URLUtils urlUtils = api.utilities().urlUtils();

        String value = requestResponse.request().parameterValue("DATA", HttpParameterType.URL);

        if (value == null) {
            value = requestResponse.request().parameterValue("DATA", HttpParameterType.BODY);
        }

        value = urlUtils.decode(value);

        Utils.DecodeResult result = Utils.decode(ByteArray.byteArray(value));

        this.requestEditor.setRequest(requestResponse.request().withBody(result.getResult())
                .withRemovedParameters(HttpParameter.urlParameter("DATA", "")));
        if (result.getError() == true) {
            this.api.logging().logToError(result.getResult().toString());
            // this.requestEditor.setEditable(false);
        }
    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        boolean res;
        boolean header;
        boolean validPath;

        try {
            header = requestResponse.request().hasHeader("MgxpaRIAglobalUniqueSessionID");
            validPath = requestResponse.request().pathWithoutQuery().endsWith("/xparequester");
        } catch (Exception ex) {
            return false;
        }

        res = (header && validPath && requestResponse.request().hasParameter("DATA", HttpParameterType.URL)) ||
                (header && validPath && requestResponse.request().hasParameter("DATA", HttpParameterType.BODY));

        return res;
    }

    @Override
    public String caption() {
        return "Decoded input";
    }

    @Override
    public Component uiComponent() {
        return requestEditor.uiComponent();
    }

    @Override
    public Selection selectedData() {
        return requestEditor.selection().isPresent() ? requestEditor.selection().get() : null;
    }

    @Override
    public boolean isModified() {
        return requestEditor.isModified();
    }
}
