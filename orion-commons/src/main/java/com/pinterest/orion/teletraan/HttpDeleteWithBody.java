package com.pinterest.orion.teletraan;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

/**
 * Extends HttpEntityEnclosingRequestBase to allow for a body in a DELETE request.
 * This class is necessary to allow for a body in a DELETE request because:
 * - HttpDelete does not allow for a body in the request.
 * - Extending HttpEntityEnclosingRequestBase enables setEntity() to be called.
 * - Returning the method name of "DELETE" allows for the request to be executed as a DELETE request.
 */
class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
    public static final String METHOD_NAME = "DELETE";

    public String getMethod() {
        return METHOD_NAME;
    }

    public HttpDeleteWithBody(final String uri) {
        super();
        setURI(URI.create(uri));
    }
}
