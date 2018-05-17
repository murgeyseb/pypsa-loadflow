/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.pypsa_loadflow;

import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import static org.junit.Assert.*;

public class NetworkExporterTest {
    @Test
    public void testGetFormat() {
        NetworkExporter exporter = new NetworkExporter();
        assertEquals("PyPSA", exporter.getFormat());
    }

    @Test
    public void testGetComment() {
        NetworkExporter exporter = new NetworkExporter();
        assertEquals("PyPSA CSV network exporter", exporter.getComment());
    }

    @Test
    public void testExport() throws IOException {
        Network network = Importers.loadNetwork("test_network.xiidm", getClass().getResourceAsStream("/test_network.xiidm"));
        NetworkExporter exporter = new NetworkExporter();
        MemDataSource dataSource = new MemDataSource();
        exporter.export(network, null, dataSource);

        testNetworkFile(dataSource, network);
        testSnapshotsFile(dataSource, network);
        testBusesFile(dataSource, network);
        testLinesFile(dataSource, network);
        testTransformersFile(dataSource, network);
        testLoadsFile(dataSource, network);
        testGeneratorsFile(dataSource, network);
        testShuntImpedancesFile(dataSource, network);
    }

    private void testNetworkFile(MemDataSource dataSource, Network network) throws IOException {
        assertTrue(dataSource.exists("network.csv"));

        try (Reader reader = new InputStreamReader(dataSource.newInputStream("network.csv"));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim());
        ) {

            assertTrue(csvParser.getHeaderMap().containsKey("name"));
            assertTrue(csvParser.getHeaderMap().containsKey("snapshots"));
            assertTrue(csvParser.getHeaderMap().containsKey("now"));
            assertTrue(csvParser.getHeaderMap().containsKey("pypsa_version"));

            List<CSVRecord> csvRecords = csvParser.getRecords();
            assertEquals(1, csvRecords.size());
            assertEquals(network.getId(), csvRecords.get(0).get("name"));
            assertEquals(String.format("['%s']", network.getStateManager().getWorkingStateId()), csvRecords.get(0).get("snapshots"));
            assertEquals(network.getStateManager().getWorkingStateId(), csvRecords.get(0).get("now"));
            assertEquals("0.13.0", csvRecords.get(0).get("pypsa_version"));
        }
    }

    private void testSnapshotsFile(MemDataSource dataSource, Network network) throws IOException {
        assertTrue(dataSource.exists("snapshots.csv"));

        try (Reader reader = new InputStreamReader(dataSource.newInputStream("snapshots.csv"));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim());
        ) {

            assertTrue(csvParser.getHeaderMap().containsKey("name"));

            List<CSVRecord> csvRecords = csvParser.getRecords();
            assertEquals(1, csvRecords.size());
            assertEquals(network.getStateManager().getWorkingStateId(), csvRecords.get(0).get("name"));
        }
    }

    private void testBusesFile(MemDataSource dataSource, Network network) throws IOException {
        assertTrue(dataSource.exists("buses.csv"));

        try (Reader reader = new InputStreamReader(dataSource.newInputStream("buses.csv"));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim());
        ) {

            assertTrue(csvParser.getHeaderMap().containsKey("name"));
            assertTrue(csvParser.getHeaderMap().containsKey("v_nom"));
            assertTrue(csvParser.getHeaderMap().containsKey("v_mag_pu_set"));

            List<CSVRecord> csvRecords = csvParser.getRecords();
            long expectedNumberOfBuses = network.getBusView().getBusStream().count() +
                    + network.getDanglingLineCount();
            assertEquals(expectedNumberOfBuses, csvRecords.size());
            network.getBusView().getBusStream().forEach(bus -> {
                assertEquals(1, csvRecords.stream().filter(record -> record.get("name").equals(bus.getId())).count());
                CSVRecord associatedRecord = csvRecords.stream().filter(record -> record.get("name").equals(bus.getId())).findFirst().get();
                assertEquals(bus.getVoltageLevel().getNominalV(), Float.valueOf(associatedRecord.get("v_nom")).floatValue(), 0.f);
            });
        }
    }

    private void testLinesFile(MemDataSource dataSource, Network network) throws IOException {
        assertTrue(dataSource.exists("lines.csv"));

        try (Reader reader = new InputStreamReader(dataSource.newInputStream("lines.csv"));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim());
        ) {
            assertTrue(csvParser.getHeaderMap().containsKey("name"));
            assertTrue(csvParser.getHeaderMap().containsKey("bus0"));
            assertTrue(csvParser.getHeaderMap().containsKey("bus1"));
            assertTrue(csvParser.getHeaderMap().containsKey("r"));
            assertTrue(csvParser.getHeaderMap().containsKey("x"));
            assertTrue(csvParser.getHeaderMap().containsKey("g"));
            assertTrue(csvParser.getHeaderMap().containsKey("b"));

            List<CSVRecord> csvRecords = csvParser.getRecords();
            int expectedNumberOfLines = network.getLineCount() + network.getDanglingLineCount();
            assertEquals(expectedNumberOfLines, csvRecords.size());
            network.getLineStream().forEach(line -> {
                assertEquals(1, csvRecords.stream().filter(record -> record.get("name").equals(line.getId())).count());
                CSVRecord associatedRecord = csvRecords.stream().filter(record -> record.get("name").equals(line.getId())).findFirst().get();
                assertEquals(line.getTerminal1().getBusView().getBus().getId(), associatedRecord.get("bus0"));
                assertEquals(line.getTerminal2().getBusView().getBus().getId(), associatedRecord.get("bus1"));
            });
        }
    }

    private void testTransformersFile(MemDataSource dataSource, Network network) throws IOException {
        assertTrue(dataSource.exists("transformers.csv"));

        try (Reader reader = new InputStreamReader(dataSource.newInputStream("transformers.csv"));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim());
        ) {
            assertTrue(csvParser.getHeaderMap().containsKey("name"));
            assertTrue(csvParser.getHeaderMap().containsKey("bus0"));
            assertTrue(csvParser.getHeaderMap().containsKey("bus1"));
            assertTrue(csvParser.getHeaderMap().containsKey("s_nom"));
            assertTrue(csvParser.getHeaderMap().containsKey("r"));
            assertTrue(csvParser.getHeaderMap().containsKey("x"));
            assertTrue(csvParser.getHeaderMap().containsKey("g"));
            assertTrue(csvParser.getHeaderMap().containsKey("b"));
            assertTrue(csvParser.getHeaderMap().containsKey("tap_side"));
            assertTrue(csvParser.getHeaderMap().containsKey("tap_ratio"));
            assertTrue(csvParser.getHeaderMap().containsKey("phase_shift"));

            List<CSVRecord> csvRecords = csvParser.getRecords();
            assertEquals(network.getTwoWindingsTransformerCount(), csvRecords.size());
            network.getTwoWindingsTransformerStream().forEach(twt -> {
                assertEquals(1, csvRecords.stream().filter(record -> record.get("name").equals(twt.getId())).count());
                CSVRecord associatedRecord = csvRecords.stream().filter(record -> record.get("name").equals(twt.getId())).findFirst().get();
                assertEquals(twt.getTerminal1().getBusView().getBus().getId(), associatedRecord.get("bus0"));
                assertEquals(twt.getTerminal2().getBusView().getBus().getId(), associatedRecord.get("bus1"));
            });
        }
    }

    private void testLoadsFile(MemDataSource dataSource, Network network) throws IOException {
        assertTrue(dataSource.exists("loads.csv"));

        try (Reader reader = new InputStreamReader(dataSource.newInputStream("loads.csv"));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim());
        ) {
            assertTrue(csvParser.getHeaderMap().containsKey("name"));
            assertTrue(csvParser.getHeaderMap().containsKey("bus"));
            assertTrue(csvParser.getHeaderMap().containsKey("p_set"));
            assertTrue(csvParser.getHeaderMap().containsKey("q_set"));

            List<CSVRecord> csvRecords = csvParser.getRecords();
            assertEquals(network.getLoadCount() + network.getDanglingLineCount(), csvRecords.size());
            network.getLoadStream().forEach(load -> {
                assertEquals(1, csvRecords.stream().filter(record -> record.get("name").equals(load.getId())).count());
                CSVRecord associatedRecord = csvRecords.stream().filter(record -> record.get("name").equals(load.getId())).findFirst().get();
                assertEquals(load.getTerminal().getBusView().getBus().getId(), associatedRecord.get("bus"));
            });
        }
    }

    private void testGeneratorsFile(MemDataSource dataSource, Network network) throws IOException {
        assertTrue(dataSource.exists("generators.csv"));

        try (Reader reader = new InputStreamReader(dataSource.newInputStream("generators.csv"));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim());
        ) {
            assertTrue(csvParser.getHeaderMap().containsKey("name"));
            assertTrue(csvParser.getHeaderMap().containsKey("bus"));
            assertTrue(csvParser.getHeaderMap().containsKey("p_set"));
            assertTrue(csvParser.getHeaderMap().containsKey("q_set"));
            assertTrue(csvParser.getHeaderMap().containsKey("control"));

            List<CSVRecord> csvRecords = csvParser.getRecords();
            assertEquals(network.getGeneratorCount(), csvRecords.size());
            network.getGeneratorStream().forEach(generator -> {
                assertEquals(1, csvRecords.stream().filter(record -> record.get("name").equals(generator.getId())).count());
                CSVRecord associatedRecord = csvRecords.stream().filter(record -> record.get("name").equals(generator.getId())).findFirst().get();
                assertEquals(generator.getTerminal().getBusView().getBus().getId(), associatedRecord.get("bus"));
            });
        }
    }

    private void testShuntImpedancesFile(MemDataSource dataSource, Network network) throws IOException {
        assertTrue(dataSource.exists("shunt_impedances.csv"));

        try (Reader reader = new InputStreamReader(dataSource.newInputStream("shunt_impedances.csv"));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim());
        ) {
            assertTrue(csvParser.getHeaderMap().containsKey("name"));
            assertTrue(csvParser.getHeaderMap().containsKey("bus"));
            assertTrue(csvParser.getHeaderMap().containsKey("b"));

            List<CSVRecord> csvRecords = csvParser.getRecords();
            assertEquals(network.getShuntCount(), csvRecords.size());
            network.getShuntStream().forEach(shunt -> {
                assertEquals(1, csvRecords.stream().filter(record -> record.get("name").equals(shunt.getId())).count());
                CSVRecord associatedRecord = csvRecords.stream().filter(record -> record.get("name").equals(shunt.getId())).findFirst().get();
                assertEquals(shunt.getTerminal().getBusView().getBus().getId(), associatedRecord.get("bus"));
            });
        }
    }
}