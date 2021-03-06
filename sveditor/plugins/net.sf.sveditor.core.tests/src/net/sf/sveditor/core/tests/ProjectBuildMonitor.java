package net.sf.sveditor.core.tests;

import java.util.Map;

import net.sf.sveditor.core.ISVProjectBuilderListener;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;

public class ProjectBuildMonitor implements ISVProjectBuilderListener {
	private Object					fSemaphore = new Object();
	private int						fKind = -1;
	private LogHandle				fLog = LogFactory.getLogHandle("ProjectBuildMonitor");
	

	@Override
	public void build_start(int kind, Map<String, String> args) {
		fLog.debug("build_start: " + kind);
	}

	@Override
	public void build_complete(int kind, Map<String, String> args) {
		fLog.debug("build_complete: " + kind);
		// TODO Auto-generated method stub
		fKind = kind;
		synchronized (fSemaphore) {
			fSemaphore.notifyAll();
		}
	}
	
	public void reset() {
		fKind = -1;
	}
	
	public boolean wait(int kind, int ms) {
		fLog.debug("--> wait: " + kind);
		
		if (fKind == kind) {
			fLog.debug("<-- wait: " + kind + " early escape");
			return true;
		}
		
		try {
			synchronized (fSemaphore) {
				fSemaphore.wait(ms);
			}
		} catch (InterruptedException e) {}
	
		fLog.debug("<-- wait: " + kind + " " + fKind);
		return (fKind == kind);
	}

}
