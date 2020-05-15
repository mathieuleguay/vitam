/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.DataObjectInfo;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CheckSizeActionPlugin Plugin.<br>
 */
public class CheckSizeActionPlugin extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckSizeActionPlugin.class);

    public static final String CHECK_OBJECT_SIZE = "CHECK_OBJECT_SIZE";
    private static final int OG_OUT_RANK = 0;

    public CheckSizeActionPlugin() {
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) throws ProcessingException {
        checkMandatoryParameters(params);
        LOGGER.debug("CheckSizeActionPlugin running ...");

        // Set default status code to OK
        final ItemStatus itemStatus = new ItemStatus(CHECK_OBJECT_SIZE);
        try {
            // Get objectGroup
            final JsonNode jsonOG = handlerIO.getJsonFromWorkspace(
                IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + params.getObjectName());

            handlerIO.addOutputResult(OG_OUT_RANK, jsonOG, true, false);

            final Map<String, DataObjectInfo> binaryObjects = getBinaryObjects(jsonOG);

            boolean isFileSizeChanged = false;
            final JsonNode qualifiers = jsonOG.get(SedaConstants.PREFIX_QUALIFIERS);
            if (qualifiers != null) {
                final List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
                if (versions != null && !versions.isEmpty()) {
                    for (final JsonNode versionsArray : versions) {
                        for (final JsonNode version : versionsArray) {
                            if (version.get(SedaConstants.TAG_PHYSICAL_ID) == null) {
                                final String objectId = version.get(SedaConstants.PREFIX_ID).asText();
                                // The operator OR is useful for multibinary objects in GOT
                                isFileSizeChanged = isFileSizeChanged ||
                                    checkIsSizeIncorrect(binaryObjects.get(objectId), version, itemStatus);
                            }
                        }
                    }
                }
            }

            if (isFileSizeChanged) {
                handlerIO.transferJsonToWorkspace(IngestWorkflowConstants.OBJECT_GROUP_FOLDER,
                    params.getObjectName(), jsonOG, false, false);
            }

        } catch (ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        // Occurs only when GOT does not have qualifiers or versions or all versions are Physical
        if (itemStatus.getGlobalStatus().getStatusLevel() == StatusCode.UNKNOWN.getStatusLevel()) {
            itemStatus.increment(StatusCode.OK);
        }
        LOGGER.debug("CheckSizeActionPlugin response: " + itemStatus.getGlobalStatus());
        return new ItemStatus(CHECK_OBJECT_SIZE).setItemsStatus(CHECK_OBJECT_SIZE, itemStatus);
    }

    private boolean checkIsSizeIncorrect(DataObjectInfo dataObjectInfo, JsonNode version,
        ItemStatus itemStatus) {
        // create ItemStatus for subtask
        final ItemStatus subTaskItemStatus = new ItemStatus(CHECK_OBJECT_SIZE);
        Boolean isSizeChanged = Boolean.FALSE;
        if (version.get(SedaConstants.PREFIX_WORK) != null &&
            version.get(SedaConstants.PREFIX_WORK)
                .get(IngestWorkflowConstants.IS_SIZE_INCORRECT) != null &&
            version.get(SedaConstants.PREFIX_WORK)
                .get(IngestWorkflowConstants.IS_SIZE_INCORRECT).asBoolean() == Boolean.TRUE) {
            ((ObjectNode) version).remove(SedaConstants.PREFIX_WORK);
            subTaskItemStatus.increment(StatusCode.WARNING);
            itemStatus.increment(StatusCode.WARNING);
            isSizeChanged = Boolean.TRUE;
        } else {
            subTaskItemStatus.increment(StatusCode.OK);
        }
        itemStatus.setSubTaskStatus(dataObjectInfo.getId(), subTaskItemStatus);
        return isSizeChanged;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        handler.checkHandlerIO(1, Arrays.asList(new Class[] {String.class}));
    }

    private Map<String, DataObjectInfo> getBinaryObjects(JsonNode jsonOG) throws ProcessingException {
        final Map<String, DataObjectInfo> binaryObjects = new HashMap<>();

        final JsonNode work = jsonOG.get(SedaConstants.PREFIX_WORK);
        final JsonNode qualifiers = work.get(SedaConstants.PREFIX_QUALIFIERS);

        if (qualifiers == null) {
            // KO
            return binaryObjects;
        }

        final List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
        if (versions == null || versions.isEmpty()) {
            // KO
            return binaryObjects;
        }
        for (final JsonNode version : versions) {
            LOGGER.debug(version.toString());
            for (final JsonNode jsonBinaryObject : version) {
                if (jsonBinaryObject.get(SedaConstants.TAG_PHYSICAL_ID) == null) {
                    binaryObjects.put(jsonBinaryObject.get(SedaConstants.PREFIX_ID).asText(),
                        new DataObjectInfo()
                            .setSize(jsonBinaryObject.get(SedaConstants.TAG_SIZE).asLong())
                            .setId(jsonBinaryObject.get(SedaConstants.PREFIX_ID).asText())
                            .setUri(jsonBinaryObject.get(SedaConstants.TAG_URI).asText()));
                }
            }
        }
        // OK
        return binaryObjects;
    }
}
