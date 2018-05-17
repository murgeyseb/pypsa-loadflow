/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.pypsa_loadflow;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.iidm.export.Exporter;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * PyPSA network exporter.
 *
 * NetworkExporter does translate an IIDM network to
 * PyPSA input files defined in CSV formatted file.
 * It only export main synchronous component of the network
 *
 * @see <a href="https://www.pypsa.org/doc/import_export.html">PyPSA import/export format</a>
 *
 * @author Sebastien Murgey <sebastien.murgey at rte-france.com>
 */
@AutoService(Exporter.class)
public class NetworkExporter implements Exporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkExporter.class);
    private static final float EPSILON = 1e-3f;
    private static final float THRESHOLD_NOMINAL_V = 50;
    private static final float DEFAULT_LOAD_P = 0.f;
    private static final float DEFAULT_LOAD_Q = 0.f;
    private static final float DEFAULT_GENERATOR_P = 0.f;
    private static final float DEFAULT_GENERATOR_Q = 0.f;
    private Map<Bus, Float> targetVPerBus = new HashMap<>();
    private Bus slackBus;

    @Override
    public String getFormat() {
        return "PyPSA";
    }

    @Override
    public String getComment() {
        return "PyPSA CSV network exporter";
    }

    @Override
    public void export(Network network, Properties properties, DataSource dataSource) {
        if (network == null) {
            throw new IllegalArgumentException("network is null");
        }

        long startTime = System.currentTimeMillis();
        printNetwork(network, dataSource);
        LOGGER.debug("PyPSA CSV export done in {} ms", System.currentTimeMillis() - startTime);
    }

    /**
     * Internal pre-treatment that get bus voltage target
     * from generators target voltage and regulating terminal
     *
     * @param network the network object to fill voltage target mapping from
     */
    private void getTargetVPerBus(Network network) {
        network.getGeneratorStream().forEach(this::checkPVBus);
    }

    /**
     * Internal function that fills the bus target voltage map
     * for a given generator.
     *
     * In PyPSA, the target voltage is given per bus.
     * For the moment, distant regulation is not available in PyPSA.
     *
     * @param gen the generator to get voltage target from
     * @throws PowsyblException if distant regulation is expected
     */
    private void checkPVBus(Generator gen) {
        if (gen.getTerminal().getBusView().getBus() == null ||
                !gen.getTerminal().getBusView().getBus().isInMainSynchronousComponent()) {
            return;
        }
        if (gen.isVoltageRegulatorOn()) {
            if (gen.getRegulatingTerminal() != gen.getTerminal()) {
                throw new PowsyblException(String.format("Distant regulation of generator %s not available in PyPSA.", gen.getId()));
            }
            Bus controlledBus = gen.getRegulatingTerminal().getBusView().getBus();
            if (targetVPerBus.containsKey(controlledBus) &&
                Math.abs(targetVPerBus.get(controlledBus) - gen.getTargetV()) > EPSILON) {
                LOGGER.warn("Voltage target of generator {} ({}) not compatible with previously defined voltage target {}", gen.getId(), gen.getTargetV(), targetVPerBus.get(controlledBus));
                return;
            }
            targetVPerBus.put(controlledBus, gen.getTargetV());
        }
    }

    /**
     * Compute the slack node.
     *
     * The slack node for the computation is chosen such as it is a PV node.
     * It is chosen as the one with the highest number of connections,
     * on the highest voltage level
     */
    private void computeSlackNode() {
        final long[] currentConnectionNumber = {0};
        final float[] currentNominalVoltage = {0.f};
        targetVPerBus.keySet().stream().sorted(Comparator.comparing(Bus::getId)).forEach(bus -> {
            long busNumberOfConnection = bus.getLineStream().count() +
                    bus.getTwoWindingTransformerStream().count();
            float busNominalVoltage = bus.getVoltageLevel().getNominalV();
            if (busNominalVoltage - currentNominalVoltage[0] > THRESHOLD_NOMINAL_V ||
                    Math.abs(busNominalVoltage - currentNominalVoltage[0]) < THRESHOLD_NOMINAL_V && busNumberOfConnection > currentConnectionNumber[0]) {
                slackBus = bus;
                currentConnectionNumber[0] = busNumberOfConnection;
                currentNominalVoltage[0] = busNominalVoltage;
            }
        });
    }

    /**
     * Print bus information in the buses file for PyPSA
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#bus">PyPSA bus component information</a>
     *
     * @param bus the bus to print information from
     * @param busPrinter the CSV printer of buses file
     */
    private void printBus(Bus bus, CSVPrinter busPrinter) {
        if (!bus.isInMainSynchronousComponent()) {
            return;
        }
        try {
            busPrinter.printRecord(
                bus.getId(),
                bus.getVoltageLevel().getNominalV(),
                targetVPerBus.containsKey(bus) ? targetVPerBus.get(bus) / bus.getVoltageLevel().getNominalV() : 1.f
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Print line information in the lines file for PyPSA
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#line">PyPSA line component information</a>
     * @see <a href="https://www.pypsa.org/doc/power_flow.html#line-model">PyPSA line model description</a>
     *
     * @param line the line to print information from
     * @param linePrinter the CSV printer of lines file
     * @todo put correct correspondance in line modelisation between IIDM model and PyPSA model if available one day
     */
    private void printLine(Line line, CSVPrinter linePrinter) {
        if (line.getTerminal1().getBusView().getBus() == null ||
            line.getTerminal2().getBusView().getBus() == null ||
            !line.getTerminal1().getBusView().getBus().isInMainSynchronousComponent() ||
            !line.getTerminal2().getBusView().getBus().isInMainSynchronousComponent()) {
            return;
        }
        try {
            float r = line.getR();
            float x = line.getX();
            float g = line.getG1() + line.getG2();
            float b = line.getB1() + line.getB2();
            linePrinter.printRecord(
                line.getId(),
                line.getTerminal1().getBusView().getBus().getId(),
                line.getTerminal2().getBusView().getBus().getId(),
                r,
                x,
                g,
                b
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Print two windings transformer information in the transformers file for PyPSA
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#transformer">PyPSA transformer component information</a>
     * @see <a href="https://www.pypsa.org/doc/power_flow.html#transformer-model">PyPSA transformer model description</a>
     *
     * @param twt the two windings transformer to print information from
     * @param twtPrinter the CSV printer of transformers file
     * @todo put correct correspondance in transformer modelisation between IIDM model and PyPSA model if available one day
     */
    private void printTwt(TwoWindingsTransformer twt, CSVPrinter twtPrinter) {
        if (twt.getTerminal1().getBusView().getBus() == null ||
            twt.getTerminal2().getBusView().getBus() == null ||
            !twt.getTerminal1().getBusView().getBus().isInMainSynchronousComponent() ||
            !twt.getTerminal2().getBusView().getBus().isInMainSynchronousComponent()) {
            return;
        }
        try {
            float twtActualR = twt.getR() * (1 + (twt.getRatioTapChanger() != null ? twt.getRatioTapChanger().getCurrentStep().getR() / 100 : 0)
                    + (twt.getPhaseTapChanger() != null ? twt.getPhaseTapChanger().getCurrentStep().getR() / 100 : 0));
            float twtActualX = twt.getX() * (1 + (twt.getRatioTapChanger() != null ? twt.getRatioTapChanger().getCurrentStep().getX() / 100 : 0)
                    + (twt.getPhaseTapChanger() != null ? twt.getPhaseTapChanger().getCurrentStep().getX() / 100 : 0));
            float twtActualG = twt.getG() * (1 + (twt.getRatioTapChanger() != null ? twt.getRatioTapChanger().getCurrentStep().getG() / 100 : 0)
                    + (twt.getPhaseTapChanger() != null ? twt.getPhaseTapChanger().getCurrentStep().getG() / 100 : 0));
            float twtActualB = twt.getB() * (1 + (twt.getRatioTapChanger() != null ? twt.getRatioTapChanger().getCurrentStep().getB() / 100 : 0)
                    + (twt.getPhaseTapChanger() != null ? twt.getPhaseTapChanger().getCurrentStep().getB() / 100 : 0));
            float twtActualRho = (twt.getRatioTapChanger() != null ? twt.getRatioTapChanger().getCurrentStep().getRho() : 1)
                    * (twt.getPhaseTapChanger() != null ? twt.getPhaseTapChanger().getCurrentStep().getRho() : 1);
            float twtActualAlpha = twt.getPhaseTapChanger() != null ? twt.getPhaseTapChanger().getCurrentStep().getAlpha() : 0;
            float sbase = 100.f;
            float vbase = twt.getTerminal2().getVoltageLevel().getNominalV();
            twtPrinter.printRecord(
                twt.getId(),
                twt.getTerminal1().getBusView().getBus().getId(),
                twt.getTerminal2().getBusView().getBus().getId(),
                "pi",
                sbase,
                twtActualR * sbase / vbase / vbase,
                twtActualX * sbase / vbase / vbase,
                twtActualG / sbase * vbase * vbase,
                twtActualB / sbase * vbase * vbase,
                0,
                1 / twtActualRho,
                -twtActualAlpha
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Print load information in the loads file for PyPSA
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#load">PyPSA load component information</a>
     *
     * @param load the load to print information from
     * @param loadPrinter the CSV printer of loads file
     */
    private void printLoad(Load load, CSVPrinter loadPrinter) {
        // Init to be computed values to NaN
        load.getTerminal().setP(Float.NaN);
        load.getTerminal().setQ(Float.NaN);

        // Export to PyPSA Network
        if (load.getTerminal().getBusView().getBus() == null ||
                !load.getTerminal().getBusView().getBus().isInMainSynchronousComponent()) {
            return;
        }
        try {
            loadPrinter.printRecord(
                    load.getId(),
                    load.getTerminal().getBusView().getBus().getId(),
                    load.getP0(),
                    load.getQ0()
            );

            // For set-point equals to default values in PyPSA,
            // set directly the state variable, because default values are
            // not exported.
            // If modified during computation, they will be exported and
            // updater will do its job
            if (Math.abs(load.getP0() - DEFAULT_LOAD_P) < EPSILON) {
                load.getTerminal().setP(load.getP0());
            }
            if (Math.abs(load.getQ0() - DEFAULT_LOAD_Q) < EPSILON) {
                load.getTerminal().setQ(load.getQ0());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Print generator information in the generators file for PyPSA
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#generator">PyPSA generator component information</a>
     *
     * @param gen the generator to print information from
     * @param generatorPrinter the CSV printer of loads file
     */
    private void printGenerator(Generator gen, CSVPrinter generatorPrinter) {
        // Init to be computed values to NaN
        gen.getTerminal().setP(Float.NaN);
        gen.getTerminal().setQ(Float.NaN);

        // Export to PyPSA Network
        if (gen.getTerminal().getBusView().getBus() == null ||
                !gen.getTerminal().getBusView().getBus().isInMainSynchronousComponent()) {
            return;
        }
        try {
            String controlMode = "PQ";
            if (gen.getTerminal().getBusView().getBus().equals(slackBus)) {
                controlMode = "Slack";
            } else if (gen.isVoltageRegulatorOn()) {
                controlMode = "PV";
            }
            generatorPrinter.printRecord(
                    gen.getId(),
                    gen.getTerminal().getBusView().getBus().getId(),
                    gen.getTargetP(),
                    gen.getTargetQ(),
                    controlMode
            );

            // For set-point equals to default values in PyPSA,
            // set directly the state variable, because default values are
            // not exported.
            // If modified during computation, they will be exported and
            // updater will do its job
            if (Math.abs(gen.getTargetP() - DEFAULT_GENERATOR_P) < EPSILON) {
                gen.getTerminal().setP(-gen.getTargetP());
            }
            if (Math.abs(gen.getTargetQ() - DEFAULT_GENERATOR_Q) < EPSILON) {
                gen.getTerminal().setQ(-gen.getTargetQ());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Print dangling line information in the files for PyPSA
     *
     * A dangling line is a specific object that does not exist in PyPSA.
     * It has been converted into three elements:
     * <ul>
     *     <li>a bus that represents distant connection of the dangling line</li>
     *     <li>a load connected on the created bus</li>
     *     <li>a line that does the effective connection between the created bus and
     * the rest of the network</li>
     * </ul>
     *
     * Sides of the generated line are important because they will be used
     * as a convention for network variables update. The generated line will
     * be oriented from created bus to network connection bus of the dangling line.
     *
     * @param danglingLine the dangling line to print information from
     * @param busPrinter the CSV printer of buses file
     * @param linePrinter the CSV printer of lines file
     * @param loadPrinter the CSV printer of loads file
     * @todo put correct correspondance in line modelisation between IIDM model and PyPSA model if available one day
     */

    private void printDanglingLine(DanglingLine danglingLine, CSVPrinter busPrinter, CSVPrinter linePrinter, CSVPrinter loadPrinter) {
        if (danglingLine.getTerminal().getBusView().getBus() == null ||
            !danglingLine.getTerminal().getBusView().getBus().isInMainSynchronousComponent()) {
            return;
        }
        try {
            busPrinter.printRecord(
                danglingLine.getId(),
                danglingLine.getTerminal().getVoltageLevel().getNominalV(),
                1.f
            );
            // Side 2 is actual connection to network :
            // Do not change, important for updater !
            linePrinter.printRecord(
                danglingLine.getId(),
                danglingLine.getId(),
                danglingLine.getTerminal().getBusView().getBus().getId(),
                danglingLine.getR(),
                danglingLine.getX(),
                danglingLine.getG(),
                danglingLine.getB()
            );
            loadPrinter.printRecord(
                danglingLine.getId(),
                danglingLine.getId(),
                danglingLine.getP0(),
                danglingLine.getQ0()
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Print shunt information in the shunt_impedences file for PyPSA
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#shunt-impedance">PyPSA generator component information</a>
     *
     * @param shunt the shunt to print information from
     * @param shuntPrinter the CSV printer of shunt_impedences file
     */
    private void printShunt(ShuntCompensator shunt, CSVPrinter shuntPrinter) {
        if (shunt.getTerminal().getBusView().getBus() == null ||
            !shunt.getTerminal().getBusView().getBus().isInMainSynchronousComponent()) {
            return;
        }
        try {
            shuntPrinter.printRecord(
                shunt.getId(),
                shunt.getTerminal().getBusView().getBus(),
                shunt.getCurrentB()
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Print network information in CSV files for PyPSA
     *
     * @param network the network to print information from
     * @todo deal with lacking devices of the network (three windings transformers, HVDCs, and static var compensators
     */
    public void printNetwork(Network network, DataSource dataSource) {
        try (CSVPrinter networkPrinter = new CSVPrinter(new OutputStreamWriter(dataSource.newOutputStream("network.csv", false)),
                    CSVFormat.DEFAULT.withHeader("name", "snapshots", "now", "pypsa_version"));
            CSVPrinter snapshotPrinter = new CSVPrinter(new OutputStreamWriter(dataSource.newOutputStream("snapshots.csv", false)),
                    CSVFormat.DEFAULT.withHeader("name"));
            CSVPrinter busPrinter = new CSVPrinter(new OutputStreamWriter(dataSource.newOutputStream("buses.csv", false)),
                    CSVFormat.DEFAULT.withHeader("name", "v_nom", "v_mag_pu_set"));
            CSVPrinter linePrinter = new CSVPrinter(new OutputStreamWriter(dataSource.newOutputStream("lines.csv", false)),
                    CSVFormat.DEFAULT.withHeader("name", "bus0", "bus1", "r", "x", "g", "b"));
            CSVPrinter twtPrinter = new CSVPrinter(new OutputStreamWriter(dataSource.newOutputStream("transformers.csv", false)),
                    CSVFormat.DEFAULT.withHeader("name", "bus0", "bus1", "model", "s_nom", "r", "x", "g", "b", "tap_side", "tap_ratio", "phase_shift"));
            CSVPrinter loadPrinter = new CSVPrinter(new OutputStreamWriter(dataSource.newOutputStream("loads.csv", false)),
                    CSVFormat.DEFAULT.withHeader("name", "bus", "p_set", "q_set"));
            CSVPrinter generatorPrinter = new CSVPrinter(new OutputStreamWriter(dataSource.newOutputStream("generators.csv", false)),
                    CSVFormat.DEFAULT.withHeader("name", "bus", "p_set", "q_set", "control"));
            CSVPrinter shuntPrinter = new CSVPrinter(new OutputStreamWriter(dataSource.newOutputStream("shunt_impedances.csv", false)),
                    CSVFormat.DEFAULT.withHeader("name", "bus", "b"));
        ) {


            networkPrinter.printRecord(
                network.getId(),
                "['" + network.getStateManager().getWorkingStateId() + "']",
                network.getStateManager().getWorkingStateId(),
                "0.13.0" //TODO replace by actual version
            );
            snapshotPrinter.printRecord(network.getStateManager().getWorkingStateId());

            getTargetVPerBus(network);
            computeSlackNode();

            network.getBusView().getBusStream().forEach(bus -> printBus(bus, busPrinter));
            network.getLineStream().forEach(line -> printLine(line, linePrinter));
            network.getTwoWindingsTransformerStream().forEach(twt -> printTwt(twt, twtPrinter));
            network.getLoadStream().forEach(load -> printLoad(load, loadPrinter));
            network.getGeneratorStream().forEach(gen -> printGenerator(gen, generatorPrinter));
            network.getDanglingLineStream().forEach(danglingLine -> printDanglingLine(danglingLine, busPrinter, linePrinter, loadPrinter));
            network.getShuntStream().forEach(shunt -> printShunt(shunt, shuntPrinter));

            networkPrinter.flush();
            snapshotPrinter.flush();
            busPrinter.flush();
            linePrinter.flush();
            twtPrinter.flush();
            loadPrinter.flush();
            generatorPrinter.flush();
            shuntPrinter.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
