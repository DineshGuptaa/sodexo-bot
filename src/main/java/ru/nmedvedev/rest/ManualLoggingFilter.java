package ru.nmedvedev.rest;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class ManualLoggingFilter implements ClientRequestFilter {
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        System.out.println("!!! DEBUG URL: " + requestContext.getUri());
        System.out.println("!!! DEBUG HEADERS: " + requestContext.getHeaders());
        System.out.println("!!! DEBUG BODY: " + requestContext.getEntity());
    }
}