package ru.nmedvedev.rest;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.PathParam;
import ru.nmedvedev.model.SodexoResponse;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;


@Path("/api/1")
@RegisterRestClient(configKey = "sodexo-api")
@RegisterProvider(ManualLoggingFilter.class)
public interface SodexoClient {

    @GET
    @Path("/cards/{card}")
    @Produces("application/json")
    @ClientHeaderParam(name = "X-Requested-With", value = "XMLHttpRequest")
    Uni<SodexoResponse> getByCard(@PathParam(value = "card") String card);

}
