package beamline.examples.rawData;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import beamline.events.BEvent;
import beamline.exceptions.EventException;
import beamline.miners.trivial.DirectlyFollowsDependencyDiscoveryMiner;
import beamline.miners.trivial.ProcessMap;
import beamline.sources.BeamlineAbstractSource;

public class ExampleRawDataAsInput {

	public static void main(String[] args) throws Exception {
		
		// save log into tmp file
		Path logPath = Files.createTempFile(null, null);
		Files.write(logPath, "002ActA\n001ActA\n002B\n002Act_C\n001B\n001Act_C\n".getBytes(StandardCharsets.UTF_8));
		String logFile = logPath.toString();

		BeamlineAbstractSource customSource = new BeamlineAbstractSource() {
			private static final long serialVersionUID = -4849888760148225147L;

			@Override
			public void run(SourceContext<BEvent> ctx) throws Exception {
				Files.lines(Path.of(logFile)).forEach(line -> {
					String caseId = line.substring(0, 3);
					String activityName = line.substring(3);
					
					try {
						ctx.collect(BEvent.create("my-process-name", caseId, activityName));
					} catch (EventException e) {
						e.printStackTrace();
					}
				});
			}
		};
		
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env
			.addSource(customSource)
			.keyBy(BEvent::getProcessName)
			.flatMap(new DirectlyFollowsDependencyDiscoveryMiner().setModelRefreshRate(1).setMinDependency(0.1))
			.addSink(new SinkFunction<ProcessMap>() {
				private static final long serialVersionUID = 5088089920578609906L;

				@Override
				public void invoke(ProcessMap value, Context context) throws Exception {
					value.generateDot().exportToSvg(new File("src/main/resources/output/output.svg"));
				};
			});
		env.execute();
	}
}
