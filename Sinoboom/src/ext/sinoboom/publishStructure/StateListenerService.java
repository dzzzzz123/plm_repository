package ext.sinoboom.publishStructure;

import java.io.Serializable;

import com.github.jsonldjava.shaded.com.google.common.base.Objects;
import com.ptc.arbortext.windchill.partlist.PartList;

import wt.events.KeyedEvent;
import wt.events.KeyedEventListener;
import wt.fc.Persistable;
import wt.fc.PersistenceManagerEvent;
import wt.part.WTPart;
import wt.services.ManagerException;
import wt.services.ServiceEventListenerAdapter;
import wt.services.StandardManager;
import wt.util.WTException;

public class StateListenerService extends StandardManager implements StateListener, Serializable {

	private static final long serialVersionUID = 1L;
	private static final String CLASSNAME = StateListenerService.class.getName();
	private KeyedEventListener listener;

	String innerName = PropertiesHelper.getStrFromProperties(Util.partListRefInnerName);

	public String getConceptualClassname() {
		return CLASSNAME;
	}

	public static StateListenerService newStateListenerService() throws WTException {
		StateListenerService instance = new StateListenerService();
		instance.initialize();
		return instance;
	}

	@Override
	protected void performStartupProcess() throws ManagerException {
		this.listener = new WCListenerEventListener(getConceptualClassname());
		getManagerService().addEventListener(listener,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.UPDATE));
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
			System.out.println("-----------sout-------------当前监听的是状态修改");
			if (Objects.equal(typeName, Util.partListRefInnerNameValue)) {
				System.out.println("-----------sout-------------当前修改状态的是部件列表载体");
				WTPart ref = (WTPart) target;
				Util.alterPartListByRef(ref);
				Util.alterRefByRefUrl(ref);
			} else if (target instanceof PartList) {
				System.out.println("-----------sout-------------当前修改状态的是部件列表");
				PartList partList = (PartList) target;
				Util.alterRefByPartList(partList);
			}

		}
	}

}
