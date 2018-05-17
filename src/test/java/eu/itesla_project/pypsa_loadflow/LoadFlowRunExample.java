/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.pypsa_loadflow;

import com.google.common.collect.Sets;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.loadflow.validation.ValidationType;

import java.util.Set;

public class LoadFlowRunExample {
    public static void main(String[] args) throws Exception {
        Network network = Importers.loadNetwork("test_network.xiidm", LoadFlowRunExample.class.getResourceAsStream("/test_network.xiidm"));
        ComputationManager computationManager = LocalComputationManager.getDefault();
        LoadFlow pyPsaLoadFlow = new PyPsaLoadFlowFactory().create(network, computationManager, 1);
        pyPsaLoadFlow.run(LoadFlowParameters.load());

        // Pass loadflow validation
        Set<ValidationType> validationTypes = Sets.newHashSet(ValidationType.values());

        validationTypes.forEach(validationType -> {
            try {
                System.out.println("Validate load-flow results of network " + network.getId()
                        + " - validation type: " + validationType
                        + " - result: " + (validationType.check(network, ValidationConfig.load(), computationManager.getLocalDir()) ? "success" : "fail"));
            } catch (Exception e) {
                System.err.println("Error validating load-flow results of network " + network.getId()
                        + " - validation type: " + validationType
                        + " - error: " + e.getMessage());
            }
        });
    }
}
