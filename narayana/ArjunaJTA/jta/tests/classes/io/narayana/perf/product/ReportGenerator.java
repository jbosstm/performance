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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ReportGenerator {
    private static final String BM_LINE_PATTERN = "(io.narayana.perf|i.n.p.p)";

    private Map<Long, Row> results = new TreeMap<>();

    private static void fatal(String msg) {
        System.err.printf("%s%n", msg);

        System.exit(1);
    }

    public static void main(String[] args) {//throws IOException {
        if (args.length == 0)
            fatal("syntax: GenerateReport <benchmark output file>");

        Path path = Paths.get(args[0]);
        Pattern pattern = Pattern.compile(BM_LINE_PATTERN);
        ReportGenerator report = new ReportGenerator();

        try (Stream<String> lines = Files.lines(path)) {
            Stream<String> data = lines.filter(pattern.asPredicate());

            data.forEach(report::processBenchmark);

            report.printOn(System.out);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            fatal(e.getMessage());
        }
    }

    public void printOn(PrintStream out) throws IOException, URISyntaxException {
        printPreamble(out);
        printTableHeader(out);
        printData(out);
    }

    private void printPreamble(PrintStream out) throws URISyntaxException, IOException {
        out.print(new String(Files.readAllBytes(Paths.get(getClass().getResource("/purpose.txt").toURI()))));
    }

    public void printTableHeader(PrintStream out) {
        out.printf("%7s", "Threads");
        for (String prod : getProducts(true))
            out.printf("%12s", prod);

        out.printf("%n");
    }

    public void printData(PrintStream out) {
        for (Row row : results.values())
            row.printOn(out);

        out.printf("%n");
    }

    public TreeSet<String> getProducts(boolean anonymize) {
        Iterator<Row> rows = results.values().iterator();
        TreeSet<String> products = new TreeSet<>();

        if (rows.hasNext()) {
            Row row = rows.next();
            int i = 0;

            for (String prod : row.getValues()) {
                if (anonymize && !"Narayana".equals(prod))
                    prod = getCharForNumber(++i);

                products.add(prod);
            }
        } else {
            products.add("No data");
        }

        return products;
    }

    private String getCharForNumber(int i) {
        return i > 0 && i < 27 ? String.valueOf((char)(i + 'A' - 1)) : null;
    }

    private void processBenchmark(String data) {
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

    private static class Row {
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
