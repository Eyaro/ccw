package ccw.console;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.part.IPageBookViewPage;

import ccw.ClojureCore;
import ccw.CCWPlugin;
import ccw.debug.ClojureClient;
import ccw.editors.antlrbased.EvaluateTextAction;
import ccw.launching.LaunchUtils;
import ccw.outline.NamespaceBrowser;
import ccw.preferences.PreferenceConstants;

public class ConsolePageParticipant implements IConsolePageParticipant
{
	private IOConsole console;
	private ClojureClient clojureClient;

	public void init(IPageBookViewPage page, IConsole console) {
		assert org.eclipse.debug.ui.console.IConsole.class.isInstance
		(console);
		assert TextConsole.class.isInstance(console);
		this.console = (IOConsole) console;
		new Thread(new Runnable () {
			public void run() {
				activate();
			}
		}).start();

	}

	public synchronized void activate () {
		if (clojureClient == null) {
			bindConsoleToClojureEnvironment();
			if (clojureClient != null) {
				System.out.println("activated");
			}
		}
		if (clojureClient != null) {
			NamespaceBrowser.setClojureClient(clojureClient);
		}
	}

	public void activated() {
		//      activate();
	}

	private  void bindConsoleToClojureEnvironment() {
		if (clojureClient == null) {
			org.eclipse.debug.ui.console.IConsole processConsole =
				(org.eclipse.debug.ui.console.IConsole) console;
			int clojureVMPort = LaunchUtils.getLaunchServerReplPort
			(processConsole.getProcess().getLaunch());
			boolean stop = false;
			while (stop!=true) {
				if (clojureVMPort != -1) {
					stop=true;
					clojureClient = new ClojureClient(clojureVMPort);
					addPatternMatchListener(this.console);
					if (CCWPlugin.getDefault().getPreferenceStore().getBoolean
							(PreferenceConstants.SWITCH_TO_NS_ON_REPL_STARTUP)) {
						try {
							List<IFile> files = LaunchUtils.getFilesToLaunchList
							(processConsole.getProcess().getLaunch().getLaunchConfiguration());
							if (files.size() > 0) {
								String namespace = ClojureCore.getDeclaredNamespace(files.get
										(0));
								if (namespace != null) {
									EvaluateTextAction.evaluateText(this.console, "(in-ns '" +
											namespace + ")", false);
								}
							}
						} catch (CoreException e) {
							CCWPlugin.logError("error while trying to guess the ns to which make the REPL console switch", e);
						}
					}
					break;
				}
				try {
					Thread.sleep(100);
					clojureVMPort = LaunchUtils.getLaunchServerReplPort
					(processConsole.getProcess().getLaunch());
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					stop=true;
				}
			}
		}
	}

	public void deactivated() {
		// Nothing
	}

	public void dispose() {
		// Nothing
	}

	public Object getAdapter(Class adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	private void addPatternMatchListener(TextConsole console) {
		console.addPatternMatchListener(new IPatternMatchListener() {
			public int getCompilerFlags() {
				return 0;
			}
			public String getLineQualifier() {
				return null;
			}

			public String getPattern() {
				return ".*\n";
			}

			public void connect(TextConsole console) {
				// Nothing
			}

			public void disconnect() {
				// Nothing
			}

			public void matchFound(PatternMatchEvent event) {
				if (clojureClient != null) {
					NamespaceBrowser.setClojureClient(clojureClient);
				}
			}
		});
	} 
}