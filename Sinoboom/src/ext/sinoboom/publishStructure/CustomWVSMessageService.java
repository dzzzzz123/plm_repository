package ext.sinoboom.publishStructure;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;

import com.ptc.arbortext.windchill.partlist.PartList;

import wt.events.KeyedEvent;
import wt.fc.Persistable;
import wt.fc.collections.WTCollection;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.LifeCycleServiceEvent;
import wt.lifecycle.LifeCycleState;
import wt.part.WTPart;
import wt.services.ManagerException;
import wt.services.ManagerService;
import wt.services.ServiceEventListenerAdapter;
import wt.services.StandardManager;
import wt.util.WTException;

public class CustomWVSMessageService extends StandardManager implements WVSMessageService, Serializable {

	private static final long serialVersionUID = 1L;
	private static final String CLASSNAME = CustomWVSMessageService.class.getName();

	@SuppressWarnings("deprecation")
	public String getConceptualClassname() {
		return CLASSNAME;
	}

	protected void performStartupProcess() throws ManagerException {
		super.performStartupProcess();
		getManagerService().addEventListener(new ServiceEventListenerAdapter(this.getConceptualClassname()) {
			public void notifyVetoableMultiObjectEvent(Object event) {

				if (!(event instanceof KeyedEvent)) {
					return;
				}
				KeyedEvent eventObject = (KeyedEvent) event;
				Object target = eventObject.getEventTarget();

				process((LifeCycleServiceEvent) event, target);
			}
		}, LifeCycleServiceEvent.generateEventKey(LifeCycleServiceEvent.STATE_CHANGE));

		System.out.println("Start Message Service");
	}

	public void shutdown() throws ManagerException {
		super.shutdown();
	}

	public void registerEvents(ManagerService managerService) {
	}

	public static CustomWVSMessageService newCustomWVSMessageService() throws WTException {
		CustomWVSMessageService instance = new CustomWVSMessageService();
		instance.initialize();
		return instance;
	}

	@SuppressWarnings("rawtypes")
	protected void process(LifeCycleServiceEvent event, Object target) {

		try {
			// INSERT CUSTOM EVENT PROCESSING HERE
			if (target instanceof wt.fc.collections.WTCollection) {
				Iterator objsIt = ((WTCollection) target).persistableIterator();

				while (objsIt.hasNext()) {
					Persistable p = (Persistable) objsIt.next();
					LifeCycleState currentState = ((LifeCycleManaged) p).getState();
					String typeName = Util.getPerType(p);
					HashSet<String> innerNameList = Util.getInnerNameList();
					if (innerNameList.contains(typeName)) {
						if (p instanceof WTPart) {
							WTPart ref = (WTPart) p;
							Util.alterRefByRefUrl(ref, currentState);
							break;
						} else if (p instanceof PartList) {
							PartList partList = (PartList) p;
							Util.alterRefByPartList(partList, currentState);
							break;
						}
					}
				}
			} else {
				System.out.println("not WTCollection  " + target.getClass());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
