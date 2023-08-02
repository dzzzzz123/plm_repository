package ext.sinoboom.ppmService.servlet;

import org.apache.commons.lang3.StringUtils;

import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.folder.Folder;
import wt.folder.FolderHelper;
import wt.inf.container.WTContainerRef;
import wt.pdmlink.PDMLinkProduct;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;

public class TestFolder {

	public static void main(String[] args) throws WTException {

		String pathString = "/Default";
		PDMLinkProduct product = (PDMLinkProduct) getObjByOR(PDMLinkProduct.class, "157559");
		WTContainerRef containerRef = WTContainerRef.newWTContainerRef(product);
		Folder folder = FolderHelper.service.getFolder(pathString, containerRef);
		Long oid = getOid(folder);
		System.out.println(oid);

	}

	private static Long getOid(Folder folder) throws WTException {
		Long id = PersistenceHelper.getObjectIdentifier(folder).getId();
		return id;

	}

	/**
	 * 根据id获取对象
	 */
	private static Object getObjByOR(Class queryClass, String or) {
		try {
			if (StringUtils.isBlank(or)) {
				return null;
			}
			QuerySpec qs = new QuerySpec(queryClass);
			qs.appendWhere(new SearchCondition(queryClass, "thePersistInfo.theObjectIdentifier.id",
					SearchCondition.EQUAL, Long.valueOf(or)));
			QueryResult qr = PersistenceHelper.manager.find(qs);
			if (qr.hasMoreElements()) {
				return qr.nextElement();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

}
