package com.example.fhirserverdemo.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.dstu3.model.Observation;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class DbObservationServiceTest {

    private FhirContext ctxDstu3 = FhirContext.forDstu3();
    private IParser parser = ctxDstu3.newJsonParser();

    @MockBean
    private DbObservationService dbObservationService;

    @Test
    public void findByPatientId() {
        String expected = getFile("json/example.json");
        List<Observation> exampleObservations = new ArrayList<>();
        exampleObservations.add(parser.parseResource(Observation.class, expected));

        Mockito.when(dbObservationService.findByPatientId("example", "vital-signs", null, null))
                .thenReturn(exampleObservations);
    }

    private String getFile(String fileName) {
        String result;
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        try {
            result = new String(Files.readAllBytes(file.toPath()));
        } catch (IOException i) {
            throw new RuntimeException("Sample JSON for testing not found.");
        }
        return result;
    }

}