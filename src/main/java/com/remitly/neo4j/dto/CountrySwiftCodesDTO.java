package com.remitly.neo4j.dto;

import java.util.List;

public class CountrySwiftCodesDTO {
    private String countryISO2;
    private String countryName;
    private List<SwiftCodeSummaryDTO> swiftCodes;

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

    public List<SwiftCodeSummaryDTO> getSwiftCodes() {
        return swiftCodes;
    }

    public void setSwiftCodes(List<SwiftCodeSummaryDTO> swiftCodes) {
        this.swiftCodes = swiftCodes;
    }
    public static class SwiftCodeSummaryDTO {
        private String address;
        private String bankName;
        private String countryISO2;
        private boolean isHeadquarter;
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
}