package beamline.examples.speechRecognition;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.Queue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import org.apache.flink.configuration.Configuration;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import beamline.events.BEvent;
import beamline.sources.BeamlineAbstractSource;

public class SpeechRecognitionSource extends BeamlineAbstractSource {

	private static final long serialVersionUID = -2638193617911467198L;
	private static final long MILLISECS_FOR_NEW_CASE = 2500;
	TargetDataLine microphone;
	Recognizer recognizer;
	
	@Override
	public void open(Configuration parameters) throws Exception {
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 60000, 16, 2, 4, 44100, false);
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		
		Model model = new Model("src/main/resources/vosk-model-small-en-us-0.15/");
		recognizer = new Recognizer(model, 120000);
		
		microphone = (TargetDataLine) AudioSystem.getLine(info);
		microphone.open(format);
		microphone.start();
	}
	
	@Override
	public void run(SourceContext<BEvent> ctx) throws Exception {
		Queue<BEvent> buffer = new LinkedList<>();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				int caseId = 0;
				try {
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
								System.out.println(word);
								buffer.offer(BEvent.create("speech", "case-" + caseId, word));
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
