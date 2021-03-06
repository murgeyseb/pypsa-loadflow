/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.pypsa_loadflow;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.loadflow.LoadFlowParameters;

import java.util.Objects;

import static eu.itesla_project.pypsa_loadflow.PyPsaLoadFlowParameters.*;

/**
 * PyPSA loadflow additional parameters configuration loader
 *
 * Used to load PyPSA loadflow additional parameters from PowSyBl configuration
 *
 * @author Sebastien Murgey <sebastien.murgey at rte-france.com>
 */
@AutoService(LoadFlowParameters.ConfigLoader.class)
public class PyPsaLoadFlowParametersConfigLoader implements LoadFlowParameters.ConfigLoader<PyPsaLoadFlowParameters> {

    static final String MODULE_NAME = "pypsa-default-loadflow-parameters";

    @Override
    public String getExtensionName() {
        return "PyPSALoadflowParameters";
    }

    @Override
    public String getCategoryName() {
        return "loadflow-parameters";
    }

    @Override
    public Class<? super PyPsaLoadFlowParameters> getExtensionClass() {
        return PyPsaLoadFlowParameters.class;
    }

    @Override
    public PyPsaLoadFlowParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        ModuleConfig config = platformConfig.getModuleConfigIfExists(MODULE_NAME);
        PyPsaLoadFlowParameters pyPsaLoadFlowParameters = new PyPsaLoadFlowParameters();
        if (config != null) {
            pyPsaLoadFlowParameters.setLoadflowScriptPath(config.getPathProperty("loadflowScriptPath", DEFAULT_LOADFLOW_SCRIPT_PATH));
            pyPsaLoadFlowParameters.setDebugActivated(config.getBooleanProperty("debugActivated", DEFAULT_DEBUG_ACTIVATED));
            pyPsaLoadFlowParameters.setDcLoadFlow(config.getBooleanProperty("dcLoadFlow", DEFAULT_DC_LOAD_FLOW));
            pyPsaLoadFlowParameters.setRelaxationCoeff(config.getFloatProperty("relaxationCoeff", DEFAULT_RELAXATION_COEFF));
        }
        return pyPsaLoadFlowParameters;
    }
}
