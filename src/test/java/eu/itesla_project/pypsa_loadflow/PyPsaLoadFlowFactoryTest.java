/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.pypsa_loadflow;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PyPsaLoadFlowFactoryTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testCreate() {
        Network network = Mockito.mock(Network.class);
        ComputationManager computationManager = Mockito.mock(ComputationManager.class);

        LoadFlowFactory factory = new PyPsaLoadFlowFactory();

        LoadFlow loadFlow = factory.create(network, computationManager, 0);
        assertTrue(loadFlow instanceof PyPsaLoadFlow);
    }

    @Test
    public void checkFailsWhenNullNetwork() {
        ComputationManager computationManager = Mockito.mock(ComputationManager.class);

        LoadFlowFactory factory = new PyPsaLoadFlowFactory();
        exception.expect(NullPointerException.class);
        factory.create(null, computationManager, 0);
    }

    @Test
    public void checkFailsWhenNullComputationManager() {
        Network network = Mockito.mock(Network.class);

        LoadFlowFactory factory = new PyPsaLoadFlowFactory();
        exception.expect(NullPointerException.class);
        factory.create(network, null, 0);
    }
}
