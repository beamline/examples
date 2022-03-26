package beamline.examples.windowsWindowMonitor;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

import beamline.events.BEvent;
import beamline.exceptions.EventException;
import beamline.sources.BeamlineAbstractSource;

public class WindowsWindowMonitorSource extends BeamlineAbstractSource {

	private static final long serialVersionUID = -7826513897494030243L;
	private static final int POLLING_DELAY = 100;
	
	@Override
	public void run(SourceContext<BEvent> ctx) throws Exception {
		Queue<BEvent> buffer = new LinkedList<>();
		
		String caseId = UUID.randomUUID().toString();
		new Thread(new Runnable() {
			@Override
			public void run() {
				String latestProcess = "";
				while(isRunning()) {
					String currentProcess = getWindowName();
					if (!currentProcess.isEmpty() && !currentProcess.equals(latestProcess)) {
						latestProcess = currentProcess;
						try {
							buffer.add(BEvent.create("window", caseId, currentProcess));
						} catch (EventException e) { }
					}
					
					try {
						Thread.sleep(POLLING_DELAY);
					} catch (InterruptedException e) { }
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
