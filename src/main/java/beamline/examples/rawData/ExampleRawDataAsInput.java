package beamline.examples.rawData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;

public class ExampleRawDataAsInput {

	public static void main(String[] args) throws IOException {
		
		// save log into tmp file
		Path logFile = Files.createTempFile(null, null);
		Files.write(logFile, "002ActA\n001ActA\n002B\n002Act_C\n001B\n001Act_C\n".getBytes(StandardCharsets.UTF_8));

		XFactory factory = new XFactoryNaiveImpl();
		
		// extract all strings
		Observable<String> stramOfStrings = Observable.defer(() -> new ObservableSource<String>() {
			@Override
			public void subscribe(@NonNull Observer<? super @NonNull String> observer) {
				try {
					Files.lines(logFile).forEach(observer::onNext);
					observer.onComplete();
				} catch (IOException e) {
					observer.onError(e);
				}
			}
		});
		
		// transform strings into traces
		Observable<XTrace> streamOfXTraces = stramOfStrings.flatMap(new Function<String, ObservableSource<XTrace>>() {
			@Override
			public @NonNull ObservableSource<XTrace> apply(@NonNull String t) throws Throwable {
				String caseId = t.substring(0, 3);
				String activityName = t.substring(3);
				
				XTrace wrapper = factory.createTrace();
				XEvent event = factory.createEvent();
				
				XConceptExtension.instance().assignName(wrapper, caseId);
				XConceptExtension.instance().assignName(event, activityName);
				
				wrapper.add(event);
				
				return Observable.just(wrapper);
			}
		});
		
		// process the traces
		streamOfXTraces.subscribe(new Consumer<XTrace>() {
			@Override
			public void accept(@NonNull XTrace t) throws Throwable {
				System.out.println(
					XConceptExtension.instance().extractName(t) + " - " +
					XConceptExtension.instance().extractName(t.get(0)) +  " - " +
							t.get(0).getAttributes()
				);
			}
		});
	}
}
