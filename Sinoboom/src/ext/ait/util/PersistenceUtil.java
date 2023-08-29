package ext.ait.util;

import org.apache.commons.lang3.StringUtils;

import com.ptc.core.meta.type.mgmt.common.TypeDefinitionDefaultView;
import com.ptc.core.meta.type.mgmt.server.impl.WTTypeDefinition;

import wt.doc.WTDocument;
import wt.doc.WTDocumentMaster;
import wt.doc.WTDocumentMasterIdentity;
import wt.epm.EPMDocument;
import wt.epm.EPMDocumentMaster;
import wt.epm.EPMDocumentMasterIdentity;
import wt.epm.util.EPMSoftTypeServerUtilities;
import wt.fc.IdentityHelper;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTObject;
import wt.folder.Folder;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.State;
import wt.org.WTPrincipal;
import wt.part.WTPart;
import wt.part.WTPartMaster;
import wt.part.WTPartMasterIdentity;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.session.SessionHelper;
import wt.type.TypeDefinitionReference;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.Mastered;
import wt.vc.baseline.ManagedBaseline;
import wt.vc.config.LatestConfigSpec;
import wt.vc.wip.CheckoutLink;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.Workable;

public class PersistenceUtil {

	private static ReferenceFactory factory = new ReferenceFactory();

	/**
	 * @description 得到对象的自定义/内部名称
	 * @param WTObject
	 * @return String
	 * @throws WTException
	 */
	public static String getTypeName(WTObject obj) {
		String typeDisplayName = null;
		TypeDefinitionReference ref = null;
		WTTypeDefinition definition = null;
		if (obj instanceof WTPart) {
			ref = ((WTPart) obj).getTypeDefinitionReference();
			try {
				@SuppressWarnings("deprecation")
				TypeDefinitionDefaultView view = EPMSoftTypeServerUtilities.getTypeDefinition(ref);
				definition = (WTTypeDefinition) PersistenceHelper.manager.refresh(view.getObjectID());
				typeDisplayName = definition.getDisplayNameKey(); // 类型的key
			} catch (WTException e) {
				e.printStackTrace();
			}
		}
		return typeDisplayName;
	}

	/**
	 * 检出对象
	 * 
	 * @param Workable
	 * @return Workable
	 * @throws WTException
	 */
	public static Workable checkoutPart(Workable workable) throws WTException {
		if (workable == null) {
			return null;
		}
		if (WorkInProgressHelper.isWorkingCopy(workable)) {
			return workable;
		}
		Folder folder = WorkInProgressHelper.service.getCheckoutFolder();
		try {
			CheckoutLink checkoutLink = WorkInProgressHelper.service.checkout(workable, folder, "AutoCheckOut");
			workable = checkoutLink.getWorkingCopy();
		} catch (WTPropertyVetoException ex) {
			ex.printStackTrace();
		}
		if (!WorkInProgressHelper.isWorkingCopy(workable)) {
			workable = WorkInProgressHelper.service.workingCopyOf(workable);
		}
		return workable;
	}

	/**
	 * 判断对象是否处于检出状态
	 * 
	 * @param Workable
	 * @return boolean
	 */
	public static boolean isCheckOut(Workable workable) {
		try {
			return WorkInProgressHelper.isCheckedOut(workable);
		} catch (WTException e) {

		}
		return false;
	}

	/**
	 * 修改几乎所有存在于Windchill中对象的状态
	 * 
	 * @param Object
	 * @param Object
	 * @return String
	 * @throws WTPropertyVetoException
	 * @throws WTException
	 */
	public static String changeObjectState(Object obj, Object objState) throws WTPropertyVetoException, WTException {
		String message = "";
		State newState = null;
		WTPrincipal currentUser = null;
		try {
			currentUser = SessionHelper.manager.getPrincipal();
			SessionHelper.manager.setAdministrator();
			if (objState instanceof State) {
				newState = (State) objState;
			} else if (objState instanceof String) {
				newState = State.toState(objState + "");
			}

			if (obj instanceof WTPart) {
				WTPart part = (WTPart) obj;
				part = (WTPart) wt.lifecycle.LifeCycleHelper.service
						.setLifeCycleState((wt.lifecycle.LifeCycleManaged) part, newState);
				PersistenceHelper.manager.refresh(part);

			} else if (obj instanceof EPMDocument) {
				EPMDocument epmDocument = (EPMDocument) obj;
				epmDocument = (EPMDocument) wt.lifecycle.LifeCycleHelper.service
						.setLifeCycleState((wt.lifecycle.LifeCycleManaged) epmDocument, newState);
				PersistenceHelper.manager.refresh(epmDocument);

			} else if (obj instanceof WTDocument) {
				WTDocument wtDocument = (WTDocument) obj;
				wtDocument = (WTDocument) wt.lifecycle.LifeCycleHelper.service
						.setLifeCycleState((wt.lifecycle.LifeCycleManaged) wtDocument, newState);
				PersistenceHelper.manager.refresh(wtDocument);

			} else if (obj instanceof ManagedBaseline) {
				ManagedBaseline managedBaseline = (ManagedBaseline) obj;
				managedBaseline = (ManagedBaseline) wt.lifecycle.LifeCycleHelper.service
						.setLifeCycleState((wt.lifecycle.LifeCycleManaged) managedBaseline, newState);
				PersistenceHelper.manager.refresh(managedBaseline);

			} else if (obj instanceof LifeCycleManaged) {
				LifeCycleManaged lifeCycleManaged = (LifeCycleManaged) obj;
				lifeCycleManaged = (LifeCycleManaged) wt.lifecycle.LifeCycleHelper.service
						.setLifeCycleState((wt.lifecycle.LifeCycleManaged) lifeCycleManaged, newState);
				PersistenceHelper.manager.refresh(lifeCycleManaged);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (currentUser != null) {
				SessionHelper.manager.setPrincipal(currentUser.getName());
			}
		}
		return message;
	}

	/**
	 * 更改部件/文档/图纸/其他对象的名称
	 * 
	 * @param Mastered
	 * @param String
	 */
	public static void changeName(Mastered mast, String name) {
		WTPrincipal user = null;
		try {
			user = SessionHelper.getPrincipal();
			SessionHelper.manager.setAdministrator();
			if (mast instanceof WTDocumentMaster) {
				WTDocumentMaster master = (WTDocumentMaster) mast;
				master = (WTDocumentMaster) PersistenceHelper.manager.refresh(master);
				WTDocumentMasterIdentity identity = (WTDocumentMasterIdentity) master.getIdentificationObject();
				identity.setName(name);
				master = (WTDocumentMaster) IdentityHelper.service.changeIdentity(master, identity);
				PersistenceHelper.manager.save(master);
			} else if (mast instanceof WTPartMaster) {
				WTPartMaster master = (WTPartMaster) mast;
				master = (WTPartMaster) PersistenceHelper.manager.refresh(master);
				WTPartMasterIdentity identity = (WTPartMasterIdentity) master.getIdentificationObject();
				identity.setName(name);
				master = (WTPartMaster) IdentityHelper.service.changeIdentity(master, identity);
				PersistenceHelper.manager.save(master);
			} else if (mast instanceof EPMDocumentMaster) {
				EPMDocumentMaster master = (EPMDocumentMaster) mast;
				master = (EPMDocumentMaster) PersistenceHelper.manager.refresh(master);
				EPMDocumentMasterIdentity identity = (EPMDocumentMasterIdentity) master.getIdentificationObject();
				identity.setName(name);
				master = (EPMDocumentMaster) IdentityHelper.service.changeIdentity(master, identity);
				PersistenceHelper.manager.save(master);
			}
			SessionHelper.manager.setPrincipal(user.getName());
			user = null;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (user != null) {
				try {
					SessionHelper.manager.setPrincipal(user.getName());
				} catch (WTException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 修改文档、物料、图纸等其他对象的编号
	 * 
	 * @param Mastered
	 * @param String
	 */
	public static void changeNumber(Mastered mast, String number) {
		WTPrincipal user = null;
		try {
			user = SessionHelper.getPrincipal();
			SessionHelper.manager.setAdministrator();
			if (mast instanceof WTDocumentMaster) {
				WTDocumentMaster master = (WTDocumentMaster) mast;
				master = (WTDocumentMaster) PersistenceHelper.manager.refresh(master);
				WTDocumentMasterIdentity identity = (WTDocumentMasterIdentity) master.getIdentificationObject();
				identity.setNumber(number);
				master = (WTDocumentMaster) IdentityHelper.service.changeIdentity(master, identity);
				PersistenceHelper.manager.save(master);
			} else if (mast instanceof WTPartMaster) {
				WTPartMaster master = (WTPartMaster) mast;
				master = (WTPartMaster) PersistenceHelper.manager.refresh(master);
				WTPartMasterIdentity identity = (WTPartMasterIdentity) master.getIdentificationObject();
				identity.setNumber(number);
				master = (WTPartMaster) IdentityHelper.service.changeIdentity(master, identity);
				PersistenceHelper.manager.save(master);
			} else if (mast instanceof EPMDocumentMaster) {
				EPMDocumentMaster master = (EPMDocumentMaster) mast;
				master = (EPMDocumentMaster) PersistenceHelper.manager.refresh(master);
				EPMDocumentMasterIdentity identity = (EPMDocumentMasterIdentity) master.getIdentificationObject();
				identity.setNumber(number);
				master = (EPMDocumentMaster) IdentityHelper.service.changeIdentity(master, identity);
				PersistenceHelper.manager.save(master);
			}
			SessionHelper.manager.setPrincipal(user.getName());
			user = null;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (user != null) {
				try {
					SessionHelper.manager.setPrincipal(user.getName());
				} catch (WTException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 获取对象的oid
	 * 
	 * @param WTObject
	 * @return String
	 */
	public static String object2Oid(WTObject obj) {
		return "OR:" + obj.getClass().getName() + ":" + obj.getPersistInfo().getObjectIdentifier().getId();
	}

	/**
	 * 获取oid所对应的对象
	 * 
	 * @param String
	 * @return WTObject
	 * @throws WTException
	 */
	public static WTObject oid2Object(String oid) throws WTException {
		return (WTObject) factory.getReference(oid).getObject();
	}

	/**
	 * 根据VR获取对应版本的对象
	 * 
	 * @param Class
	 * @param String
	 * @return Object
	 */
	@SuppressWarnings("deprecation")
	public static Object getObjByVR(Class queryClass, String vr) {
		try {
			if (StringUtils.isBlank(vr)) {
				return null;
			}
			QuerySpec qs = new QuerySpec(queryClass);
			qs.appendWhere(
					new SearchCondition(queryClass, "iterationInfo.branchId", SearchCondition.EQUAL, Long.valueOf(vr)));
			QueryResult qr = PersistenceHelper.manager.find(qs);
			qr = new LatestConfigSpec().process(qr);
			if (qr.hasMoreElements()) {
				return qr.nextElement();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据编号获取对象
	 * 
	 * @param String
	 * @param Class
	 * @return Object
	 */
	@SuppressWarnings("deprecation")
	public static Object getObjByNumber(String number, Class queryClass) {
		try {
			if (StringUtils.isBlank(number)) {
				return null;
			}
			QuerySpec qs = new QuerySpec(queryClass);
			qs.appendWhere(new SearchCondition(queryClass, "master>number", SearchCondition.EQUAL, number.trim()));
			QueryResult qr = PersistenceHelper.manager.find(qs);
			qr = new LatestConfigSpec().process(qr);
			if (qr.hasMoreElements()) {
				return qr.nextElement();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
