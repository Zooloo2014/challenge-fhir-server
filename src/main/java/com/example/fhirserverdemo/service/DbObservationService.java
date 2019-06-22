package com.example.fhirserverdemo.service;

import org.hl7.fhir.dstu3.model.Observation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class DbObservationService {

    @Autowired
    private final MongoTemplate mongoTemplate;

    public DbObservationService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Observation save(Observation observation) {
        mongoTemplate.save(observation);
        return observation;
    }

    public List<Observation> getAllObservations() {
        return mongoTemplate.findAll(Observation.class);
    }

    public List<Observation> findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id.myStringValue").is(id));
        System.out.println(query.toString());
        return mongoTemplate.find(query, Observation.class, "observation");
    }

    public List<Observation> findByPatientId(String patientId, String category, String loinc, String dates) throws RuntimeException {
        Query query = new Query();
        query.addCriteria(Criteria.where("subject.reference.myStringValue").is("Patient/" + patientId));
        query.addCriteria(Criteria.where("category.coding.code.myStringValue").is(category));
        if (loinc != null) {
            query.addCriteria(Criteria.where("code.coding.code.myStringValue").is(category));
        }
        if (dates != null) {
            try {
                DateCriteriaList dcl = new DateCriteriaList(dates);
                for (DateCriteria dc: dcl.getList()) {
                    if (dc.getOperator().equals("ge")) {
                        query.addCriteria(Criteria.where("effective.myStringValue").gte(dc.getDateString()));
                    } else if (dc.getOperator().equals("lt")) {
                        query.addCriteria(Criteria.where("effective.myStringValue").lte(dc.getDateString()));
                    } else {
                        query.addCriteria(Criteria.where("effective.myStringValue").is(dc.getDateString()));
                    }
                }
            } catch (ParseException p) {
                throw new RuntimeException("Could not decode date query parameters");
            }
        }
        return mongoTemplate.find(query, Observation.class);
    }


    class DateCriteria implements Comparable  {
        private String operator;
        private Date date;
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        public DateCriteria(String item) throws ParseException {
            try {
                date = sdf.parse(item);
                operator = "";
            } catch (ParseException p) {
                operator = item.substring(0, 2).toLowerCase();

                try {
                    date = sdf.parse(item.substring(2));
                } catch (Exception e) {
                    throw e;
                }
            }
        }

        public String getOperator() {
            return operator;
        }

        public Date getDate() {
            return date;
        }

        public String getDateString() {
            return sdf.format(date);
        }

        @Override
        public int compareTo(Object o) {
            if (this.date == ((DateCriteria)o).getDate())
                return 0;
            else if (this.date.after(((DateCriteria)o).getDate()))
                return 1;
            else
                return -1;
        }
    }

    class DateCriteriaList {
        private List<DateCriteria> list;

        public DateCriteriaList(String params) throws ParseException {
            list = new ArrayList<>();
            String[] items = params.split(",");
            for (String s: items) {
                list.add(new DateCriteria(s));
            }
            Collections.sort(list);
        }

        public List<DateCriteria> getList() {
            return list;
        }
    }
}
