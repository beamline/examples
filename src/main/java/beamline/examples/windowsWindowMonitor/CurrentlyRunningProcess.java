package beamline.examples.windowsWindowMonitor;

import java.util.Date;
import java.util.UUID;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

import beamline.sources.XesSource;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class CurrentlyRunningProcess implements XesSource {

	private static final int POLLING_DELAY = 100;
	private static final XFactory xesFactory = new XFactoryNaiveImpl();
	
	private String caseId;
	private String latestProcess = null;
	private Subject<XTrace> ps;
	
	public CurrentlyRunningProcess() {
		this.caseId = UUID.randomUUID().toString();
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
				while(true) {
					String currentProcess = getWindowName();
					if (!currentProcess.isEmpty() && !currentProcess.equals(latestProcess)) {
						latestProcess = currentProcess;
						XEvent event = xesFactory.createEvent();
						XConceptExtension.instance().assignName(event, currentProcess);
						XTimeExtension.instance().assignTimestamp(event, new Date());
						XTrace eventWrapper = xesFactory.createTrace();
						XConceptExtension.instance().assignName(eventWrapper, caseId);
						eventWrapper.add(event);
						ps.onNext(eventWrapper);
					}
					
					try {
						Thread.sleep(POLLING_DELAY);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	private static String getWindowName() {
		int MAX_TITLE_LENGTH = 1024;
		char[] buffer = new char[MAX_TITLE_LENGTH * 2];
		HWND hwnd = User32.INSTANCE.GetForegroundWindow();
		User32.INSTANCE.GetWindowText(hwnd, buffer, MAX_TITLE_LENGTH);
		
		IntByReference pid = new IntByReference();
		User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
		HANDLE p = Kernel32.INSTANCE.OpenProcess(Kernel32.PROCESS_QUERY_INFORMATION | Kernel32.PROCESS_VM_READ, false, pid.getValue());
		Psapi.INSTANCE.GetModuleBaseNameW(p, null, buffer, MAX_TITLE_LENGTH);

		return Native.toString(buffer);
	}
	
	public interface Psapi extends StdCallLibrary {
		@SuppressWarnings("deprecation")
		Psapi INSTANCE = (Psapi) Native.loadLibrary("Psapi", Psapi.class);
		WinDef.DWORD GetModuleBaseNameW(HANDLE hProcess, HANDLE hModule, char[] lpBaseName, int nSize);
	}
}
