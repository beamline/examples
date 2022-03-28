package beamline.examples.wikipedia;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.json.JSONObject;

import beamline.events.BEvent;
import beamline.exceptions.EventException;
import beamline.sources.BeamlineAbstractSource;

public class WikipediaEditSource extends BeamlineAbstractSource {

	private static final long serialVersionUID = 608025607423103621L;
	private static List<String> processesToStream = Arrays.asList("enwiki");

	public void run(SourceContext<BEvent> ctx) throws Exception {
		Queue<BEvent> buffer = new LinkedList<>();
		
		new Thread(() -> {
			Client client = ClientBuilder.newClient();
			WebTarget target = client.target("https://stream.wikimedia.org/v2/stream/recentchange");
			SseEventSource source = SseEventSource.target(target).reconnectingEvery(5, TimeUnit.SECONDS).build();
			source.register((InboundSseEvent t) -> {
					String data = t.readData();
					if (data != null) {
						JSONObject obj = new JSONObject(data);
						
						String processName = obj.getString("wiki");
						String caseId = obj.getString("title");
						String activityName = obj.getString("type");
						
						if (processesToStream.contains(processName)) {
							// prepare the actual event
							try {
								buffer.add(BEvent.create(processName, caseId, activityName));
							} catch (EventException e) {
								e.printStackTrace();
							}
						}
					}
				});
			source.open();
		}).start();
		
		while(isRunning()) {
			while (isRunning() && buffer.isEmpty()) {
				Thread.sleep(100l);
			}
			if (isRunning()) {
				synchronized (ctx.getCheckpointLock()) {
					BEvent e = buffer.poll();
					ctx.collect(e);
				}
			}
		}
	}
}
