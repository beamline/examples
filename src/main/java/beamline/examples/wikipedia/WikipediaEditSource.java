package beamline.examples.wikipedia;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.apache.commons.codec.digest.DigestUtils;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.json.JSONObject;

import beamline.sources.XesSource;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class WikipediaEditSource implements XesSource {

	private static final XFactory xesFactory = new XFactoryNaiveImpl();
	private static List<String> processesToStream = Arrays.asList("enwiki");
	private Subject<XTrace> ps;
	
	public WikipediaEditSource() {
		this.ps = PublishSubject.create();
	}
	
	@Override
	public Observable<XTrace> getObservable() {
		return ps;
	}

	@Override
	public void prepare() throws Exception {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Client client = ClientBuilder.newClient();
				WebTarget target = client.target("https://stream.wikimedia.org/v2/stream/recentchange");
				SseEventSource source = SseEventSource.target(target).reconnectingEvery(5, TimeUnit.SECONDS).build();
				source.register(new Consumer<InboundSseEvent>() {
					@Override
					public void accept(InboundSseEvent t) {
						String data = t.readData();
						if (data != null) {
							JSONObject obj = new JSONObject(data);
							
							String processName = obj.getString("wiki");
							String caseId = DigestUtils.md5Hex(obj.getString("title"));
							String activityName = obj.getString("type");
							
							if (processesToStream.contains(processName)) {
								// prepare the actual event
								XEvent event = xesFactory.createEvent();
								XConceptExtension.instance().assignName(event, activityName);
								XTimeExtension.instance().assignTimestamp(event, new Date());
								XTrace eventWrapper = xesFactory.createTrace();
								XConceptExtension.instance().assignName(eventWrapper, caseId);
								eventWrapper.add(event);
								ps.onNext(eventWrapper);
								System.out.println(caseId + " --> " + activityName);
							}
						}
					}
				});
				source.open();
			}
		}).start();
	}

}
