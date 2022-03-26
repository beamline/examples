package beamline.examples.speechRecognition;

import java.io.File;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import beamline.events.BEvent;
import beamline.miners.trivial.DirectlyFollowsDependencyDiscoveryMiner;
import beamline.miners.trivial.ProcessMap;

public class SpeechRecognition {

	public static void main(String...args) throws Exception {
//		System.out.println("starting...");
//		TrivialDiscoveryMiner miner = new TrivialDiscoveryMiner();
//		miner.setModelRefreshRate(1);
//		miner.setMinDependency(0);
//
//		// in the following statement we set a hook to save the map every 1000 events processed
//		miner.setOnAfterEvent(() -> {
//			if (miner.getProcessedEvents() % 2 == 0) {
//				try {
//					File f = new File("src/main/resources/output/output.svg");
//					miner.getLatestResponse().generateDot().exportToSvg(f);
//				} catch (IOException e) { }
//			}
//		});
//		
//		// connects the miner to the actual source
//		XesSource source = new SpeechRecognizerSource();
//		source.prepare();
//		source.getObservable().subscribe(miner);
		System.out.println("starting...");
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env
			.addSource(new SpeechRecognitionSource())
			.keyBy(BEvent::getProcessName)
			.flatMap(new DirectlyFollowsDependencyDiscoveryMiner()
					.setModelRefreshRate(1)
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
