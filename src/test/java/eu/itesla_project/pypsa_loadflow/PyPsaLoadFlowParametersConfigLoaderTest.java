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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.file.FileSystem;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PyPsaLoadFlowParametersConfigLoaderTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void getExtensionName() throws Exception {
        PyPsaLoadFlowParametersConfigLoader configLoader = new PyPsaLoadFlowParametersConfigLoader();
        assertEquals("PyPSALoadflowParameters", configLoader.getExtensionName());
    }

    @Test
    public void getCategoryName() throws Exception {
        PyPsaLoadFlowParametersConfigLoader configLoader = new PyPsaLoadFlowParametersConfigLoader();
        assertEquals("loadflow-parameters", configLoader.getCategoryName());
    }

    @Test
    public void getExtensionClass() throws Exception {
        PyPsaLoadFlowParametersConfigLoader configLoader = new PyPsaLoadFlowParametersConfigLoader();
        assertEquals(PyPsaLoadFlowParameters.class, configLoader.getExtensionClass());
    }

    @Test
    public void checkThrowsWhenNullPlatformConfig() throws Exception {
        PyPsaLoadFlowParametersConfigLoader configLoader = new PyPsaLoadFlowParametersConfigLoader();
        exception.expect(NullPointerException.class);
        configLoader.load(null);
    }

    @Test
    public void load() throws Exception {
        try (FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix())) {
            InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);

            boolean debugActivated = true;
            boolean dcLoadFlow = true;
            float relaxationCoeff = 0.7f;

            MapModuleConfig mapModuleConfig = platformConfig.createModuleConfig("pypsa-default-loadflow-parameters");
            mapModuleConfig.setStringProperty("debugActivated", Objects.toString(debugActivated));
            mapModuleConfig.setStringProperty("dcLoadFlow", Objects.toString(dcLoadFlow));
            mapModuleConfig.setStringProperty("relaxationCoeff", Objects.toString(relaxationCoeff));

            PyPsaLoadFlowParametersConfigLoader configLoader = new PyPsaLoadFlowParametersConfigLoader();
            PyPsaLoadFlowParameters parameters = configLoader.load(platformConfig);

            assertEquals(debugActivated, parameters.isDebugActivated());
            assertEquals(dcLoadFlow, parameters.isDcLoadFlow());
            assertEquals(relaxationCoeff, parameters.getRelaxationCoeff(), 0f);
        }
    }

}
