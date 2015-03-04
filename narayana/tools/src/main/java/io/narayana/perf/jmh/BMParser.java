package io.narayana.perf.jmh;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BMParser {
    BufferedReader reader;

    public BMParser() {
    }

    public BMParser(String fname) throws IOException {
        reader = new BufferedReader(new FileReader(fname));
        // read the header
        reader.readLine();
    }

    public Benchmark nextBenchmark() throws IOException {
        return parseBenchmark(reader.readLine());
    }

    public Benchmark parseBenchmark(String line) throws IOException {
        Benchmark bm = new Benchmark();

        if (line == null)
            return null;

        String[] values = line.split( "," );

        bm.benchmark = stripQuotes(values[0]);
        bm.mode = stripQuotes(values[1]);
        bm.threads = Integer.valueOf(stripQuotes(values[2]));
        bm.samples = Integer.valueOf(stripQuotes(values[3]));
        bm.score = Double.valueOf(stripQuotes(values[4]));
        if (!values[5].equals( "NaN" ) )
            bm.scoreError = Double.valueOf(values[5]);

        bm.unit = stripQuotes(values[6]);

        return bm;
    }

    public static Map<String, Benchmark> readBenchmarks(String fname) throws IOException {
        BMParser parser = new BMParser(fname);
        Map<String, Benchmark> benchmarks = new HashMap<>();
        Benchmark bm = parser.nextBenchmark();

        while (bm != null) {
            benchmarks.put(bm.benchmark, bm);
            bm = parser.nextBenchmark();
        }

        return benchmarks;
    }

    private String stripQuotes( String quotedStr ) {
        return quotedStr.replace( "\"", "" );
    }
}
