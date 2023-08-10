package ext.sinoboom.cadDoc;

import java.io.Serializable;

import ext.ait.util.IBAUtil;
import ext.ait.util.PartUtil;
import ext.sinoboom.publishStructure.PropertiesHelper;
import wt.epm.EPMDocument;
import wt.epm.workspaces.EPMWorkspaceManagerEvent;
import wt.events.KeyedEvent;
import wt.events.KeyedEventListener;
import wt.part.WTPart;
import wt.services.ManagerException;
import wt.services.ServiceEventListenerAdapter;
import wt.services.StandardManager;
import wt.util.WTException;

public class CheckinListenerService extends StandardManager implements Serializable, CheckinListener {
	private static final long serialVersionUID = 1L;
	private static final String CLASSNAME = CheckinListenerService.class.getName();
	private KeyedEventListener listener;
	public static String EN_NAME = "windchill.cad.enname";

	public String getConceptualClassname() {
		return CLASSNAME;
	}

	public static CheckinListenerService newCheckinListenerService() throws WTException {
		CheckinListenerService instance = new CheckinListenerService();
		instance.initialize();
		return instance;
	}

	protected void performStartupProcess() throws ManagerException {
		super.performStartupProcess();
		listener = new WCListenerEventListener(getConceptualClassname());
		// 监听cad检入事件
		System.out.println("添加cad检入后监听程序 ext.sinoboom.cadDoc.listener.CheckinListenerService");
		getManagerService().addEventListener(this.listener,
				EPMWorkspaceManagerEvent.generateEventKey(EPMWorkspaceManagerEvent.PRE_WORKSPACE_CHECKIN));

	}

	class WCListenerEventListener extends ServiceEventListenerAdapter {
		public WCListenerEventListener(String manager_name) {
			super(manager_name);
		}

		@Override
		public void notifyEvent(Object arg0) {
			super.notifyEvent(arg0);
		}

		@Override
		public void notifyMultiObjectEvent(Object arg0) {
			super.notifyMultiObjectEvent(arg0);
		}

		@Override
		public void notifyVetoableMultiObjectEvent(Object arg0) throws Exception {
			super.notifyVetoableMultiObjectEvent(arg0);
		}

		@Override
		public void notifyVetoableEvent(Object eve) throws Exception {
			if (!(eve instanceof KeyedEvent)) {
				return;
			}
			KeyedEvent event = (KeyedEvent) eve;
			Object target = event.getEventTarget();
			if (target instanceof EPMDocument) {
				EPMDocument drw = (EPMDocument) target;
				String docType = drw.getDocType().toString();
				System.out.println("<----docType----->" + docType);
				if (docType.equals("CADDRAWING")) {
					WTPart wtPart = PartUtil.getPartByEPM(drw);
					EPMDocument asm = PartUtil.get2DOr3DByPart(wtPart, "3D");
					IBAUtil drwIba = new IBAUtil(drw);
					IBAUtil asmIba = new IBAUtil(asm);
					String enNameIBA = PropertiesHelper.getStrFromProperties(EN_NAME);
					String enNameValue = asmIba.getIBAValue(enNameIBA);
					System.out.println("<----enNameValue----->" + enNameValue);
					drwIba.setIBAValue(enNameIBA, enNameValue);
				}
			} else {
				System.out.println("<----docType is not CADDRAWING----->");
			}
		}

	}
}
