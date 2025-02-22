package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.ExtendedObjectResource;
import org.traccar.database.MediaManager;
import org.traccar.model.Inmate;
import org.traccar.model.User;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Path("inmates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InmateResource extends ExtendedObjectResource<Inmate> {

    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int IMAGE_SIZE_LIMIT = 500000;

    @Inject
    private MediaManager mediaManager;

    private String imageExtension(String type) {
        return switch (type) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/svg+xml" -> "svg";
            default -> throw new IllegalArgumentException("Unsupported image type");
        };
    }

    public InmateResource() {
        super(Inmate.class, "firstname");
    }

    @Path("{id}/images/{path}")
    @POST
    @Consumes("image/*")
    public Response uploadImage(
            @PathParam("id") long inmateId, File file,
            @HeaderParam(HttpHeaders.CONTENT_TYPE) String type,
            @PathParam("path") String path) throws StorageException, IOException {

        Inmate inmate = storage.getObject(Inmate.class, new Request(
                new Columns.All(),
                new Condition.And(
                        new Condition.Equals("id", inmateId),
                        new Condition.Permission(User.class, getUserId(), Inmate.class))));
        if (inmate != null) {
            String extension = imageExtension(type);
            try (var input = new FileInputStream(file);
                 var output = mediaManager.createFileStreamAllowDuplicates(inmate.getDniIdentification(), path, extension)) {

                long transferred = 0;
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                int read;
                while ((read = input.read(buffer, 0, buffer.length)) >= 0) {
                    output.write(buffer, 0, read);
                    transferred += read;
                    if (transferred > IMAGE_SIZE_LIMIT) {
                        throw new IllegalArgumentException("Image size limit exceeded");
                    }
                }
            }
            return Response.ok(path + "." + extension).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @Path("{id}/images")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listImages(@PathParam("id") long inmateId) throws StorageException {
        Inmate inmate = storage.getObject(Inmate.class, new Request(
                new Columns.All(),
                new Condition.And(
                        new Condition.Equals("id", inmateId),
                        new Condition.Permission(User.class, getUserId(), Inmate.class))));
        List<String> imagesList = mediaManager.getImagesList(inmate.getDniIdentification());
        if (imagesList == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("No images found").build();
        }
        return Response.ok(imagesList).build();
    }

    @Path("{id}/images/{path}")
    @DELETE
    public Response deleteImage(@PathParam("id") long inmateId, @PathParam("path") String path) throws StorageException {
        Inmate inmate = storage.getObject(Inmate.class, new Request(
                new Columns.All(),
                new Condition.And(
                        new Condition.Equals("id", inmateId),
                        new Condition.Permission(User.class, getUserId(), Inmate.class))));
        if (inmate != null) {
            boolean deleted = mediaManager.deleteImage(inmate.getDniIdentification(), path);
            if (deleted) {
                return Response.ok("Image deleted successfully").build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("Image not found").build();
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

}
