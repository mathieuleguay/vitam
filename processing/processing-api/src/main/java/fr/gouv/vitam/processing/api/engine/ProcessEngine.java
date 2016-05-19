/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.api.engine;

import fr.gouv.vitam.processing.api.exception.EngineIllegalArgumentException;
import fr.gouv.vitam.processing.api.exception.WorkflowNotFoundException;
import fr.gouv.vitam.processing.api.model.Response;
import fr.gouv.vitam.processing.api.model.WorkParams;

/**
 * Process Engine
 * 
 * Provides access to all the services and manage a workflow operations.
 *
 */
// TODO REVIEW improve comment form
// FIXME REVIEW missing package-info
public interface ProcessEngine {

	/**
	 * Starts a new process instance in the exactly specified process definition
	 * with the given id.
	 * 
	 * @param workParams
	 *            used to encapsulate a parameters
	 * @param workflowId
	 *            used to identify workflow id
	 * @return the Response of process: message and status code
	 * @throws EngineIllegalArgumentException
	 *             if an or all argument null or invalid
	 * @throws WorkflowNotFoundException
	 *             if workflow not found
	 */
	// FIXME REVIEW rename more accurately: startWorkflow ?
	public Response startProcessByWorkFlowId(WorkParams workParams, String workflowId)
			throws EngineIllegalArgumentException, WorkflowNotFoundException;

}
