package beamline.examples.rawData;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import beamline.events.BEvent;
import beamline.exceptions.EventException;
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
						ctx.collect(BEvent.create("my-process-name", activityName, caseId));
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
//			.keyBy(BEvent::getTraceName)
			.print();
		env.execute();
	}
}
