/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.pypsa_loadflow;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.commons.io.WorkingDirectory;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowResultImpl;
import jep.Jep;
import jep.JepException;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Loadflow computation implementation based on PyPSA
 * open source power flow
 *
 * @author Sebastien Murgey <sebastien.murgey at rte-france.com>
 */
public class PyPsaLoadFlow implements LoadFlow {
    private static final Logger LOGGER = LoggerFactory.getLogger(PyPsaLoadFlow.class);
    private static final String WORKING_DIR_PREFIX = "powsybl_pypsa_";
    private final Network network;
    private final ComputationManager computationManager;

    /**
     * Loadflow implementation constructor
     *
     * @param network the network to use as input for loadflow computation
     * @param computationManager the computation manager
     * @param priority priority of computation
     */
    public PyPsaLoadFlow(Network network, ComputationManager computationManager, int priority) {
        this.network = Objects.requireNonNull(network, "network is null");
        this.computationManager = Objects.requireNonNull(computationManager, "computation manager is null");
    }

    @Override
    public LoadFlowResult run(LoadFlowParameters loadFlowParameters) throws Exception {
        Objects.requireNonNull(loadFlowParameters);
        LOGGER.debug("Using parameters {}", loadFlowParameters);

        Path loadflowScriptPath;
        boolean debug;
        boolean dcMode;
        float relaxationCoeff;
        PyPsaLoadFlowParameters pyPsaParameters = loadFlowParameters.getExtension(PyPsaLoadFlowParameters.class);
        if (pyPsaParameters != null) {
            LOGGER.debug("Using PyPSA parameters {}", pyPsaParameters);
            loadflowScriptPath = pyPsaParameters.getLoadflowScriptPath();
            debug = pyPsaParameters.isDebugActivated();
            dcMode = pyPsaParameters.isDcLoadFlow();
            relaxationCoeff = pyPsaParameters.getRelaxationCoeff();
        } else {
            loadflowScriptPath = PyPsaLoadFlowParameters.DEFAULT_LOADFLOW_SCRIPT_PATH;
            debug = PyPsaLoadFlowParameters.DEFAULT_DEBUG_ACTIVATED;
            dcMode = PyPsaLoadFlowParameters.DEFAULT_DC_LOAD_FLOW;
            relaxationCoeff = PyPsaLoadFlowParameters.DEFAULT_RELAXATION_COEFF;
        }
        // Do we use a seed to initialize computation
        boolean useSeed = loadFlowParameters.getVoltageInitMode() == LoadFlowParameters.VoltageInitMode.DC_VALUES ||
                loadFlowParameters.getVoltageInitMode() == LoadFlowParameters.VoltageInitMode.PREVIOUS_VALUES;

        // Do we compute linear power flow to initialize computation
        boolean computeLpf = loadFlowParameters.getVoltageInitMode() == LoadFlowParameters.VoltageInitMode.DC_VALUES;

        boolean isOk = false;
        int numberOfIterations = 0;

        DateTime startTime = DateTime.now();
        try (WorkingDirectory workingDirectory = new WorkingDirectory(computationManager.getLocalDir(), WORKING_DIR_PREFIX, debug)) {
            DataSource dataSource = DataSourceUtil.createDataSource(workingDirectory.toPath(), "", null);

            LOGGER.debug("PyPSA CSV export in {}", workingDirectory.toPath());
            NetworkExporter networkExporter = new NetworkExporter();
            networkExporter.export(network, null, dataSource);
            try (Jep jep = new Jep(false)) {
                // The following two lines are necessary to be able
                // to initialize pypsa package through Jep
                jep.eval("import sys");
                jep.eval("sys.argv = ['pdm']");

                // Set script parameters
                jep.set("debug", debug);
                jep.set("dc_mode", dcMode);
                jep.set("relaxation_coeff", relaxationCoeff);
                jep.set("use_seed", useSeed);
                jep.set("compute_lpf", computeLpf);
                jep.set("input_directory", workingDirectory.toPath().toString());
                jep.set("output_directory", workingDirectory.toPath().resolve("outputs").toString());

                // Run script
                jep.runScript(loadflowScriptPath.toString());

                // Get script results
                isOk = Boolean.valueOf((String) jep.getValue("converged"));
                numberOfIterations = (Integer) jep.getValue("n_iter");
            } catch (JepException e) {
                throw new PowsyblException(e);
            }

            // Update network state if converged
            if (isOk) {
                NetworkUpdater networkUpdater = new NetworkUpdater(workingDirectory.toPath().resolve("outputs"));
                networkUpdater.updateNetworkState(network, dcMode);
            }
        }

        // Build result object
        Duration computationDuration = new Duration(startTime, DateTime.now());
        PeriodFormatter timeFormatter = new PeriodFormatterBuilder()
                .appendSecondsWithMillis()
                .toFormatter();
        Map<String, String> metrics = new HashMap<>();
        metrics.put("computation-time", timeFormatter.print(computationDuration.toPeriod()));
        metrics.put("number-of-iterations", Integer.toString(numberOfIterations));
        return new LoadFlowResultImpl(isOk, metrics, "");
    }

    @Override
    public CompletableFuture<LoadFlowResult> runAsync(String workingStateId, LoadFlowParameters parameters) {
        network.getStateManager().setWorkingState(workingStateId);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return run(parameters);
            } catch (Exception e) {
                throw new PowsyblException(e);
            }
        }, computationManager.getExecutor());
    }

    @Override
    public String getName() {
        return "PyPSA loadflow";
    }

    @Override
    public String getVersion() {
        return "0.13.0";
    }
}
