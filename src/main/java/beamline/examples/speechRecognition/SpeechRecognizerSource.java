package beamline.examples.speechRecognition;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import beamline.sources.XesSource;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class SpeechRecognizerSource implements XesSource {

	private static final XFactory xesFactory = new XFactoryNaiveImpl();
	private static final long MILLISECS_FOR_NEW_CASE = 2500;
	
	private int caseId = 0;
	private Subject<XTrace> ps;

	public SpeechRecognizerSource() {
		this.ps = PublishSubject.create();
	}

	@Override
	public void prepare() throws Exception {
		new Thread(new Runnable() {
			@Override
			public void run() {
				AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 60000, 16, 2, 4, 44100, false);
				DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
				TargetDataLine microphone;

				try {
					Model model = new Model("src/main/resources/vosk-model-small-en-us-0.15/");
					Recognizer recognizer = new Recognizer(model, 120000);

					microphone = (TargetDataLine) AudioSystem.getLine(info);
					microphone.open(format);
					microphone.start();

					ByteArrayOutputStream out = new ByteArrayOutputStream();
					int numBytesRead;
					int CHUNK_SIZE = 1024;
					int bytesRead = 0;
					byte[] b = new byte[4096];

					System.out.println("Start talking now...");
					
					int lastWordIndex = 0;
					long lastWordMillisecs = Long.MIN_VALUE;
					
					while (bytesRead <= 100000000) {
						numBytesRead = microphone.read(b, 0, CHUNK_SIZE);
						bytesRead += numBytesRead;
						out.write(b, 0, numBytesRead);

						// using just partial results
						recognizer.acceptWaveForm(b, numBytesRead);
						String text = (String) new JSONObject(recognizer.getPartialResult()).get("partial");
						if (text.isEmpty()) {
							continue;
						}
						String[] words = text.split(" ");
						String word = "";
						
						if (lastWordIndex < words.length) {
							for (; lastWordIndex < words.length; lastWordIndex++) {
								word = word + " " + words[lastWordIndex];
							}
							word = word.trim();
							
							if (!word.isEmpty()) {
								
								// processing new case ids
								if (lastWordMillisecs + MILLISECS_FOR_NEW_CASE < System.currentTimeMillis()) {
									caseId++;
								}
								lastWordMillisecs = System.currentTimeMillis();
								
								// prepare the actual event
								XEvent event = xesFactory.createEvent();
								XConceptExtension.instance().assignName(event, word);
								XTimeExtension.instance().assignTimestamp(event, new Date());
								XTrace eventWrapper = xesFactory.createTrace();
								XConceptExtension.instance().assignName(eventWrapper, "case-" + caseId);
								System.out.println(word);
								eventWrapper.add(event);
								ps.onNext(eventWrapper);
							}
						}
					}

					recognizer.close();
					microphone.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public Observable<XTrace> getObservable() {
		return ps;
	}
}
