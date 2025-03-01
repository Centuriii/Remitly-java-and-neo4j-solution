package com.remitly.neo4j.service;

import com.remitly.neo4j.dto.CountrySwiftCodesDTO;
import com.remitly.neo4j.dto.SwiftCodeCreateDTO;
import com.remitly.neo4j.dto.SwiftCodeDTO;
import com.remitly.neo4j.exception.CountryNotFoundException;
import com.remitly.neo4j.exception.SwiftCodeAlreadyExistsException;
import com.remitly.neo4j.exception.SwiftCodeNotFoundException;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class SwiftCodeService {
    private static final Logger log = LoggerFactory.getLogger(SwiftCodeService.class);

    @Autowired
    private Driver neo4jDriver;

    /**
     * Retrieves details for a specific SWIFT code.
     *
     * @param swiftCode The SWIFT code to look up
     * @return DTO with bank details and branch information if applicable
     * @throws SwiftCodeNotFoundException if the SWIFT code is not found
     */
    public SwiftCodeDTO getSwiftCodeDetails(String swiftCode) {
        log.info("Fetching details for SWIFT code: {}", swiftCode);

        try (Session session = neo4jDriver.session()) {
            boolean exists = session.executeRead(tx -> {
                var result = tx.run("MATCH (b:Bank {swiftCode: $swiftCode}) RETURN count(b) as count",
                        Map.of("swiftCode", swiftCode));
                return result.single().get("count").asLong() > 0;
            });

            if (!exists) {
                throw new SwiftCodeNotFoundException("SWIFT code not found: " + swiftCode);
            }

            return session.executeRead(tx -> {
                var result = tx.run(
                        "MATCH (b:Bank {swiftCode: $swiftCode})-[:LOCATED_IN]->(c:Country) "
                                + "RETURN b, c, b.type = 'HEADQUARTERS' as isHeadquarter",
                        Map.of("swiftCode", swiftCode));

                if (result.hasNext()) {
                    Record record = result.next();
                    Node bankNode = record.get("b").asNode();
                    Node countryNode = record.get("c").asNode();
                    boolean isHeadquarter = record.get("isHeadquarter").asBoolean();

                    SwiftCodeDTO dto = new SwiftCodeDTO();
                    dto.setAddress(bankNode.get("address").asString());
                    dto.setBankName(bankNode.get("name").asString());
                    dto.setCountryISO2(countryNode.get("iso2Code").asString());
                    dto.setCountryName(countryNode.get("name").asString());
                    dto.setHeadquarter(isHeadquarter);
                    dto.setSwiftCode(swiftCode);

                    if (isHeadquarter) {
                        dto.setBranches(getBranchesForHeadquarter(tx, swiftCode));
                    }

                    return dto;
                } else {
                    throw new SwiftCodeNotFoundException("SWIFT code not found: " + swiftCode);
                }
            });
        }
    }

    /**
     * Gets all branches for a headquarters SWIFT code.
     * 
     * @param tx                   The transaction context
     * @param headquarterSwiftCode The headquarters SWIFT code
     * @return List of branch DTOs
     */
    private List<SwiftCodeDTO.BranchDTO> getBranchesForHeadquarter(TransactionContext tx, String headquarterSwiftCode) {
        var result = tx.run(
                "MATCH (branch:Bank)-[:BRANCH_OF]->(hq:Bank {swiftCode: $swiftCode}) "
                        + "MATCH (branch)-[:LOCATED_IN]->(c:Country) " + "RETURN branch, c",
                Map.of("swiftCode", headquarterSwiftCode));

        if (!result.hasNext()) {
            return Collections.emptyList();
        }

        List<SwiftCodeDTO.BranchDTO> branches = new ArrayList<>();

        while (result.hasNext()) {
            Record record = result.next();
            Node branchNode = record.get("branch").asNode();
            Node countryNode = record.get("c").asNode();

            SwiftCodeDTO.BranchDTO branchDTO = new SwiftCodeDTO.BranchDTO();
            branchDTO.setAddress(branchNode.get("address").asString());
            branchDTO.setBankName(branchNode.get("name").asString());
            branchDTO.setCountryISO2(countryNode.get("iso2Code").asString());
            branchDTO.setHeadquarter(false); // Always false for branches
            branchDTO.setSwiftCode(branchNode.get("swiftCode").asString());

            branches.add(branchDTO);
        }

        return branches;
    }

    /**
     * Retrieves all SWIFT codes for a specific country.
     *
     * @param countryISO2 The ISO2 country code
     * @return DTO with country details and all its SWIFT codes
     * @throws CountryNotFoundException if the country is not found
     */
    public CountrySwiftCodesDTO getSwiftCodesByCountry(String countryISO2) {
        log.info("Fetching SWIFT codes for country ISO2 code: {}", countryISO2);

        String upperCaseCountryISO2 = countryISO2.toUpperCase();

        try (Session session = neo4jDriver.session()) {
            boolean exists = session.executeRead(tx -> {
                var result = tx.run("MATCH (c:Country {iso2Code: $iso2Code}) RETURN count(c) as count",
                        Map.of("iso2Code", upperCaseCountryISO2));
                return result.single().get("count").asLong() > 0;
            });

            if (!exists) {
                throw new CountryNotFoundException("Country not found with ISO2 code: " + upperCaseCountryISO2);
            }

            return session.executeRead(tx -> {
                var countryResult = tx.run("MATCH (c:Country {iso2Code: $iso2Code}) RETURN c",
                        Map.of("iso2Code", upperCaseCountryISO2));

                if (!countryResult.hasNext()) {
                    throw new CountryNotFoundException("Country not found with ISO2 code: " + upperCaseCountryISO2);
                }

                Record countryRecord = countryResult.next();
                Node countryNode = countryRecord.get("c").asNode();

                CountrySwiftCodesDTO dto = new CountrySwiftCodesDTO();
                dto.setCountryISO2(countryNode.get("iso2Code").asString());
                dto.setCountryName(countryNode.get("name").asString());

                var banksResult = tx.run(
                        "MATCH (b:Bank)-[:LOCATED_IN]->(c:Country {iso2Code: $iso2Code}) "
                                + "RETURN b, b.type = 'HEADQUARTERS' as isHeadquarter " + "ORDER BY b.name",
                        Map.of("iso2Code", upperCaseCountryISO2));

                List<CountrySwiftCodesDTO.SwiftCodeSummaryDTO> swiftCodes = new ArrayList<>();

                while (banksResult.hasNext()) {
                    Record bankRecord = banksResult.next();
                    Node bankNode = bankRecord.get("b").asNode();
                    boolean isHeadquarter = bankRecord.get("isHeadquarter").asBoolean();

                    CountrySwiftCodesDTO.SwiftCodeSummaryDTO swiftCode = new CountrySwiftCodesDTO.SwiftCodeSummaryDTO();
                    swiftCode.setAddress(bankNode.get("address").asString());
                    swiftCode.setBankName(bankNode.get("name").asString());
                    swiftCode.setCountryISO2(upperCaseCountryISO2);
                    swiftCode.setHeadquarter(isHeadquarter);
                    swiftCode.setSwiftCode(bankNode.get("swiftCode").asString());

                    swiftCodes.add(swiftCode);
                }

                dto.setSwiftCodes(swiftCodes);

                return dto;
            });
        }
    }

    /**
     * Creates a new SWIFT code entry in the database.
     *
     * @param createDTO The SWIFT code data to create
     * @return true if the SWIFT code was created successfully
     * @throws SwiftCodeAlreadyExistsException if the SWIFT code already exists
     */
    public boolean createSwiftCode(SwiftCodeCreateDTO createDTO) {
        log.info("Creating new SWIFT code: {}", createDTO.getSwiftCode());

        String swiftCode = createDTO.getSwiftCode();
        String countryISO2 = createDTO.getCountryISO2().toUpperCase();
        String countryName = createDTO.getCountryName().toUpperCase();

        try (Session session = neo4jDriver.session()) {
            boolean exists = session.executeRead(tx -> {
                var result = tx.run("MATCH (b:Bank {swiftCode: $swiftCode}) RETURN count(b) as count",
                        Map.of("swiftCode", swiftCode));
                return result.single().get("count").asLong() > 0;
            });

            if (exists) {
                throw new SwiftCodeAlreadyExistsException("SWIFT code already exists: " + swiftCode);
            }

            String bankType = createDTO.isHeadquarter() ? "HEADQUARTERS" : "BRANCH";

            return session.executeWrite(tx -> {
                tx.run("MERGE (c:Country {iso2Code: $iso2Code}) "
                        + "ON CREATE SET c.name = $name, c.timeZone = $timeZone " + "ON MATCH SET c.name = $name",
                        Map.of("iso2Code", countryISO2, "name", countryName, "timeZone", "UTC" // Default timezone if
                                                                                               // not provided
                ));

                tx.run("CREATE (b:Bank {swiftCode: $swiftCode}) " + "SET b.name = $name, " + "b.address = $address, "
                        + "b.type = $type, " + "b.codeType = $codeType",
                        Map.of("swiftCode", swiftCode, "name", createDTO.getBankName(), "address",
                                createDTO.getAddress(), "type", bankType, "codeType",
                                swiftCode.length() == 11 ? "BIC11" : "BIC8"));

                tx.run("MATCH (b:Bank {swiftCode: $swiftCode}) " + "MATCH (c:Country {iso2Code: $iso2Code}) "
                        + "MERGE (b)-[:LOCATED_IN]->(c)", Map.of("swiftCode", swiftCode, "iso2Code", countryISO2));

                if (!createDTO.isHeadquarter() && swiftCode.length() >= 8) {
                    String bic8 = swiftCode.substring(0, 8);
                    String hqSwiftCode = bic8 + "XXX";

                    var result = tx.run("MATCH (hq:Bank {swiftCode: $hqSwiftCode}) RETURN count(hq) as count",
                            Map.of("hqSwiftCode", hqSwiftCode));

                    if (result.single().get("count").asLong() > 0) {
                        tx.run("MATCH (branch:Bank {swiftCode: $branchSwiftCode}) "
                                + "MATCH (hq:Bank {swiftCode: $hqSwiftCode}) " + "MERGE (branch)-[:BRANCH_OF]->(hq)",
                                Map.of("branchSwiftCode", swiftCode, "hqSwiftCode", hqSwiftCode));
                    }
                }

                return true;
            });
        }
    }

    /**
     * Deletes a SWIFT code from the database.
     *
     * @param swiftCode The SWIFT code to delete
     * @return true if the SWIFT code was deleted successfully
     * @throws SwiftCodeNotFoundException if the SWIFT code is not found
     */
    public boolean deleteSwiftCode(String swiftCode) {
        log.info("Deleting SWIFT code: {}", swiftCode);

        try (Session session = neo4jDriver.session()) {
            boolean exists = session.executeRead(tx -> {
                var result = tx.run("MATCH (b:Bank {swiftCode: $swiftCode}) RETURN count(b) as count",
                        Map.of("swiftCode", swiftCode));
                return result.single().get("count").asLong() > 0;
            });

            if (!exists) {
                throw new SwiftCodeNotFoundException("SWIFT code not found: " + swiftCode);
            }

            return session.executeWrite(tx -> {
                var hasBranchesResult = tx.run("MATCH (b:Bank {swiftCode: $swiftCode})<-[:BRANCH_OF]-(branch:Bank) "
                        + "RETURN count(branch) as branchCount", Map.of("swiftCode", swiftCode));

                long branchCount = hasBranchesResult.single().get("branchCount").asLong();

                if (branchCount > 0) {
                    tx.run("MATCH (branch:Bank)-[r:BRANCH_OF]->(hq:Bank {swiftCode: $swiftCode}) " + "DELETE r",
                            Map.of("swiftCode", swiftCode));
                }

                var result = tx.run("MATCH (b:Bank {swiftCode: $swiftCode}) " + "OPTIONAL MATCH (b)-[r]-() "
                        + "DELETE r, b " + "RETURN count(b) as deleted", Map.of("swiftCode", swiftCode));

                return result.single().get("deleted").asLong() > 0;
            });
        }
    }
}