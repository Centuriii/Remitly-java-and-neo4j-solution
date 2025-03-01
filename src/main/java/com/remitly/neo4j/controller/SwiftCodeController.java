package com.remitly.neo4j.controller;

import com.remitly.neo4j.dto.CountrySwiftCodesDTO;
import com.remitly.neo4j.dto.MessageResponseDTO;
import com.remitly.neo4j.dto.SwiftCodeCreateDTO;
import com.remitly.neo4j.dto.SwiftCodeDTO;
import com.remitly.neo4j.exception.CountryNotFoundException;
import com.remitly.neo4j.exception.SwiftCodeAlreadyExistsException;
import com.remitly.neo4j.exception.SwiftCodeNotFoundException;
import com.remitly.neo4j.service.SwiftCodeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/swift-codes")
public class SwiftCodeController {

    @Autowired
    private SwiftCodeService swiftCodeService;

    /**
     * Retrieves details for a specific SWIFT code.
     *
     * @param swiftCode The SWIFT code to look up
     * @return Bank details and branch information if applicable
     */
    @GetMapping("/{swiftCode}")
    public ResponseEntity<SwiftCodeDTO> getSwiftCodeDetails(@PathVariable("swiftCode") String swiftCode) {
        try {
            SwiftCodeDTO swiftCodeDetails = swiftCodeService.getSwiftCodeDetails(swiftCode);
            return ResponseEntity.ok(swiftCodeDetails);
        } catch (SwiftCodeNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves all SWIFT codes for a specific country.
     *
     * @param countryISO2 The ISO2 country code
     * @return Country details and all its SWIFT codes
     */
    @GetMapping("/country/{countryISO2}")
    public ResponseEntity<CountrySwiftCodesDTO> getSwiftCodesByCountry(
            @PathVariable("countryISO2") String countryISO2) {
        try {
            CountrySwiftCodesDTO countrySwiftCodes = swiftCodeService.getSwiftCodesByCountry(countryISO2);
            return ResponseEntity.ok(countrySwiftCodes);
        } catch (CountryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Creates a new SWIFT code entry.
     *
     * @param createDTO The SWIFT code data to create
     * @return Success message
     */
    @PostMapping
    public ResponseEntity<MessageResponseDTO> createSwiftCode(@Valid @RequestBody SwiftCodeCreateDTO createDTO) {
        try {
            swiftCodeService.createSwiftCode(createDTO);
            MessageResponseDTO response = new MessageResponseDTO("SWIFT code created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SwiftCodeAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponseDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponseDTO("Failed to create SWIFT code: " + e.getMessage()));
        }
    }

    /**
     * Deletes a SWIFT code entry.
     *
     * @param swiftCode The SWIFT code to delete
     * @return Success message
     */
    @DeleteMapping("/{swiftCode}")
    public ResponseEntity<MessageResponseDTO> deleteSwiftCode(@PathVariable("swiftCode") String swiftCode) {
        try {
            boolean deleted = swiftCodeService.deleteSwiftCode(swiftCode);
            if (deleted) {
                MessageResponseDTO response = new MessageResponseDTO("SWIFT code deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new MessageResponseDTO("Failed to delete SWIFT code"));
            }
        } catch (SwiftCodeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponseDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponseDTO("Failed to delete SWIFT code: " + e.getMessage()));
        }
    }
}