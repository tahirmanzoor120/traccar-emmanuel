package org.traccar.api.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.Inmate;

@Path("inmates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InmateResource extends ExtendedObjectResource<Inmate> {

    public InmateResource() {
        super(Inmate.class, "firstname");
    }

}
