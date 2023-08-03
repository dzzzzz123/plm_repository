package ext.sinoboom.publishStructure;

import java.io.Serializable;
import java.util.HashSet;

import wt.events.KeyedEvent;
import wt.events.KeyedEventListener;
import wt.fc.Persistable;
import wt.part.WTPart;
import wt.services.ManagerException;
import wt.services.ServiceEventListenerAdapter;
import wt.services.StandardManager;
import wt.util.WTException;
import wt.vc.wip.WorkInProgressServiceEvent;

public class MaintainUrlListenerService extends StandardManager implements MaintainUrlListener, Serializable {

	private static final long serialVersionUID = 1L;
	private static final String CLASSNAME = MaintainUrlListenerService.class.getName();
	private KeyedEventListener listener;

	public String getConceptualClassname() {
		return CLASSNAME;
	}

	public static MaintainUrlListenerService newMaintainUrlListenerService() throws WTException {
		MaintainUrlListenerService instance = new MaintainUrlListenerService();
		instance.initialize();
		return instance;
	}

	@Override
	protected void performStartupProcess() throws ManagerException {
		this.listener = new WCListenerEventListener(getConceptualClassname());
		getManagerService().addEventListener(listener,
				WorkInProgressServiceEvent.generateEventKey(WorkInProgressServiceEvent.POST_CHECKIN));
	}

	class WCListenerEventListener extends ServiceEventListenerAdapter {
		public WCListenerEventListener(String manager_name) {
			super(manager_name);
		}

		@Override
		public void notifyVetoableEvent(Object event) throws Exception {
			if (!(event instanceof KeyedEvent)) {
				return;
			}
			Persistable target = (Persistable) ((KeyedEvent) event).getEventTarget();
			String typeName = Util.getPerType(target);
			System.out.println("-----------当前操作是维护url-------------");
			HashSet<String> innerNameList = Util.getInnerNameList();
			if (innerNameList.contains(typeName)) {
				WTPart ref = (WTPart) target;
				String ErrorMsg = Util.maintainRefUrl(ref);
				if (ErrorMsg.length() > 0) {
					throw new WTException(ErrorMsg);
				}
			}
		}
	}

}
