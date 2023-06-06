/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package io.narayana.perf.product;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ReportGenerator {
    private static String BM_LINE_PATTERN = "(io.narayana.perf|i.n.p.p)";
    private static final String CHART_TITLE = "Parallelism Performance Comparison";
    private static final String XAXIS_LABEL = "Number of Threads";
    private static final String YAXIS_LABEL = "Transactions / sec";//"Normalised Transactions / sec";
    private static String PATTERN2="Comparison";

    private Map<Long, Row> results = new TreeMap<>();

    private static void fatal(String msg) {
        System.err.printf("%s%n", msg);

        System.exit(1);
    }

    public static void main(String[] args) {
        System.out.println(System.getProperty("user.dir"));
        if (args.length == 0)
            fatal("syntax: GenerateReport <benchmark output file> [override pattern: " +  BM_LINE_PATTERN + " and: " + PATTERN2 + "]");

        Path path = Paths.get(args[0]);
        if (args.length > 1) {
            BM_LINE_PATTERN = args[1];
            PATTERN2 = args[2];
        }
        Pattern pattern = Pattern.compile(BM_LINE_PATTERN);
        ReportGenerator report = new ReportGenerator();
        boolean anonymize = Boolean.valueOf(System.getProperty("anonymize", "true"));

        try (Stream<String> lines = Files.lines(path)) {
            Stream<String> data = lines.filter(pattern.asPredicate());

            data.forEach(report::processBenchmark);

            report.printOn(System.out, anonymize);
//            report.normalizeData();
            report.generateChart(anonymize, "benchmark.png", "png");
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            fatal(e.getMessage());
        }
    }

    public void printOn(PrintStream out, boolean anonymize) throws IOException, URISyntaxException {
        printPreamble(out);
        printTableHeader(out, anonymize);
        printData(out);
    }

    private void printPreamble(PrintStream out) throws URISyntaxException, IOException {
        out.print(new String(Files.readAllBytes(Paths.get(getClass().getResource("/purpose.txt").toURI()))));
    }

    public void printTableHeader(PrintStream out, boolean anonymize) {
        out.printf("%7s", "Threads");

        for (String prod : getProducts(anonymize))
            out.printf("%12s", prod);

        out.printf("%n");
    }

    public void printData(PrintStream out) {
        for (Row row : results.values())
            row.printOn(out);

        out.printf("%n");
    }

    public TreeSet<String> getProducts(boolean anonymize) {
        TreeSet<String> products = new TreeSet<>();

        for (Row row : results.values()) {
            int i = 0;

            for (String prod : row.getProductNames()) {
                if (anonymize && !"Narayana".equals(prod))
                    prod = getCharForNumber(++i);

                products.add(prod);
            }
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
            // first field contains the name of the class that performs the comparison
            Optional productName = Arrays.stream(fields[0].split("\\.")).filter(name -> name.contains(PATTERN2)).findFirst();

            if (!productName.isPresent())
                return;

            Long tcnt = Long.parseLong(fields[2]); // the number of threads used to generate the benchmark
            Long tput = (long)Double.parseDouble(fields[4]); // the throughput
            Row row = results.get(tcnt);
            String prod = (String) productName.get();

            if (row == null) {
                row = new Row(tcnt);
                results.put(tcnt, row);
            }

            // the class names follow the format: <product>Comparison
            row.addColumn(prod.substring(0, prod.length() - "Comparison".length()), tput.doubleValue());
        }
    }

    private void generateChart(boolean anonymize, String imageFileLocation, String imageFormat) throws IOException {
        BarChart chart = new BarChart();

        for (Row row : results.values()) {
            String threadCnt = String.valueOf(row.getThreadCnt());
            int i = 0;

            for (Map.Entry<String, Double> datum : row.getOpsPerSecond().entrySet()) {
                String prod = datum.getKey();

                if (anonymize && !"Narayana".equals(prod))
                    prod = getCharForNumber(++i);

                chart.addDataPoint(datum.getValue(), prod, threadCnt);
            }
        }

        chart.generateImage(imageFileLocation, imageFormat, CHART_TITLE, XAXIS_LABEL, YAXIS_LABEL);
    }

    private void normalizeData() {
       for (Row row : results.values()) {
           row.normalizeData();
       }
    }

    private static class Row {
        Long threadCnt;
        Map<String, Double> opsPerSecond;
        Double maxTput = 0.0;
        Double minTput = Double.MAX_VALUE;
        boolean normalized;

        Row(Long threadCnt) {
            this.threadCnt = threadCnt;
            opsPerSecond = new TreeMap<>();
        }

        void addColumn(String product, Double tput) {
            opsPerSecond.put(product, tput);

            if (tput > maxTput)
                maxTput = tput;

            if (tput < minTput)
                minTput = tput;
        }

        Long getThreadCnt() {
            return threadCnt;
        }

        Map<String, Double> getOpsPerSecond() {
            return opsPerSecond;
        }

        Set<String> getProductNames() {
            return opsPerSecond.keySet();
        }

        void normalizeData() {
            normalizeData(true);
        }

        void denormalizeData() {
            normalizeData(false);
        }

        private void normalizeData(boolean normalize) {
            if ((normalize && normalized) || (!normalize && !normalized))
                return;

            Double range = maxTput - minTput;
            Map<String, Double> normalOpsPerSecond = new TreeMap<>();

            for (Map.Entry<String, Double> row : opsPerSecond.entrySet()) {
                Double normalized;

                if (normalize)
                    normalized = (row.getValue() - minTput) / range;
                else
                    normalized = (row.getValue() * range + minTput);

                normalOpsPerSecond.put(row.getKey(), normalized);
            }

            opsPerSecond = normalOpsPerSecond;
            normalized = normalize;
        }

        void printOn(PrintStream out) {
            out.printf("%7d", threadCnt);

            for (Double tput : opsPerSecond.values())
                out.printf("%12.0f", tput);

            out.printf("%n");
        }
    }
}