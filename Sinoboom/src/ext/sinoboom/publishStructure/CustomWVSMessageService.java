package ext.sinoboom.publishStructure;

import java.io.Serializable;
import java.util.Iterator;

import com.ptc.arbortext.windchill.partlist.PartList;

import wt.events.KeyedEvent;
import wt.fc.Persistable;
import wt.fc.collections.WTCollection;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.LifeCycleServiceEvent;
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

				if (event instanceof KeyedEvent) {
					System.out.println("event instanceof KeyedEvent");
					System.out.println("event" + event);
				} else {
					System.out.println("event not instanceof KeyedEvent ");
					System.out.println("event" + event);
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
			System.out.println("GOT STATE CHANGE EVENT");
			System.out.println(event);

			// INSERT CUSTOM EVENT PROCESSING HERE
			if (target instanceof wt.fc.collections.WTCollection) {
				Iterator objsIt = ((WTCollection) target).persistableIterator();

				while (objsIt.hasNext()) {
					Persistable p = (Persistable) objsIt.next();
					String currentState = ((LifeCycleManaged) p).getLifeCycleState().toString();
					System.out.println("The current lifecycle is (" + currentState + ")");

					if (p instanceof WTPart) {
						System.out.println("p is a WTPart");
						break;
					} else if (p instanceof PartList) {
						System.out.println("p is a PartList");
						break;
					}

				}
			} else {
				System.out.println("not WTCollection");
				System.out.println(target.getClass());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
