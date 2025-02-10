package fish.focus.uvms.movement.service.dao;

import fish.focus.uvms.movement.service.TransactionalTests;
import fish.focus.uvms.movement.service.entity.group.MovementFilterGroup;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.ejb.EJBTransactionRolledbackException;
import javax.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class MovementSearchGroupDaoIntTest extends TransactionalTests {

    private static final String TEST_USER_NAME = "MovementSearchGroupDaoIntTestUser";

    @EJB
    private MovementSearchGroupDao dao;

    @Test
    @OperateOnDeployment("movementservice")
    public void createMovementFilterGroup() {
        MovementFilterGroup movementFilterGroup = newFilterGroup();
        movementFilterGroup = dao.createMovementFilterGroup(movementFilterGroup);
        assertNotNull(movementFilterGroup.getId());
        em.flush();
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void createMovementFilterGroupNoUpdateTime() {
        MovementFilterGroup movementFilterGroup = newFilterGroup();
        movementFilterGroup.setUpdated(null);

        dao.createMovementFilterGroup(movementFilterGroup);

        assertNotNull(movementFilterGroup.getId());
        assertThrows(ConstraintViolationException.class, () -> em.flush());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void createMovementFilterGroupNoUpdateBy() {
        MovementFilterGroup movementFilterGroup = newFilterGroup();
        movementFilterGroup.setUpdatedBy(null);

        dao.createMovementFilterGroup(movementFilterGroup);

        assertNotNull(movementFilterGroup.getId());
        assertThrows(ConstraintViolationException.class, () -> em.flush());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void deleteMovementFilterGroup() {
        MovementFilterGroup movementFilterGroup = newFilterGroup();
        movementFilterGroup = dao.createMovementFilterGroup(movementFilterGroup);
        assertNotNull(movementFilterGroup.getId());
        dao.deleteMovementFilterGroup(movementFilterGroup);
        em.flush();

        movementFilterGroup = dao.getMovementFilterGroupById(movementFilterGroup.getId());
        assertNull(movementFilterGroup);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getMovementFilterGroupById() {
        MovementFilterGroup movementFilterGroup = newFilterGroup();
        movementFilterGroup = dao.createMovementFilterGroup(movementFilterGroup);
        assertNotNull(movementFilterGroup.getId());
        em.flush();

        MovementFilterGroup movementFilterGroup2 = dao.getMovementFilterGroupById(movementFilterGroup.getId());
        assertNotNull(movementFilterGroup2);
        assertEquals(movementFilterGroup.getId(), movementFilterGroup2.getId());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void failGetMovementFilterGroupById() {
        MovementFilterGroup movementFilterGroup = dao.getMovementFilterGroupById(UUID.randomUUID());
        assertNull(movementFilterGroup);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getMovementFilterGroupsByUser() {
        MovementFilterGroup movementFilterGroup = newFilterGroup();
        movementFilterGroup = dao.createMovementFilterGroup(movementFilterGroup);
        assertNotNull(movementFilterGroup.getId());
        em.flush();

        List<MovementFilterGroup> movementFilterGroupsByUser = dao.getMovementFilterGroupsByUser(TEST_USER_NAME);
        assertNotNull(movementFilterGroupsByUser);
        assertFalse(movementFilterGroupsByUser.isEmpty());

        MovementFilterGroup foundIt = null;
        for (MovementFilterGroup mfg : movementFilterGroupsByUser) {
            if (mfg.getId().equals(movementFilterGroup.getId())) {
                foundIt = mfg;
                break;
            }
        }
        assertNotNull(foundIt);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void updateMovementFilterGroup() {
        MovementFilterGroup movementFilterGroup = newFilterGroup();
        movementFilterGroup.setUpdatedBy("First Value");
        movementFilterGroup = dao.createMovementFilterGroup(movementFilterGroup);
        assertNotNull(movementFilterGroup.getId());
        em.flush();

        movementFilterGroup.setUpdatedBy("Second Value");
        movementFilterGroup = dao.updateMovementFilterGroup(movementFilterGroup);
        em.flush();

        MovementFilterGroup movementFilterGroup2 = dao.getMovementFilterGroupById(movementFilterGroup.getId());
        assertNotNull(movementFilterGroup2);
        assertEquals(movementFilterGroup.getId(), movementFilterGroup2.getId());
        assertEquals(movementFilterGroup.getUpdatedBy(), movementFilterGroup2.getUpdatedBy());
        assertNotEquals("First Value", movementFilterGroup2.getUpdatedBy());

        assertNotEquals(TEST_USER_NAME, movementFilterGroup2.getUpdatedBy());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void updateMovementFilterGroupFailedNoId() {
        MovementFilterGroup movementFilterGroup = newFilterGroup();

        assertThrows(EJBTransactionRolledbackException.class, () -> dao.updateMovementFilterGroup(movementFilterGroup));
    }

    private MovementFilterGroup newFilterGroup() {
        MovementFilterGroup movementFilterGroup = new MovementFilterGroup();
        movementFilterGroup.setName("Test");
        movementFilterGroup.setActive("true");
        movementFilterGroup.setDynamic("true");
        movementFilterGroup.setFilters(new ArrayList<>());
        movementFilterGroup.setGlobal("true");
        movementFilterGroup.setUser(TEST_USER_NAME);
        movementFilterGroup.setUpdated(Instant.now());
        movementFilterGroup.setUpdatedBy(TEST_USER_NAME);

        return movementFilterGroup;
    }
}
