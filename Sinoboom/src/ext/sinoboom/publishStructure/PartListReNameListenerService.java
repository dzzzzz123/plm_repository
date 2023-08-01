package ext.sinoboom.publishStructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import com.ptc.arbortext.windchill.partlist.PartListMaster;

import wt.events.KeyedEvent;
import wt.events.KeyedEventListener;
import wt.fc.IdentityServiceEvent;
import wt.part.WTPart;
import wt.services.ManagerException;
import wt.services.ServiceEventListenerAdapter;
import wt.services.StandardManager;
import wt.util.WTException;

public class PartListReNameListenerService extends StandardManager implements PartListReNameListener, Serializable {

	private static final long serialVersionUID = 1L;
	private static final String CLASSNAME = PartListReNameListenerService.class.getName();
	private KeyedEventListener listener;

	public String getConceptualClassname() {
		return CLASSNAME;
	}

	public static PartListReNameListenerService newPartListReNameListenerService() throws WTException {
		PartListReNameListenerService instance = new PartListReNameListenerService();
		instance.initialize();
		return instance;
	}

	@Override
	protected void performStartupProcess() throws ManagerException {
		this.listener = new WCListenerEventListener(getConceptualClassname());
		getManagerService().addEventListener(listener,
				IdentityServiceEvent.generateEventKey(IdentityServiceEvent.PRE_CHANGE_IDENTITY));
	}

	class WCListenerEventListener extends ServiceEventListenerAdapter {
		public WCListenerEventListener(String manager_name) {
			super(manager_name);
		}

		@Override
		public void notifyVetoableEvent(Object event) throws Exception {
			System.out.println("触发了改名事件");
			if (!(event instanceof KeyedEvent)) {
				return;
			}
			Object target = ((KeyedEvent) event).getEventTarget();
			Object type = ((KeyedEvent) event).getEventType();
			ArrayList<WTPart> wtPartsNotAttached = new ArrayList<WTPart>();
			Iterator<WTPart> a = wtPartsNotAttached.iterator();
			System.out.println("---------type---------" + type);
			System.out.println("---------target---------" + target);
			if (target instanceof PartListMaster) {
				// TODO
				System.out.println("---------确实是PartListMaster---------");
				return;
			}
		}
	}

}
