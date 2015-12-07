/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package io.narayana.perf.product;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GenerateReport {
    private static final String BM_LINE_PATTERN = "(io.narayana.perf|i.n.p.p)";

    private static void fatal(String msg) {
        System.err.printf("%s%n", msg);

        System.exit(1);
    }

    public static void main(String[] args) {//throws IOException {
        if (args.length == 0)
            fatal("syntax: GenerateReport <benchmark output file>");

        Path path = Paths.get(args[0]);

        Pattern pattern = Pattern.compile(BM_LINE_PATTERN);
        Map<Long, Row> results = new TreeMap<>();

        try (Stream<String> lines = Files.lines(path)) {
            Stream<String> data = lines.filter(pattern.asPredicate());

            data.forEach(item -> processBenchmark(results, item));

            if (results.size() == 0)
                fatal("No matching benchmarks");
        } catch (IOException e) {
            e.printStackTrace();
            fatal(e.getMessage());
        }

        Iterator<Row> rows = results.values().iterator();
        Row row = rows.next(); // must be at least one

        System.out.printf("%7s", "Threads");
        for (String prod : row.getValues()) {
            System.out.printf("%12s", prod);
        }

        System.out.printf("%n");

        while (true) {
            row.printOn(System.out);
            if (!rows.hasNext())
                break;
            row = rows.next();
        }
    }

    private static void processBenchmark(Map<Long, Row> results, String data) {
        // pick out fields 0, 2 and 4
        String[] fields = data.split(",");
        if (fields.length == 7) {
            Optional pName = Arrays.stream(fields[0].split("\\.")).filter(name -> name.contains("Comparison")).findFirst();

            if (!pName.isPresent())
                return;

            Long tcnt = Long.parseLong(fields[2]);
            Long tput = (long)Double.parseDouble(fields[4]);
            Row row = results.get(tcnt);
            String prod = (String) pName.get();

            if (row == null) {
                row = new Row(tcnt);
                results.put(tcnt, row);
            }

            row.addColumn(prod.substring(0, prod.length() - "Comparison".length()), tput);
        }
    }

    static class Row {
        Long threadCnt;
        Map<String, Long> opsPerSecond;

        Row(Long threadCnt) {
            this.threadCnt = threadCnt;
            opsPerSecond = new TreeMap<>();
        }

        void addColumn(String product, Long tput) {
            opsPerSecond.put(product, tput);
        }

        Set<String> getValues() {
            return opsPerSecond.keySet();
        }

        void printOn(PrintStream out) {
            out.printf("%7d", threadCnt);

            for (Long tput : opsPerSecond.values())
                out.printf("%12d", tput);

            out.printf("%n");
        }
    }
}
