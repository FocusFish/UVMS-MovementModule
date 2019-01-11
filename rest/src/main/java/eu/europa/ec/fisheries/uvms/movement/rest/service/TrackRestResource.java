package eu.europa.ec.fisheries.uvms.movement.rest.service;

import java.util.List;
import java.util.UUID;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import com.vividsolutions.jts.geom.Geometry;
import eu.europa.ec.fisheries.schema.movement.v1.MovementTrack;
import eu.europa.ec.fisheries.uvms.movement.service.dao.MovementDao;
import eu.europa.ec.fisheries.uvms.movement.service.entity.Track;
import eu.europa.ec.fisheries.uvms.movement.service.mapper.MovementEntityToModelMapper;
import eu.europa.ec.fisheries.uvms.movement.service.util.WKTUtil;
import eu.europa.ec.fisheries.uvms.rest.security.RequiresFeature;
import eu.europa.ec.fisheries.uvms.rest.security.UnionVMSFeature;

@Path("/track")
@Stateless
public class TrackRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(TrackRestResource.class);

    @Inject
    private MovementDao movementDao;

    @GET
    @Consumes(value = {MediaType.APPLICATION_JSON})
    @Produces(value = {MediaType.APPLICATION_JSON})
    @Path(value = "/{id}")
    @RequiresFeature(UnionVMSFeature.viewMovements)
    public Response getTrack(@PathParam("id") String stringId) {
        try {

            UUID id = UUID.fromString(stringId);
            Track track = movementDao.getTrackById(id);
            List<Geometry> points = movementDao.getPointsFromTrack(track);
            MovementTrack returnTrack = MovementEntityToModelMapper.mapToMovementTrack(track, points);

            return Response.ok(returnTrack).type(MediaType.APPLICATION_JSON)
                    .header("MDC", MDC.get("requestId")).build();

        } catch (Exception e) {
            LOG.error("[ Error when getting segment. ] {}", e.getMessage(), e);
            return Response.status(500).entity(ExceptionUtils.getRootCause(e)).type(MediaType.APPLICATION_JSON)
                    .header("MDC", MDC.get("requestId")).build();
        }
    }
}