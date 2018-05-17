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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PyPsaLoadFlowTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void checkFailsWhenNullNetwork() {
        exception.expect(NullPointerException.class);
        new PyPsaLoadFlow(
            null,
            Mockito.mock(ComputationManager.class),
            0
        );
    }

    @Test
    public void checkFailsWhenNullComputationManager() {
        exception.expect(NullPointerException.class);
        new PyPsaLoadFlow(
                Mockito.mock(Network.class),
                null,
                0
        );
    }

    @Test
    public void getName() {
        LoadFlow computation = new PyPsaLoadFlow(
            Mockito.mock(Network.class),
            Mockito.mock(ComputationManager.class),
            0
        );
        assertEquals("PyPSA loadflow", computation.getName());
    }

    @Test
    public void getVersion() {
        LoadFlow computation = new PyPsaLoadFlow(
            Mockito.mock(Network.class),
            Mockito.mock(ComputationManager.class),
            0
        );
        assertNotNull(computation.getVersion());
        assertEquals("0.13.0", computation.getVersion());
    }
}
