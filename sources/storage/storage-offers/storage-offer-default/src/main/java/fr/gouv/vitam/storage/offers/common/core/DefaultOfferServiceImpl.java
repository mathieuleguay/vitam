/*******************************************************************************
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
 *******************************************************************************/

package fr.gouv.vitam.storage.offers.common.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.builder.StoreContextBuilder;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.model.ContainerInformation;

/**
 * Default offer service implementation
 */
public class DefaultOfferServiceImpl implements DefaultOfferService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferServiceImpl.class);

    private static final DefaultOfferService INSTANCE = new DefaultOfferServiceImpl();
    private final ContentAddressableStorage defaultStorage;
    private static final String STORAGE_CONF_FILE_NAME = "default-storage.conf";

    private final Map<String, DigestType> digestTypeFor;
    private final Map<String, String> objectTypeFor;

    private DefaultOfferServiceImpl() {
        StorageConfiguration configuration;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(STORAGE_CONF_FILE_NAME),
                StorageConfiguration.class);
        } catch (final IOException exc) {
            LOGGER.error(exc);
            throw new ExceptionInInitializerError(exc);
        }
        defaultStorage = StoreContextBuilder.newStoreContext(configuration);
        digestTypeFor = new HashMap<>();
        objectTypeFor = new HashMap<>();
    }

    /**
     * @return the default instance
     */
    public static DefaultOfferService getInstance() {
        return INSTANCE;
    }

    @Override
    public String getObjectDigest(String containerName, String objectId, DigestType digestAlgorithm)
        throws ContentAddressableStorageException {
        return defaultStorage.computeObjectDigest(containerName, objectId, digestAlgorithm);
    }

    @Override
    public Response getObject(String containerName, String objectId, AsyncResponse asyncResponse)
        throws ContentAddressableStorageException {
        final Response response = defaultStorage.getObjectAsync(containerName, objectId, asyncResponse);
        final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
        final ResponseBuilder responseBuilder =
            Response.status(response.getStatus()).type(MediaType.APPLICATION_OCTET_STREAM);
        helper.writeResponse(responseBuilder);
        return response;
    }



    @Override
    public ObjectInit initCreateObject(String containerName, ObjectInit objectInit, String objectGUID)
        throws ContentAddressableStorageServerException, ContentAddressableStorageAlreadyExistException,
        ContentAddressableStorageNotFoundException {
        if (!defaultStorage.isExistingContainer(containerName)) {
            defaultStorage.createContainer(containerName);
        }
        objectInit.setId(objectGUID);
        objectTypeFor.put(objectGUID, objectInit.getType().getFolder());
        if (objectInit.getDigestAlgorithm() != null) {
            digestTypeFor.put(objectGUID, objectInit.getDigestAlgorithm());
        } else {
            digestTypeFor.put(objectGUID, VitamConfiguration.getDefaultDigestType());
        }

        return objectInit;
    }

    @Override
    public void createFolder(String containerName, String folderName)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageAlreadyExistException,
        ContentAddressableStorageNotFoundException {
        defaultStorage.createFolder(containerName, folderName);
    }

    @Override
    public String createObject(String containerName, String objectId, InputStream objectPart, boolean ending)
        throws IOException, ContentAddressableStorageException {
        // check container
        if (!defaultStorage.isExistingContainer(containerName)) {
            throw new ContentAddressableStorageException("Container does not exist");
        }
        // TODO No chunk mode (should be added in the future)
        // TODO the objectPart should contain the full object.
        try {
            defaultStorage.putObject(containerName, objectId, objectPart);
            // Check digest AFTER writing in order to ensure correctness
            final String digest = defaultStorage.computeObjectDigest(containerName,
                objectId, getDigestAlgoFor(objectId));
            // remove digest algo
            digestTypeFor.remove(objectId);
            return digest;
        } catch (final ContentAddressableStorageException exc) {
            LOGGER.error("Error with storage service", exc);
            throw exc;
        }
    }

    @Override
    public boolean isObjectExist(String containerName, String objectId)
        throws ContentAddressableStorageServerException {
        return defaultStorage.isExistingObject(containerName, objectId);
    }

    @Override
    public JsonNode getCapacity(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        final ObjectNode result = JsonHandler.createObjectNode();
        final ContainerInformation containerInformation = defaultStorage.getContainerInformation(containerName);
        result.put("usableSpace", containerInformation.getUsableSpace());
        result.put("usedSpace", containerInformation.getUsedSpace());
        result.put("tenantId", containerName);
        return result;
    }

    private DigestType getDigestAlgoFor(String id) {
        return digestTypeFor.get(id) != null ? digestTypeFor.get(id) : VitamConfiguration.getDefaultDigestType();
    }

    @Override
    public boolean checkObject(String containerName, String objectId, String digest,
        DigestType digestAlgorithm) throws ContentAddressableStorageException {
        String offerDigest = getObjectDigest(containerName, objectId, digestAlgorithm);
        return offerDigest.equals(digest);
    }
}
