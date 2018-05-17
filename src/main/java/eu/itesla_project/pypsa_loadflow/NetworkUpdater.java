/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.pypsa_loadflow;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.function.BiConsumer;

/**
 * PyPSA network importer.
 *
 * NetworkImporter does extract state variables of the network
 * from PyPSA output files defined in CSV formatted file.
 * It then updates the states variables of the associated IIDM network
 *
 * @see <a href="https://www.pypsa.org/doc/import_export.html">PyPSA import/export format</a>
 *
 * @author Sebastien Murgey <sebastien.murgey at rte-france.com>
 */
public class NetworkUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkUpdater.class);
    private final Path inputPath;
    private Bus slackBus;

    /**
     * Importer constructor
     *
     * @param inputPath the path of the directory where to retrieve CSV files
     */
    public NetworkUpdater(Path inputPath) {
        this.inputPath = inputPath;
    }


    /**
     * Utility function suited for parsing generators CSV file to get Slack node
     * information.
     *
     * @param filename the name of the file to be parsed
     * @param network the network to put extracted information in
     */
    private void readSlackNode(String filename, Network network) {
        try (Reader reader = Files.newBufferedReader(inputPath.resolve(filename));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim());
        ) {
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();

            for (CSVRecord csvRecord : csvRecords) {
                if (csvRecord.get("control").equals("Slack")) {
                    slackBus = network.getGenerator(csvRecord.get("name")).getRegulatingTerminal().getBusView().getBus();
                    break;
                }
            }
        } catch (NoSuchFileException e) {
            LOGGER.warn("Output file {} not generated: could be an error...", filename);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Utility function suited for parsing many PyPSA CSV file with given name in the input directory
     * and call a handler method to deal with each record.
     *
     * @param filename the name of the file to be parsed
     * @param network the network to put extracted information in
     * @param handler the method handler to call on each record
     */
    private void readCsvFileWithHandler(String filename, Network network, BiConsumer<Network, CSVRecord> handler) {
        try (Reader reader = Files.newBufferedReader(inputPath.resolve(filename));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim());
        ) {
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();

            for (CSVRecord csvRecord : csvRecords) {
                if (!csvRecord.get("name").equals(network.getStateManager().getWorkingStateId())) {
                    continue;
                }
                handler.accept(network, csvRecord);
            }
        } catch (NoSuchFileException e) {
            LOGGER.warn("Output file {} not generated: could be an error...", filename);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Handler method to deal with buses voltage angle file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#bus">PyPSA bus component information</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerBusesAngle(Network network, CSVRecord csvRecord) {
        network.getBusView().getBusStream().forEach(bus -> {
            if (csvRecord.isMapped(bus.getId())) {
                float angleRadians = Float.parseFloat(csvRecord.get(bus.getId()));
                bus.setAngle((float) Math.toDegrees(angleRadians));
            } else if (bus == slackBus) {
                bus.setAngle(0.f);
            } else {
                bus.setAngle(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with buses voltage magnitude file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#bus">PyPSA bus component information</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerBusesVoltage(Network network, CSVRecord csvRecord) {
        network.getBusView().getBusStream().forEach(bus -> {
            if (csvRecord.isMapped(bus.getId())) {
                float voltagePu = Float.parseFloat(csvRecord.get(bus.getId()));
                bus.setV(voltagePu * bus.getVoltageLevel().getNominalV());
            } else {
                bus.setV(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with generators active power file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#generator">PyPSA generator component information</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerGeneratorsP(Network network, CSVRecord csvRecord) {
        network.getGeneratorStream().forEach(generator -> {
            if (csvRecord.isMapped(generator.getId())) {
                generator.getTerminal().setP(-Float.valueOf(csvRecord.get(generator.getId())));
            } else {
                generator.getTerminal().setP(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with generators reactive power file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#generator">PyPSA generator component information</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerGeneratorsQ(Network network, CSVRecord csvRecord) {
        network.getGeneratorStream().forEach(generator -> {
            if (csvRecord.isMapped(generator.getId())) {
                generator.getTerminal().setQ(-Float.valueOf(csvRecord.get(generator.getId())));
            } else {
                generator.getTerminal().setQ(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with loads active power file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#load">PyPSA load component information</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerLoadsP(Network network, CSVRecord csvRecord) {
        network.getLoadStream().forEach(load -> {
            if (csvRecord.isMapped(load.getId())) {
                load.getTerminal().setP(Float.valueOf(csvRecord.get(load.getId())));
            } else {
                load.getTerminal().setP(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with loads reactive power file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#load">PyPSA load component information</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerLoadsQ(Network network, CSVRecord csvRecord) {
        network.getLoadStream().forEach(load -> {
            if (csvRecord.isMapped(load.getId())) {
                load.getTerminal().setQ(Float.valueOf(csvRecord.get(load.getId())));
            } else {
                load.getTerminal().setQ(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with line active power on side 0 file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#line">PyPSA line component information</a>
     * @see <a href="https://www.pypsa.org/doc/power_flow.html#line-model">PyPSA line model description</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerLinesP0(Network network, CSVRecord csvRecord) {
        network.getLineStream().forEach(line -> {
            if (csvRecord.isMapped(line.getId())) {
                line.getTerminal1().setP(Float.valueOf(csvRecord.get(line.getId())));
            } else {
                line.getTerminal1().setP(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with line active power on side 1 file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#line">PyPSA line component information</a>
     * @see <a href="https://www.pypsa.org/doc/power_flow.html#line-model">PyPSA line model description</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerLinesP1(Network network, CSVRecord csvRecord) {
        network.getLineStream().forEach(line -> {
            if (csvRecord.isMapped(line.getId())) {
                line.getTerminal2().setP(Float.valueOf(csvRecord.get(line.getId())));
            } else {
                line.getTerminal2().setP(Float.NaN);
            }
        });
        network.getDanglingLineStream().forEach(danglingLine -> {
            if (csvRecord.isMapped(danglingLine.getId())) {
                danglingLine.getTerminal().setP(Float.valueOf(csvRecord.get(danglingLine.getId())));
            } else {
                danglingLine.getTerminal().setP(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with line reactive power on side 0 file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#line">PyPSA line component information</a>
     * @see <a href="https://www.pypsa.org/doc/power_flow.html#line-model">PyPSA line model description</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerLinesQ0(Network network, CSVRecord csvRecord) {
        network.getLineStream().forEach(line -> {
            if (csvRecord.isMapped(line.getId())) {
                line.getTerminal1().setQ(Float.valueOf(csvRecord.get(line.getId())));
            } else {
                line.getTerminal1().setQ(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with line reactive power on side 1 file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#line">PyPSA line component information</a>
     * @see <a href="https://www.pypsa.org/doc/power_flow.html#line-model">PyPSA line model description</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */

    private void handlerLinesQ1(Network network, CSVRecord csvRecord) {
        network.getLineStream().forEach(line -> {
            if (csvRecord.isMapped(line.getId())) {
                line.getTerminal2().setQ(Float.valueOf(csvRecord.get(line.getId())));
            } else {
                line.getTerminal2().setQ(Float.NaN);
            }
        });
        network.getDanglingLineStream().forEach(danglingLine -> {
            if (csvRecord.isMapped(danglingLine.getId())) {
                danglingLine.getTerminal().setQ(Float.valueOf(csvRecord.get(danglingLine.getId())));
            } else {
                danglingLine.getTerminal().setQ(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with transformer active power on side 0 file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#transformer">PyPSA transformer component information</a>
     * @see <a href="https://www.pypsa.org/doc/power_flow.html#transformer-model">PyPSA transformer model description</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerTransformersP0(Network network, CSVRecord csvRecord) {
        network.getTwoWindingsTransformerStream().forEach(twt -> {
            if (csvRecord.isMapped(twt.getId())) {
                twt.getTerminal1().setP(Float.valueOf(csvRecord.get(twt.getId())));
            } else {
                twt.getTerminal1().setP(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with transformer active power on side 1 file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#transformer">PyPSA transformer component information</a>
     * @see <a href="https://www.pypsa.org/doc/power_flow.html#transformer-model">PyPSA transformer model description</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerTransformersP1(Network network, CSVRecord csvRecord) {
        network.getTwoWindingsTransformerStream().forEach(twt -> {
            if (csvRecord.isMapped(twt.getId())) {
                twt.getTerminal2().setP(Float.valueOf(csvRecord.get(twt.getId())));
            } else {
                twt.getTerminal2().setP(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with transformer reactive power on side 0 file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#transformer">PyPSA transformer component information</a>
     * @see <a href="https://www.pypsa.org/doc/power_flow.html#transformer-model">PyPSA transformer model description</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerTransformersQ0(Network network, CSVRecord csvRecord) {
        network.getTwoWindingsTransformerStream().forEach(twt -> {
            if (csvRecord.isMapped(twt.getId())) {
                twt.getTerminal1().setQ(Float.valueOf(csvRecord.get(twt.getId())));
            } else {
                twt.getTerminal1().setQ(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with transformer reactive power on side 1 file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#transformer">PyPSA transformer component information</a>
     * @see <a href="https://www.pypsa.org/doc/power_flow.html#transformer-model">PyPSA transformer model description</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerTransformersQ1(Network network, CSVRecord csvRecord) {
        network.getTwoWindingsTransformerStream().forEach(twt -> {
            if (csvRecord.isMapped(twt.getId())) {
                twt.getTerminal2().setQ(Float.valueOf(csvRecord.get(twt.getId())));
            } else {
                twt.getTerminal2().setQ(Float.NaN);
            }
        });
    }

    /**
     * Handler method to deal with shunt impedances reactive power file
     *
     * @see <a href="https://www.pypsa.org/doc/components.html#shunt-impedance">PyPSA shunt impedance component information</a>
     *
     * @param network the network to put extracted information in
     * @param csvRecord the CSV record to parse
     */
    private void handlerShuntImpedancesQ(Network network, CSVRecord csvRecord) {
        network.getShuntStream().forEach(shunt -> {
            if (csvRecord.isMapped(shunt.getId())) {
                shunt.getTerminal().setQ(-Float.valueOf(csvRecord.get(shunt.getId())));
            }
        });
    }

    /**
     * Update network information from CSV files generated by PyPSA
     *
     * @param network the network to put extracted information in
     * @todo deal with lacking devices of the network (three windings transformers, HVDCs, and static var compensators)
     */
    public void updateNetworkState(Network network, boolean dcLoadFlow) {
        // Get slack node
        readSlackNode("generators.csv", network);

        // Solve issue with loads reactive power not exported
        network.getLoadStream().forEach(load -> load.getTerminal().setQ(load.getQ0()));

        // Import state variables
        readCsvFileWithHandler("buses-v_ang.csv", network, this::handlerBusesAngle);
        readCsvFileWithHandler("buses-v_mag_pu.csv", network, this::handlerBusesVoltage);
        readCsvFileWithHandler("generators-p.csv", network, this::handlerGeneratorsP);
        readCsvFileWithHandler("loads-p.csv", network, this::handlerLoadsP);
        readCsvFileWithHandler("lines-p0.csv", network, this::handlerLinesP0);
        readCsvFileWithHandler("lines-p1.csv", network, this::handlerLinesP1);
        readCsvFileWithHandler("transformers-p0.csv", network, this::handlerTransformersP0);
        readCsvFileWithHandler("transformers-p1.csv", network, this::handlerTransformersP1);
        if (!dcLoadFlow) {
            readCsvFileWithHandler("generators-q.csv", network, this::handlerGeneratorsQ);
            readCsvFileWithHandler("loads-q.csv", network, this::handlerLoadsQ);
            readCsvFileWithHandler("lines-q0.csv", network, this::handlerLinesQ0);
            readCsvFileWithHandler("lines-q1.csv", network, this::handlerLinesQ1);
            readCsvFileWithHandler("transformers-q0.csv", network, this::handlerTransformersQ0);
            readCsvFileWithHandler("transformers-q1.csv", network, this::handlerTransformersQ1);
            readCsvFileWithHandler("shunt_impedances-q.csv", network, this::handlerShuntImpedancesQ);
        }
    }
}
