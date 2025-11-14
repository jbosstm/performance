/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package com.hp.mwtests.ts.arjuna.performance;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.arjuna.ats.arjuna.common.Uid;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class UidBenchmark {

   private byte[] byteForm;
   private Uid toCopy;

   @Setup
   public void setup() {
      toCopy = Uid.maxUid();
      byteForm = toCopy.getBytes();
   }

   @Benchmark
   public Uid fromByteForm() {
      return new Uid(byteForm);
   }

   @Benchmark
   public byte[] getBytes(Blackhole bh) {
      var uid = new Uid(toCopy);
      // this should enable uid barriers to still materialize
      bh.consume(uid);
      return uid.getBytes();
   }

   @Benchmark
   public String stringForm(Blackhole bh) {
      var uid = new Uid(toCopy);
      bh.consume(uid);
      return uid.stringForm();
   }

}
