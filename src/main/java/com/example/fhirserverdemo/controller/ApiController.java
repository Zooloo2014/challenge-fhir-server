package com.example.fhirserverdemo.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.example.fhirserverdemo.service.DbObservationService;
import com.example.fhirserverdemo.service.NextSequence;
import org.hl7.fhir.dstu3.model.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/fhir")
public class ApiController {

    private final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private final DbObservationService dbObservationService;

    @Autowired
    private NextSequence nextSequenceService;

    // Create a context for DSTU3
    private FhirContext ctxDstu3 = FhirContext.forDstu3();

    public ApiController(DbObservationService dbObservationService) {
        this.dbObservationService = dbObservationService;
    }

    @GetMapping(value="/Observations", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Observation>> search(@RequestParam(name="id", required=false) String id,
                                         @RequestParam(name="patient") String patientId,
                                         @RequestParam(name="category", defaultValue="vital-signs") String category,
                                         @RequestParam(name="code", required=false) String code,
                                         @RequestParam(name="date", required=false) String dates) {

        /*
         *       Get the requested data according to criteria
         * */
        List<Observation> observations;
        if (id != null) {   // Find by Observation id, for testing the insert
            observations = dbObservationService.findById(id);
        } else {            // Find by patient id & other criteria
            observations = dbObservationService.findByPatientId(patientId, category, code, dates);
        }

        /*
        *       Return as list of strings because Jackson has issues with this conversion to JSON
        * */
        IParser parser = ctxDstu3.newJsonParser();
        List<Observation> returnList = new ArrayList<>();

        for (Observation o: observations) {
            returnList.add(o);
        }

        return new ResponseEntity<>(returnList, HttpStatus.OK);
    }

    @PostMapping(value="/Observations", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> create(HttpServletRequest request,
                                         @RequestBody String observation,
                                         @RequestParam(name="format", required=false) String mimeType,
                                         UriComponentsBuilder builder) throws URISyntaxException {

        /*
         *       Parse the payload, set fields, save, setup return string
         *       (Jackson doesn't like the object format this is in...)
         * */
        long newId = nextSequenceService.getNextSequence("obs_id");

        IParser parser = ctxDstu3.newJsonParser();
        Observation newObservation = parser.parseResource(Observation.class, observation);

        newObservation.setId(Long.toString(newId));
        newObservation.setIssued(new Date());
        dbObservationService.save(newObservation);

        /*
         *       Set up Location header and body for return
         * */
        String returnObservation = parser.encodeResourceToString(newObservation);

        URI currLocation = new URI(request.getRequestURI());
        UriComponents uriComponents = builder.scheme(currLocation.getScheme())
                .host(currLocation.getHost())
//                .path("/Observations/{id}/_history/[vid]") // TODO
                .path(currLocation.getPath() + "/{id}")
                .buildAndExpand(newId);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setLocation(uriComponents.toUri());

        return new ResponseEntity<>(returnObservation, responseHeaders, HttpStatus.CREATED);
    }

}
