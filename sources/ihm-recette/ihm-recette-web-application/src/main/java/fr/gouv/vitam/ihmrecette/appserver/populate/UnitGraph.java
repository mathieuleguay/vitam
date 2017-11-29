package fr.gouv.vitam.ihmrecette.appserver.populate;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;

public class UnitGraph {

    private final LoadingCache<String, UnitModel> cache;

    public UnitGraph(MetadataRepository metadataRepository) {

        cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, UnitModel>() {
                @Override
                public UnitModel load(String key) {
                    Optional<UnitModel> unitById = metadataRepository.findUnitById(key);
                    return unitById.orElseThrow(() -> new RuntimeException("rootId not present in database: " + key));
                }
            });
    }

    // work only with one parent
    public UnitModel createGraph(DescriptiveMetadataModel descriptiveMetadataModel, String rootId, int tenantId,
        String originatingAgency) {

        // id of the unit.
        String guid = GUIDFactory.newUnitGUID(tenantId).toString();

        UnitModel unitModel = new UnitModel();

        unitModel.setId(guid);
        unitModel.setSp(originatingAgency);
        unitModel.setUp(rootId);
        unitModel.setTenant(tenantId);
        unitModel.setDescriptiveMetadataModel(descriptiveMetadataModel);

        if (rootId == null) {
            return unitModel;
        }

        UnitModel rootUnit = cache.getUnchecked(rootId);

        unitModel.getSps().addAll(rootUnit.getSps());
        unitModel.getSps().add(originatingAgency);

        unitModel.getUs().addAll(rootUnit.getUs());
        unitModel.getUs().add(rootId);

        // calcul de l'uds
        unitModel.getUds().putAll(rootUnit.getUds());
        // create a copy of a map
        Map<String, Integer> uds = unitModel.getUds();
        for (String s : uds.keySet()) {
            uds.replace(s, uds.get(s) + 1);
        }

        uds.put(rootId, 1);

        return unitModel;
    }

}
