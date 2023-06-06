/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.jmh;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class CsvToJsonConverter {

    public static void main(String[] args) throws IOException {
        convert(args[0]);
    }

    public static void convert(final String csvFileName) throws IOException{
        String jsonFileName = csvFileName.substring(0, csvFileName.lastIndexOf('.')) + ".json";

        CsvSchema csvSchema = CsvSchema.emptySchema().withHeader();
        CsvMapper csvMapper = new CsvMapper();

        // Reading data from CSV file.
        List<Object> readAllData = csvMapper.reader(Map.class).with(csvSchema).readValues(new File(csvFileName)).readAll();
        ObjectMapper objMapper = new ObjectMapper();

        // Writing the data into JSON file
        objMapper.writeValue(new File(jsonFileName), readAllData);
    }

}