/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.benchmark;

import org.apache.flink.benchmark.functions.LongSourceType;
import org.apache.flink.benchmark.functions.MultiplyByTwo;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.DiscardingSink;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

public class InputBenchmark extends BenchmarkBase {
	public static final int MAP_RECORDS_PER_INVOCATION = 25_000_000;
	public static final int MAP_REBALANCE_RECORDS_PER_INVOCATION = 15_000_000;
	private static final long CHECKPOINT_INTERVAL_MS = 100;

	public static void main(String[] args)
			throws RunnerException {
		Options options = new OptionsBuilder()
				.verbosity(VerboseMode.NORMAL)
				.include(".*" + InputBenchmark.class.getCanonicalName() + ".*")
				.build();

		new Runner(options).run();
	}

	@Param({"F27_UNBOUNDED"})
	public LongSourceType sourceType;

	@Benchmark
	@OperationsPerInvocation(value = MAP_RECORDS_PER_INVOCATION)
	public void mapSink(FlinkEnvironmentContext context) throws Exception {

		StreamExecutionEnvironment env = context.env;
		env.enableCheckpointing(CHECKPOINT_INTERVAL_MS);
		env.setParallelism(1);

		DataStreamSource<Long> source = sourceType.source(env, MAP_RECORDS_PER_INVOCATION);
		source
				.map(new MultiplyByTwo())
				.addSink(new DiscardingSink<>());

		env.execute();
	}

	@Benchmark
	@OperationsPerInvocation(value = InputBenchmark.MAP_REBALANCE_RECORDS_PER_INVOCATION)
	public void mapRebalanceMapSink(FlinkEnvironmentContext context) throws Exception {

		StreamExecutionEnvironment env = context.env;
		env.enableCheckpointing(CHECKPOINT_INTERVAL_MS);
		env.setParallelism(1);

		DataStreamSource<Long> source = sourceType.source(env, MAP_REBALANCE_RECORDS_PER_INVOCATION);
		source
				.map(new MultiplyByTwo())
				.rebalance()
				.map((Long in) -> in)
				.addSink(new DiscardingSink<>());

		env.execute();
	}
}