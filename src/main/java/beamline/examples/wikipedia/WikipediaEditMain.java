package beamline.examples.wikipedia;

import java.io.File;
import java.io.IOException;

import beamline.miners.trivial.TrivialDiscoveryMiner;
import beamline.sources.XesSource;

public class WikipediaEditMain {

	public static void main(String[] args) throws Exception {
		System.out.println("starting...");
		TrivialDiscoveryMiner miner = new TrivialDiscoveryMiner();
		miner.setModelRefreshRate(1);
		miner.setMinDependency(0);

		// in the following statement we set a hook to save the map every 1000 events processed
		miner.setOnAfterEvent(() -> {
			if (miner.getProcessedEvents() % 50 == 0) {
				try {
					System.out.println("updating model");
					File f = new File("src/main/resources/output/output.svg");
					miner.getLatestResponse().generateDot().exportToSvg(f);
				} catch (IOException e) { }
			}
		});
		
		// connects the miner to the actual source
		XesSource source = new WikipediaEditSource();
		source.prepare();
		source.getObservable().subscribe(miner);
	}

}
