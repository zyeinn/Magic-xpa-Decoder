package magicxpadecoder;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.EditorMode;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;

import java.awt.*;

class MxpaHttpResponseEditor implements ExtensionProvidedHttpResponseEditor {
    // private final RawEditor responseEditor;
    private final HttpResponseEditor responseEditor;
    private HttpRequestResponse requestResponse;

    private final MontoyaApi api;

    MxpaHttpResponseEditor(MontoyaApi api, EditorCreationContext creationContext) {
        this.api = api;

        if (creationContext.editorMode() == EditorMode.READ_ONLY) {
            // responseEditor =
            // api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY,
            // EditorOptions.WRAP_LINES);
            responseEditor = api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);
        } else {
            // responseEditor =
            // api.userInterface().createHttpResponseEditor(EditorOptions.WRAP_LINES);
            responseEditor = api.userInterface().createHttpResponseEditor();
        }
    }

    @Override
    public HttpResponse getResponse() {
        return requestResponse.response();
    }

    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        this.requestResponse = requestResponse;

        Utils.DecodeResult result = Utils.decode(this.requestResponse.response().body());

        // this.responseEditor.setContents(result.getResult());
        this.responseEditor.setResponse(requestResponse.response().withBody(result.getResult()));
    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        // return (requestResponse.response().hasHeader("MgxpaNextSessionCounter") &&
        // requestResponse.response().hasHeader("Content-Type", "text/html"))
        // ||
        // (requestResponse.response().hasHeader("Access-Control-Expose-Headers",
        // "MgxpaNextSessionCounter") &&
        // requestResponse.response().hasHeader("Content-Type", "text/xml"));

        HttpRequest request = requestResponse.request();
        if (request != null) {
            return !request.method().equalsIgnoreCase("OPTIONS") && request.pathWithoutQuery().endsWith("/xparequester");
        }

        return requestResponse.response().hasHeader("MgxpaNextSessionCounter") &&
                requestResponse.response().hasHeader("Content-Type", "text/html");
    }

    @Override
    public String caption() {
        return "Deserialized input";
    }

    @Override
    public Component uiComponent() {
        return responseEditor.uiComponent();
    }

    @Override
    public Selection selectedData() {
        return responseEditor.selection().isPresent() ? responseEditor.selection().get() : null;
    }

    @Override
    public boolean isModified() {
        return responseEditor.isModified();
    }
}
