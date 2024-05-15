package com.pinterest.orion.utils;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

/**
 * Extends HttpEntityEnclosingRequestBase to allow for a body in a DELETE request.
 * HttpDelete does not allow for a body in the request.
 * This class is necessary to allow for a body in a DELETE request.
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
