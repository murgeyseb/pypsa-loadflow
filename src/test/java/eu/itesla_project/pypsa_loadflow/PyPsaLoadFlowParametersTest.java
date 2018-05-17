/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.pypsa_loadflow;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.Objects;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PyPsaLoadFlowParametersTest {

    private InMemoryPlatformConfig platformConfig;
    private FileSystem fileSystem;
    private PyPsaLoadFlowParametersConfigLoader pyPsaLoadFlowParametersConfigLoader;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        pyPsaLoadFlowParametersConfigLoader = new PyPsaLoadFlowParametersConfigLoader();
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    public void checkValues(PyPsaLoadFlowParameters parameters, boolean debugActivated, boolean dcLoadFlow, float relaxationCoeff) {
        assertEquals(debugActivated, parameters.isDebugActivated());
        assertEquals(dcLoadFlow, parameters.isDcLoadFlow());
        assertEquals(relaxationCoeff, parameters.getRelaxationCoeff(), 0.0f);
    }

    @Test
    public void testNoConfig() {
        PyPsaLoadFlowParameters parameters = pyPsaLoadFlowParametersConfigLoader.load(platformConfig);
        checkValues(parameters,
                PyPsaLoadFlowParameters.DEFAULT_DEBUG_ACTIVATED,
                PyPsaLoadFlowParameters.DEFAULT_DC_LOAD_FLOW,
                PyPsaLoadFlowParameters.DEFAULT_RELAXATION_COEFF);
    }

    @Test
    public void checkConfig() {
        boolean debugActivated = true;
        boolean dcLoadFlow = true;
        float relaxationCoeff = 0.7f;

        MapModuleConfig mapModuleConfig = platformConfig.createModuleConfig("pypsa-default-loadflow-parameters");
        mapModuleConfig.setStringProperty("debugActivated", Objects.toString(debugActivated));
        mapModuleConfig.setStringProperty("dcLoadFlow", Objects.toString(dcLoadFlow));
        mapModuleConfig.setStringProperty("relaxationCoeff", Objects.toString(relaxationCoeff));

        PyPsaLoadFlowParameters parameters = pyPsaLoadFlowParametersConfigLoader.load(platformConfig);
        checkValues(parameters, debugActivated, dcLoadFlow, relaxationCoeff);
    }

    @Test
    public void checkIncompleteConfig() {
        boolean debugActivated = true;
        boolean dcLoadFlow = true;
        float relaxationCoeff = 0.7f;

        MapModuleConfig mapModuleConfig = platformConfig.createModuleConfig("pypsa-default-loadflow-parameters");
        mapModuleConfig.setStringProperty("debugActivate", Objects.toString(debugActivated));
        mapModuleConfig.setStringProperty("dcLoadflow", Objects.toString(dcLoadFlow));
        mapModuleConfig.setStringProperty("RelaxationCoeff", Objects.toString(relaxationCoeff));

        PyPsaLoadFlowParameters parameters = pyPsaLoadFlowParametersConfigLoader.load(platformConfig);
        checkValues(parameters,
                PyPsaLoadFlowParameters.DEFAULT_DEBUG_ACTIVATED,
                PyPsaLoadFlowParameters.DEFAULT_DC_LOAD_FLOW,
                PyPsaLoadFlowParameters.DEFAULT_RELAXATION_COEFF);
    }

    @Test
    public void checkSetters() {
        boolean debugActivated = true;
        boolean dcLoadFlow = true;
        float relaxationCoeff = 0.7f;

        PyPsaLoadFlowParameters parameters = new PyPsaLoadFlowParameters();

        parameters.setDebugActivated(debugActivated);
        parameters.setDcLoadFlow(dcLoadFlow);
        parameters.setRelaxationCoeff(relaxationCoeff);

        checkValues(parameters, debugActivated, dcLoadFlow, relaxationCoeff);
    }

    @Test
    public void checkClone() {
        boolean debugActivated = true;
        boolean dcLoadFlow = true;
        float relaxationCoeff = 0.7f;

        MapModuleConfig mapModuleConfig = platformConfig.createModuleConfig("pypsa-default-loadflow-parameters");
        mapModuleConfig.setStringProperty("debugActivated", Objects.toString(debugActivated));
        mapModuleConfig.setStringProperty("dcLoadFlow", Objects.toString(dcLoadFlow));
        mapModuleConfig.setStringProperty("relaxationCoeff", Objects.toString(relaxationCoeff));

        PyPsaLoadFlowParameters parameters = pyPsaLoadFlowParametersConfigLoader.load(platformConfig);
        PyPsaLoadFlowParameters parametersCloned = new PyPsaLoadFlowParameters(parameters);

        checkValues(parametersCloned, debugActivated, dcLoadFlow, relaxationCoeff);
    }

    @Test
    public void testExtensions() {
        platformConfig.createModuleConfig("pypsa-default-loadflow-parameters");
        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load(platformConfig);

        assertEquals(1, loadFlowParameters.getExtensions().size());
        assertEquals(PyPsaLoadFlowParameters.class, loadFlowParameters.getExtensionByName("PyPSALoadflowParameters").getClass());
        assertNotNull(loadFlowParameters.getExtension(PyPsaLoadFlowParameters.class));
        assertEquals(new PyPsaLoadFlowParameters().toString().trim(), loadFlowParameters.getExtension(PyPsaLoadFlowParameters.class).toString().trim());
        assertTrue(loadFlowParameters.getExtensionByName("PyPSALoadflowParameters") instanceof PyPsaLoadFlowParameters);
    }

    @Test
    public void testNoExtensions() {
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

        assertEquals(0, loadFlowParameters.getExtensions().size());
        assertFalse(loadFlowParameters.getExtensionByName("PyPSALoadflowParameters") instanceof PyPsaLoadFlowParameters);
        assertNull(loadFlowParameters.getExtension(PyPsaLoadFlowParameters.class));
    }
}
