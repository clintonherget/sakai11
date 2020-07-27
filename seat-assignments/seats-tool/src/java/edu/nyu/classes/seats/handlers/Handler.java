package edu.nyu.classes.seats.handlers;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The interface implemented by all handlers.
 */
public interface Handler {
    /**
     * Handle a request and either redirect the user or set the request context
     * to display an appropriate view.
     *
     * If a string entry called "subpage" is added to the context, this will be
     * resolved to a handlebars template and rendered.
     */
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context);

    /**
     * True if the handler has returned a redirect.
     */
    public boolean hasRedirect();

    public String getRedirect();

    /**
     * Return any validation errors produced by this request.
     */
    public Errors getErrors();

    /**
     * Return any flash messages (from the previous request) that should be displayed.
     */
    public Map<String, List<String>> getFlashMessages();

    public interface Errors {}

    public default String getContentType() {
        return "text/html";
    }

    public default boolean hasTemplate() {
        return true;
    }
}