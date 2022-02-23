package fish.focus.uvms.movement.service;

import java.io.File;
import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fish.focus.uvms.movement.service.message.SpatialModuleMock;
import fish.focus.uvms.movement.service.message.UnionVMSMock;
import fish.focus.uvms.movement.service.message.rest.mock.AssetMTRestMock;

@ArquillianSuiteDeployment
public abstract class BuildMovementServiceTestDeployment {

    final static Logger LOG = LoggerFactory.getLogger(BuildMovementServiceTestDeployment.class);

    @Deployment(name = "movementservice", order = 2)
    public static Archive<?> createDeployment() {
        WebArchive testWar = ShrinkWrap.create(WebArchive.class, "test.war");

        File[] files = Maven.resolver().loadPomFromFile("pom.xml")
                .importRuntimeAndTestDependencies().resolve().withTransitivity().asFile();
        testWar.addAsLibraries(files);

        testWar.addPackages(true, "fish.focus.uvms.movement.service");
        testWar.addPackages(true, "fish.focus.uvms.movement.rest");

        testWar.deleteClass(UnionVMSMock.class);
        testWar.deleteClass(SpatialModuleMock.class);
        testWar.deleteClass(AssetMTRestMock.class);

        testWar.addAsWebInfResource("ejb-jar.xml");
        testWar.addAsResource("persistence-integration.xml", "META-INF/persistence.xml");
        testWar.addAsResource("beans.xml", "META-INF/beans.xml");

        testWar.delete("/WEB-INF/web.xml");
        testWar.addAsWebInfResource("mock-web.xml", "web.xml");

		return testWar;
	}
    
    @Deployment(name = "uvms", order = 1)
    public static Archive<?> createSpatialMock() {

        WebArchive testWar = ShrinkWrap.create(WebArchive.class, "unionvms.war");

        File[] files = Maven.configureResolver().loadPomFromFile("pom.xml")
                .resolve("fish.focus.uvms.movement:movement-model",
                        "fish.focus.uvms.asset:asset-model",
                        "fish.focus.uvms.commons:uvms-commons-message",
                        "fish.focus.uvms.commons:uvms-commons-date",
                        "fish.focus.uvms.asset:asset-client",
                        "fish.focus.uvms.lib:usm4uvms")
                .withTransitivity().asFile();
        testWar.addAsLibraries(files);

        testWar.addClass(UnionVMSMock.class);
        testWar.addClass(SpatialModuleMock.class);
        testWar.addClass(AssetMTRestMock.class);

        return testWar;
    }
}
