/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functional.administration.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class AdminManagementApplicationTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(VitamCollection.getMongoClientOptions());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raise an exception";
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-test.conf";
    private static final String DATABASE_HOST = "localhost";
    private static File adminConfigFile;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT));

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, MongoRule.getDataBasePort()));


        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig =
            PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);
        realAdminConfig.setMongoDbNodes(nodes);
        realAdminConfig.setDbName(MongoRule.VITAM_DB);
        realAdminConfig.setElasticsearchNodes(nodesEs);
        realAdminConfig.setClusterName(ElasticsearchRule.VITAM_CLUSTER);
        adminConfigFile = File.createTempFile("test", ADMIN_MANAGEMENT_CONF, adminConfig.getParentFile());
        PropertiesUtils.writeYaml(adminConfigFile, realAdminConfig);

    }


    @Test
    public void testFunctionnalIdConfiguration() throws Exception {
        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig =
            PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);

        Map<Integer, List<String>> list =
            realAdminConfig.getListEnableExternalIdentifiers();
        assertThat(list.get(0).get(0)).isEqualTo("INGEST_CONTRACT");
        assertThat(list.get(0).get(1)).isEqualTo("RULES");
        assertThat(list.get(1).get(0)).isEqualTo("ACCESS_CONTRACT");
        assertThat(list.get(1).get(1)).isEqualTo("PROFILE");
    }

    @Test
    public final void testFictiveLaunch() {
        try {
            new AdminManagementMain(adminConfigFile.getAbsolutePath());
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public final void shouldRaiseException() {
        new AdminManagementMain("");
    }
}
