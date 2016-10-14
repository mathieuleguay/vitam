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

package fr.gouv.vitam.storage.driver.model;

/**
 * Data structure representing global result from a 'get storage information / capacity' request
 */
public class StorageCapacityResult {

    private String tenantId;

    private long usableSpace;

    private long usedSpace;

    public StorageCapacityResult() {
        // Empty
    }

    /**
     * Initialize the needed parameters for get capacity results
     *
     * @param tenantId The request tenantId
     * @param usableSpace The usable space in offer
     * @param usedSpace The used space in offer
     */
    public StorageCapacityResult(String tenantId, long usableSpace, long usedSpace) {
        this.tenantId = tenantId;
        this.usableSpace = usableSpace;
        this.usedSpace = usedSpace;
    }

    /**
     * @return The request tenantId
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * @return The offer usable space
     */
    public long getUsableSpace() {
        return usableSpace;
    }

    /**
     * @return The offer used space
     */
    public long getUsedSpace() {
        return usedSpace;
    }

    /**
     * @param tenantId The request tenantId
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * @param usableSpace The usable space in offer
     */
    public void setUsableSpace(long usableSpace) {
        this.usableSpace = usableSpace;
    }

    /**
     * @param usedSpace The used space in offer
     */
    public void setUsedSpace(long usedSpace) {
        this.usedSpace = usedSpace;
    }
}
