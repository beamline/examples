package beamline.examples.wikipedia;

import java.io.File;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import beamline.events.BEvent;
import beamline.miners.trivial.DirectlyFollowsDependencyDiscoveryMiner;
import beamline.miners.trivial.ProcessMap;

public class WikipediaEdit {

	public static void main(String[] args) throws Exception {
		System.out.println("starting...");
		
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env
			.addSource(new WikipediaEditSource())
			.keyBy(BEvent::getProcessName)
			.flatMap(new DirectlyFollowsDependencyDiscoveryMiner()
					.setModelRefreshRate(10)
					.setMinDependency(0))
			.addSink(new SinkFunction<ProcessMap>(){
				private static final long serialVersionUID = 6818511702068908564L;

				public void invoke(ProcessMap value, Context context) throws Exception {
					System.out.println(value.getProcessedEvents());
					value.generateDot().exportToSvg(new File("src/main/resources/output/output.svg"));
				};
			});
		env.execute();
	}

}
