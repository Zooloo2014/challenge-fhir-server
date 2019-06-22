package com.example.fhirserverdemo.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.example.fhirserverdemo.service.DbObservationService;
import com.example.fhirserverdemo.service.NextSequence;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.dstu3.model.Observation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value=ApiController.class, secure=false)
public class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DbObservationService dbObservationService;

    @MockBean
    private NextSequence nextSequenceService;

    private FhirContext ctxDstu3 = FhirContext.forDstu3();
    private IParser parser = ctxDstu3.newJsonParser();
    private List<Observation> exampleObservation = new ArrayList<>();

    @Test
    public void testSearchById() throws Exception {

        exampleObservation.add(parser.parseResource(Observation.class, getFile("json/example.json")));

        Mockito.when(dbObservationService.findByPatientId("example", "vital-signs", null, null))
                .thenReturn(exampleObservation);

/*
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/fhir/Observations")
                .param("patient", "example")
                .param("category", "vital-signs")
                .accept(MediaType.APPLICATION_JSON_VALUE);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        System.out.println(result.getResponse());
*/

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/fhir/Observations/?patient=example&category=vital-signs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();
                //.andExpect(content().string(getFile("json/example.json")), is(getFile("json/example.json")));

        String expected = getFile("json/example.json");

        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), JSONCompareMode.LENIENT);

    }

/*
    @Test
    public void create1() {
    }
*/

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