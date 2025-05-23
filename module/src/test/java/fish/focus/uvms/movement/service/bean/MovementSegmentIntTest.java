package fish.focus.uvms.movement.service.bean;

import fish.focus.uvms.movement.service.MovementHelpers;
import fish.focus.uvms.movement.service.TransactionalTests;
import fish.focus.uvms.movement.service.dao.MovementDao;
import fish.focus.uvms.movement.service.entity.Movement;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Arquillian.class)
public class MovementSegmentIntTest extends TransactionalTests {

    private static final int ALL = -1;
    private static final int ORDER_NORMAL = 1;
    private static final int ORDER_REVERSED = 2;
    private static final int ORDER_RANDOM = 3;

    @EJB
    private MovementService movementService;

    @EJB
    private MovementDao movementDao;

    @EJB
    private IncomingMovementBean incomingMovementBean;

    @Test
    @OperateOnDeployment("movementservice")
    public void createVarbergGrenaNormal() {
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        UUID connectId = UUID.randomUUID();

        List<Movement> movementList = movementHelpers.createVarbergGrenaMovements(ORDER_NORMAL, ALL, connectId);
        assertMovementIds(movementList);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void createFishingTourVarberg() {
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        UUID connectId = UUID.randomUUID();

        List<Movement> movementList = movementHelpers.createFishingTourVarberg(ORDER_NORMAL, connectId);
        assertMovementIds(movementList);
    }

    private void assertMovementIds(List<Movement> movementList) {
        Collections.sort(movementList, Comparator.comparing(m -> m.getTimestamp()));

        Movement previous = null;
        for (Movement movement : movementList) {
            if (previous == null) {
                assertThat(movement.getPreviousMovement(), is(CoreMatchers.nullValue()));
            } else {
                assertThat(movement.getPreviousMovement(), is(previous));
            }
            previous = movement;
        }
    }
}
