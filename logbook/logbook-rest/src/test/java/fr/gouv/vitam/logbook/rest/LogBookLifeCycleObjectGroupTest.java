/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.logbook.rest;

import static com.jayway.restassured.RestAssured.given;

import java.io.File;

import javax.ws.rs.core.Response.Status;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.MongoDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.MongoDbAccessFactory;

/**
 *
 */
public class LogBookLifeCycleObjectGroupTest {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogBookLifeCycleObjectGroupTest.class);

    private static final String REST_URI = "/logbook/v1";

    private static final String LOGBOOK_CONF = "logbook-test.conf";
    private static final String DATABASE_HOST = "localhost";
    private static MongoDbAccess mongoDbAccess;
    private static MongodExecutable mongodExecutable;
    private static MongodProcess mongod;
    private static VitamServer vitamServer;

    private static final String LIFE_OBJECT_GROUP_ID_URI = "/operations/{id_op}/objectgrouplifecycles/{id_lc}";

    private static int databasePort = 52661;
    private static int serverPort = 8889;
    // private static File newLogbookConf;

    private static LogbookLifeCycleObjectGroupParameters logbookLifeCyclesObjectGroupParametersStart;
    private static LogbookLifeCycleObjectGroupParameters logbookLCObjectGroupParametersAppend;
    private static LogbookLifeCycleObjectGroupParameters logbookLifeCyclesObjectGroupParametersBAD;

    private static LogbookLifeCycleObjectGroupParameters logbookLifeCyclesObjectGroupParametersUpdate;

    private static JunitHelper junitHelper;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        // junitHelper = new JunitHelper();
        // databasePort = junitHelper.findAvailablePort();
        final File logbook = PropertiesUtils.findFile(LOGBOOK_CONF);
        final LogbookConfiguration realLogbook = PropertiesUtils.readYaml(logbook, LogbookConfiguration.class);
        realLogbook.setDbPort(databasePort);
        // newLogbookConf = File.createTempFile("test", LOGBOOK_CONF, logbook.getParentFile());
        // PropertiesUtils.writeYaml(newLogbookConf, realLogbook);
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(databasePort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        mongoDbAccess =
            MongoDbAccessFactory.create(
                new DbConfigurationImpl(DATABASE_HOST, databasePort,
                    "vitam-test"));
        // serverPort = junitHelper.findAvailablePort();
        // TODO verifier la compatibilité avec les tests parallèles sur jenkins
        // SystemPropertyUtil.set(VitamServer.PARAMETER_JETTY_SERVER_PORT, Integer.toString(serverPort));

        RestAssured.port = serverPort;
        RestAssured.basePath = REST_URI;

        try {
            LogbookApplication.startApplication(new String[] {LOGBOOK_CONF});
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Logbook Application Server", e);
        }

        final GUID eip = GUIDFactory.newWriteLogbookGUID(0);
        final GUID iop = GUIDFactory.newWriteLogbookGUID(0);
        final GUID ioL = GUIDFactory.newUnitGUID(0);
        final GUID iog = GUIDFactory.newObjectGroupGUID(0);


        logbookLifeCyclesObjectGroupParametersStart =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCyclesObjectGroupParametersStart.setStatus(LogbookOutcome.STARTED);
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());
        /**
         * Bad request
         */
        logbookLifeCyclesObjectGroupParametersBAD = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();

        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());


        /**
         * update
         *
         */
            final GUID eip2 = GUIDFactory.newWriteLogbookGUID(0);
        final GUID iop2 = GUIDFactory.newWriteLogbookGUID(0);
        final GUID ioL2 = GUIDFactory.newUnitGUID(0);
        logbookLifeCyclesObjectGroupParametersUpdate =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCyclesObjectGroupParametersUpdate.setStatus(LogbookOutcome.STARTED);
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.eventIdentifier,
            eip2.toString());
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop2.toString());
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL2.toString());
        /**
         *
         * update
         */

        logbookLCObjectGroupParametersAppend = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        logbookLCObjectGroupParametersAppend.setStatus(LogbookOutcome.OK);
        logbookLCObjectGroupParametersAppend.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLCObjectGroupParametersAppend.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            LogbookApplication.stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        mongoDbAccess.close();
        // junitHelper.releasePort(serverPort);
        mongod.stop();
        mongodExecutable.stop();
        // newLogbookConf.delete();
        // junitHelper.releasePort(databasePort);
    }

    @Test
    public final void given_lifeCycleObjectGroup_when_create_thenReturn_created() {
        // Creation OK

        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesObjectGroupParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetail,
            "outcomeDetail");
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersStart.toString())
            .when()
            .post(LIFE_OBJECT_GROUP_ID_URI,
                logbookLifeCyclesObjectGroupParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.CREATED.getStatusCode());



        // already exsits
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersStart.toString())
            .when()
            .post(LIFE_OBJECT_GROUP_ID_URI,
                logbookLifeCyclesObjectGroupParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.CONFLICT.getStatusCode());

        // incoherence parameters ; response bad_request
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersStart.toString())
            .when()
            .post(LIFE_OBJECT_GROUP_ID_URI,
                logbookLifeCyclesObjectGroupParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                "bad_id")
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());


        // update ok
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "ModifiedoutcomeDetailMessage");
        logbookLifeCyclesObjectGroupParametersStart.setStatus(LogbookOutcome.OK);
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersStart.toString())
            .when()
            .put(LIFE_OBJECT_GROUP_ID_URI,
                logbookLifeCyclesObjectGroupParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.OK.getStatusCode());


        // Update illegal argument incoherence parameters ; response bad_request
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersStart.toString())
            .when()
            .put(LIFE_OBJECT_GROUP_ID_URI,
                logbookLifeCyclesObjectGroupParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                "bad_id")
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

    }

    @Test
    public final void given_lifeCycleObjectGroup_Without_MandotoryParams_when_create_thenReturn_BAD_RESUEST() {
        final GUID guidTest = GUIDFactory.newWriteLogbookGUID(0);

        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(LogbookParameterName.objectIdentifier,
            guidTest.toString());
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersBAD.toString())
            .when()
            .post(LIFE_OBJECT_GROUP_ID_URI,
                logbookLifeCyclesObjectGroupParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public final void given_lifeCycleGO_when_update_thenReturn_notfound() {
        // update notFound
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesObjectGroupParametersUpdate.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.outcomeDetail,
            "outcomeDetail");
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());


        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersUpdate.toString())
            .when()
            .put(LIFE_OBJECT_GROUP_ID_URI,
                logbookLifeCyclesObjectGroupParametersUpdate
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesObjectGroupParametersUpdate.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }


    @Test
    public final void given_lifeCycleGO_Without_MandotoryParams_when_update_thenReturn_BAD_RESUEST() {
        final GUID guidTest = GUIDFactory.newWriteLogbookGUID(0);
        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(LogbookParameterName.objectIdentifier,
            guidTest.toString());
        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            guidTest.toString());

        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersBAD.toString())
            .when()
            .put(LIFE_OBJECT_GROUP_ID_URI,
                logbookLifeCyclesObjectGroupParametersBAD
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesObjectGroupParametersBAD.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

}
