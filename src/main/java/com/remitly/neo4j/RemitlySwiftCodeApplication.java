package com.remitly.neo4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication
public class RemitlySwiftCodeApplication {

    @Autowired
    private CsvImportService csvImportService;

    public static void main(String[] args) {
        SpringApplication.run(RemitlySwiftCodeApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void importCsvDataAfterStartup() {
        if (Boolean.parseBoolean(System.getenv().getOrDefault("IMPORT_CSV_ON_STARTUP", "true"))) {
            csvImportService.importCsvData();
        }
    }
}