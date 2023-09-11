package ext.sinoboom.pipingIntegration.service;

import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.lang3.StringUtils;

import ext.sinoboom.pipingIntegration.util.CommUtil;
import wt.pom.WTConnection;
import wt.util.WTException;

public class GetComponentMaterialCodeService {

	private static ResultSet commonQuery(String sql) throws Exception {
		System.out.println(sql);
		WTConnection con = CommUtil.getWTConnection();
		Statement statement = con.prepareStatement(sql);
		ResultSet rs = statement.executeQuery(sql);
		return rs;
	}

	private static boolean common(String sql) throws Exception {
		System.out.println(sql);
		WTConnection con = CommUtil.getWTConnection();
		Statement statement = con.prepareStatement(sql);
		boolean execute = statement.execute(sql);
		return execute;
	}

	public static String generateNumber(String prefix) throws WTException {
		System.out.println("------------------编号前缀:" + prefix);
		String querySql = "SELECT MAX(TO_NUMBER(WTPARTNUMBER)) AS MAXPARTNUMBER FROM WTPARTMASTER WHERE WTPARTNUMBER LIKE '"
				+ prefix + "%' AND LENGTH(WTPARTNUMBER) = 12";
		String queryCadSql = "SELECT MAX(TO_NUMBER(DOCUMENTNUMBER)) AS MAXCADNUMBER FROM EPMDOCUMENTMASTER WHERE DOCUMENTNUMBER LIKE '"
				+ prefix + "%' AND LENGTH(DOCUMENTNUMBER) = 12";
		String querySinoSql = "SELECT MAXNUMBER FROM SINONUMBER WHERE PREFIXNUMBER LIKE '" + prefix + "%'";
		int seqLength = 12 - prefix.length();
		try {
			ResultSet rsSino = commonQuery(querySinoSql);
			ResultSet rsCad = commonQuery(queryCadSql);
			ResultSet rs = commonQuery(querySql);
			String maxNumber = "";
			String maxCadNumber = "";
			String maxSinoNumber = "";
			String newSeq = "1";
			String maxSeq = "";
			Integer PREFIXNUMBER = 0;
			Integer MAXNUMBER = 0;
			String newNumber = "";
			boolean rsSinoBoolean = rsSino.next();

			if (rs.next()) {
				maxNumber = rs.getString("MAXPARTNUMBER");
			}
			if (rsCad.next()) {
				maxCadNumber = rsCad.getString("MAXCADNUMBER");
			}

			// 取cad和工件最大值的最大值
			if (StringUtils.isNotBlank(maxNumber)) {
				if (StringUtils.isNotBlank(maxCadNumber)) {
					newNumber = Long.valueOf(maxNumber) > Long.valueOf(maxCadNumber) ? maxNumber : maxCadNumber;
				} else {
					newNumber = StringUtils.isBlank(maxNumber) ? maxCadNumber : maxNumber;
				}
			} else {
				throw new Exception("没有对应的部件");
			}

			System.out.println("最大编号：" + newNumber);
			if (StringUtils.isNotBlank(newNumber)) {
				// 取后四位的值
				maxSeq = StringUtils.mid(newNumber, prefix.length(), seqLength);
				if (rsSinoBoolean) {
					maxSinoNumber = rsSino.getString("MAXNUMBER");
				}
				Integer maxSinoNumberI = isNumeric(maxSinoNumber) ? Integer.parseInt(maxSinoNumber) : 0;
				maxSeq = Long.valueOf(maxSeq) > Long.valueOf(maxSinoNumberI) ? maxSeq : String.valueOf(maxSinoNumberI);
				Integer maxSeqI = Integer.valueOf(maxSeq);
				// 最大值加一
				newSeq = (++maxSeqI).toString();
			}
			while (newSeq.length() < seqLength) {
				newSeq = "0" + newSeq;
			}
			PREFIXNUMBER = Integer.valueOf(prefix);
			MAXNUMBER = Integer.valueOf(newSeq);

			// sino表查询到的最大值
			// 在sino表中查询，如果存在值
			if (rsSinoBoolean) {
				String updateSinoSql = "UPDATE SINONUMBER SET MAXNUMBER = '" + MAXNUMBER + "' WHERE PREFIXNUMBER = '"
						+ PREFIXNUMBER + "'";
				boolean common = common(updateSinoSql);
			}
			// 如果sino表中不存在最大值
			else {
				String insertSinoSql = "INSERT INTO SINONUMBER(INDEXTWO,PREFIXNUMBER,MAXNUMBER) VALUES (SINONUMBERINDEX.NEXTVAL,'"
						+ PREFIXNUMBER + "','" + MAXNUMBER + "')";
				boolean common = common(insertSinoSql);
			}
			return prefix + newSeq;
		} catch (Exception e) {
			e.printStackTrace();
			throw new WTException("生成前缀为" + prefix + "的编号出错,请检查你的编号");
		}
	}

	private static boolean isNumeric(String str) {
		return str.matches("^\\d+$");
	}
}
