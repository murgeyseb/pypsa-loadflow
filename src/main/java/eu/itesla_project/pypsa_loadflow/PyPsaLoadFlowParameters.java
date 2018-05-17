/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.pypsa_loadflow;

import com.google.common.collect.ImmutableMap;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.loadflow.LoadFlowParameters;

import java.util.Objects;

/**
 * PyPSA loadflow additional parameters
 *
 * @author Sebastien Murgey <sebastien.murgey at rte-france.com>
 */
public class PyPsaLoadFlowParameters extends AbstractExtension<LoadFlowParameters> {
    public static final boolean DEFAULT_DEBUG_ACTIVATED = false;
    public static final boolean DEFAULT_DC_LOAD_FLOW = false;
    public static final float DEFAULT_RELAXATION_COEFF = 1f;

    private boolean debugActivated = DEFAULT_DEBUG_ACTIVATED;
    private boolean dcLoadFlow = DEFAULT_DC_LOAD_FLOW;
    private float relaxationCoeff = DEFAULT_RELAXATION_COEFF;

    @Override
    public String getName() {
        return "PyPSALoadflowParameters";
    }

    /**
     * Get a flag triggering the activation of debug mode
     *
     * While in debug mode, working directory generated for computation
     * is not removed at the end of the computation, and some extra consistency checks
     * are performed before powerflow computation by PyPSA.
     *
     * @return the flag triggering the activation of debug mode
     */
    public boolean isDebugActivated() {
        return debugActivated;
    }

    /**
     * Get a flag triggering the activation of debug mode.
     * Default value is false.
     *
     * @param debugActivated the flag to set, true is debug mode activated, false otherwise
     * @return this
     */
    public PyPsaLoadFlowParameters setDebugActivated(boolean debugActivated) {
        this.debugActivated = debugActivated;
        return this;
    }

    /**
     * Get a flag triggering if DC approximation is used for computation
     *
     * @return the flag triggering the activation of DC approximation
     */
    public boolean isDcLoadFlow() {
        return dcLoadFlow;
    }

    /**
     * Set a flag triggering if DC approximation is used for computation
     * Default value is false.
     *
     * @param dcLoadFlow the flag to set, true if DC approximation activated, false otherwise
     * @return this
     */
    public PyPsaLoadFlowParameters setDcLoadFlow(boolean dcLoadFlow) {
        this.dcLoadFlow = dcLoadFlow;
        return this;
    }

    /**
     * Get the coefficient used for SOR (Successive Over Relaxation)
     *
     * @return the coefficient for SOR
     */
    public float getRelaxationCoeff() {
        return relaxationCoeff;
    }

    /**
     * Set the coefficient used for SOR (Successive Over Relaxation)
     *
     * Must be positive. Values > 1 are used to speed up convergence of a slow-converging process,
     * while values < 1 are often used to help establish convergence of a diverging iterative process
     * or speed up the convergence of an overshooting process.
     * Default value is 1.
     *
     * @param relaxationCoeff the coefficient to set for SOR
     * @return this
     */
    public PyPsaLoadFlowParameters setRelaxationCoeff(float relaxationCoeff) {
        this.relaxationCoeff = relaxationCoeff;
        return this;
    }

    /**
     * Default constructor for PyPSA loadflow computation parameters
     */
    public PyPsaLoadFlowParameters() {
    }

    /**
     * Copy constructor for PyPSA loadflow computation parameters
     */
    public PyPsaLoadFlowParameters(PyPsaLoadFlowParameters other) {
        Objects.requireNonNull(other);

        this.debugActivated = other.debugActivated;
        this.dcLoadFlow = other.dcLoadFlow;
        this.relaxationCoeff = other.relaxationCoeff;
    }

    @Override
    public String toString() {
        ImmutableMap.Builder<String, Object> immutableMapBuilder = ImmutableMap.builder();
        immutableMapBuilder.put("debugActivated", debugActivated)
                .put("dcLoadFlow", dcLoadFlow)
                .put("relaxationCoeff", relaxationCoeff);

        return immutableMapBuilder.build().toString();
    }
}
