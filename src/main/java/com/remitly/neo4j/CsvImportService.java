package com.remitly.neo4j;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class CsvImportService {
    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);
    private static final String CSV_FILE_PATH = System.getenv().getOrDefault("CSV_FILE_PATH", "banks.csv");

    @Autowired
    private Driver neo4jDriver;

    public void importCsvData() {
        log.info("Starting CSV data import");

        if (dataExists()) {
            log.info("Data already exists in the database. Skipping import.");
            return;
        }

        createConstraints();

        try (Reader reader = new FileReader(CSV_FILE_PATH);
                CSVParser csvParser = new CSVParser(reader,
                        CSVFormat.Builder.create().setHeader().setIgnoreHeaderCase(true).setTrim(true).build())) {

            int batchSize = 100;
            int recordCount = 0;
            int totalCount = 0;

            Set<String> countries = new HashSet<>();
            Map<String, Map<String, Object>> records = new HashMap<>();
            Map<String, String> headquarters = new HashMap<>();

            for (CSVRecord csvRecord : csvParser) {
                recordCount++;
                totalCount++;

                String iso2Code = csvRecord.get("COUNTRY ISO2 CODE").toUpperCase();
                String swiftCode = csvRecord.get("SWIFT CODE");
                String codeType = csvRecord.get("CODE TYPE");

                boolean isHeadquarters = swiftCode.endsWith("XXX");
                String bankType = isHeadquarters ? "HEADQUARTERS" : "BRANCH";

                if (isHeadquarters) {
                    String bic8 = swiftCode.substring(0, 8);
                    headquarters.put(bic8, swiftCode);
                }

                countries.add(iso2Code);

                Map<String, Object> record = new HashMap<>();
                record.put("swiftCode", swiftCode);
                record.put("codeType", codeType);
                record.put("name", csvRecord.get("NAME"));
                record.put("address", csvRecord.get("ADDRESS"));
                record.put("town", csvRecord.get("TOWN NAME"));
                record.put("countryName", csvRecord.get("COUNTRY NAME").toUpperCase());
                record.put("iso2Code", iso2Code);
                record.put("timeZone", csvRecord.get("TIME ZONE"));
                record.put("bankType", bankType);

                records.put(swiftCode, record);

                if (recordCount >= batchSize) {
                    processBatch(countries, records, headquarters);
                    log.info("Processed {} records (total: {})", recordCount, totalCount);

                    recordCount = 0;
                    countries = new HashSet<>();
                    records = new HashMap<>();
                }
            }

            if (!records.isEmpty()) {
                processBatch(countries, records, headquarters);
                log.info("Processed final batch of {} records (total: {})", records.size(), totalCount);
            }

            createBranchRelationships(headquarters);

            log.info("Completed importing {} total records", totalCount);

        } catch (IOException e) {
            log.error("Error reading CSV file", e);
            throw new RuntimeException("Failed to read CSV file", e);
        }
    }

    private boolean dataExists() {
        try (Session session = neo4jDriver.session()) {
            long count = session.executeRead(tx -> {
                var result = tx.run("MATCH (b:Bank) RETURN count(b) as count");
                if (result.hasNext()) {
                    return result.next().get("count").asLong();
                }
                return 0L;
            });
            return count > 0;
        }
    }

    private void createConstraints() {
        log.info("Creating schema constraints");

        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run("CREATE CONSTRAINT IF NOT EXISTS FOR (c:Country) REQUIRE c.iso2Code IS UNIQUE");
                return null;
            });

            session.executeWrite(tx -> {
                tx.run("CREATE CONSTRAINT IF NOT EXISTS FOR (b:Bank) REQUIRE b.swiftCode IS UNIQUE");
                return null;
            });

            session.executeWrite(tx -> {
                tx.run("CREATE INDEX bank_name IF NOT EXISTS FOR (b:Bank) ON (b.name)");
                return null;
            });

            session.executeWrite(tx -> {
                tx.run("CREATE INDEX bank_type IF NOT EXISTS FOR (b:Bank) ON (b.type)");
                return null;
            });

            session.executeWrite(tx -> {
                tx.run("CREATE INDEX country_name IF NOT EXISTS FOR (c:Country) ON (c.name)");
                return null;
            });

            log.info("Schema constraints and indexes created successfully");
        } catch (Neo4jException e) {
            log.error("Error creating schema constraints", e);
            throw e;
        }
    }

    private void processBatch(Set<String> countries, Map<String, Map<String, Object>> records,
            Map<String, String> headquarters) {
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                for (String iso2Code : countries) {
                    Map<String, Object> record = records.values().stream()
                            .filter(r -> iso2Code.equals(r.get("iso2Code"))).findFirst().orElseThrow();

                    tx.run("MERGE (c:Country {iso2Code: $iso2Code}) "
                            + "ON CREATE SET c.name = $name, c.timeZone = $timeZone",
                            Map.of("iso2Code", iso2Code, "name", record.get("countryName"), "timeZone",
                                    record.get("timeZone")));
                }
                return null;
            });

            session.executeWrite(tx -> {
                for (Map<String, Object> record : records.values()) {
                    String swiftCode = (String) record.get("swiftCode");

                    tx.run("MERGE (b:Bank {swiftCode: $swiftCode}) "
                            + "ON CREATE SET b.codeType = $codeType, b.name = $name, "
                            + "b.address = $address, b.town = $town, b.type = $bankType",
                            Map.of("swiftCode", swiftCode, "codeType", record.get("codeType"), "name",
                                    record.get("name"), "address", record.get("address"), "town", record.get("town"),
                                    "bankType", record.get("bankType")));

                    tx.run("MATCH (b:Bank {swiftCode: $swiftCode}) " + "MATCH (c:Country {iso2Code: $iso2Code}) "
                            + "MERGE (b)-[:LOCATED_IN]->(c)",
                            Map.of("swiftCode", swiftCode, "iso2Code", record.get("iso2Code")));
                }
                return null;
            });
        } catch (Neo4jException e) {
            log.error("Error processing batch", e);
            throw e;
        }
    }

    private void createBranchRelationships(Map<String, String> headquarters) {
        log.info("Creating BRANCH_OF relationships between branches and headquarters");
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run("MATCH (branch:Bank) " + "WHERE branch.type = 'BRANCH' "
                        + "WITH branch, substring(branch.swiftCode, 0, 8) AS bic8 " + "MATCH (hq:Bank) "
                        + "WHERE hq.type = 'HEADQUARTERS' AND substring(hq.swiftCode, 0, 8) = bic8 "
                        + "MERGE (branch)-[:BRANCH_OF]->(hq)", Map.of());
                return null;
            });
            log.info("Branch relationships created successfully");
        } catch (Neo4jException e) {
            log.error("Error creating branch relationships", e);
            throw e;
        }
    }
}