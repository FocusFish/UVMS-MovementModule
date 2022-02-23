package fish.focus.uvms.movement.rest.service;

import fish.focus.schema.movement.asset.v1.VesselType;
import fish.focus.schema.movement.v1.MovementPoint;
import fish.focus.schema.movement.v1.MovementSourceType;
import fish.focus.uvms.movement.rest.BuildMovementRestDeployment;
import fish.focus.uvms.movement.model.dto.MovementDto;
import fish.focus.uvms.movement.rest.filter.AppError;
import fish.focus.uvms.movement.service.dto.ManualMovementDto;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class ManualMovementRestResourceTest extends BuildMovementRestDeployment {

    @Test
    @OperateOnDeployment("movementservice")
    public void createManualMovementTest() {
        ManualMovementDto movement = createManualMovement();

        Response response = getWebTarget()
                .path("manualMovement")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(movement), Response.class);
        assertEquals(200, response.getStatus());

    }

    @Test
    @OperateOnDeployment("movementservice")
    public void createManualMovementWithTimeInFutureTest() {
        ManualMovementDto movement = createManualMovement();
        movement.getMovement().setTimestamp(Instant.now().plus(5, ChronoUnit.MINUTES));

        Response response = getWebTarget()
                .path("manualMovement")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(movement), Response.class);
        assertEquals(200, response.getStatus());

        AppError error = response.readEntity(AppError.class);
        assertEquals(400, error.code.intValue());
        assertTrue(error.description, error.description.contains("Time in future"));

    }

    @Test
    @OperateOnDeployment("movementservice")
    public void createManualMovementLessInfoInVesselTypeTest() {
        String movement = "{\"movement\":{\"location\":{\"longitude\":0.0,\"latitude\":0.0,\"altitude\":null},\"heading\":0.0,\"timestamp\":\"1575545948.469924300\",\"speed\":0.0,\"source\":\"MANUAL\"},\"asset\":{\"ircs\":\"T\",\"cfr\":\"T\"}}";

        Response response = getWebTarget()
                .path("manualMovement")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(movement), Response.class);
        assertEquals(200, response.getStatus());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void createManualMovementOnlySecondsInTimestampTest() {
        String movement = "{\"movement\":{\"location\":{\"longitude\":0.0,\"latitude\":0.0,\"altitude\":null},\"heading\":0.0,\"timestamp\":\"1575545948\",\"speed\":0.0,\"source\":\"MANUAL\"},\"asset\":{\"ircs\":\"T\",\"cfr\":\"T\"}}";

        Response response = getWebTarget()
                .path("manualMovement")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(movement), Response.class);
        assertEquals(200, response.getStatus());
    }


    /*
     * Helper functions for REST calls
     */
    private ManualMovementDto createManualMovement() {
        ManualMovementDto movement = new ManualMovementDto();
        VesselType asset = new VesselType();
        asset.setCfr("T");
        asset.setExtMarking("T");
        asset.setFlagState("T");
        asset.setIrcs("T");
        asset.setName("T");
        movement.setAsset(asset);

        MovementDto dto = new MovementDto();
        MovementPoint location = new MovementPoint();
        location.setLatitude(0.0);
        location.setLongitude(0.0);
        dto.setLocation(location);
        dto.setTimestamp(Instant.now());
        dto.setHeading(0.0f);
        dto.setSpeed(0.0f);
        dto.setSource(MovementSourceType.MANUAL);
        movement.setMovement(dto);

        return movement;
    }
    
}
