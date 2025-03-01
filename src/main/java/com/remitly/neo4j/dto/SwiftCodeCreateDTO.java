package com.remitly.neo4j.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SwiftCodeCreateDTO {
    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Country ISO2 code is required")
    @Size(min = 2, max = 2, message = "Country ISO2 code must be exactly 2 characters")
    private String countryISO2;

    @NotBlank(message = "Country name is required")
    private String countryName;

    private boolean isHeadquarter;

    @NotBlank(message = "SWIFT code is required")
    @Pattern(regexp = "^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$", 
             message = "SWIFT code must be either 8 or 11 characters, with first 6 characters being letters")
    private String swiftCode;
    
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getCountryISO2() {
        return countryISO2;
    }

    public void setCountryISO2(String countryISO2) {
        this.countryISO2 = countryISO2;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public boolean isHeadquarter() {
        return isHeadquarter;
    }

    public void setHeadquarter(boolean headquarter) {
        isHeadquarter = headquarter;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
    }
}