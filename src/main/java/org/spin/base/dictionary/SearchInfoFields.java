package org.spin.base.dictionary;

import java.util.Arrays;
import java.util.List;

import org.adempiere.core.domains.models.I_M_Product;
import org.compiere.util.Util;

public class SearchInfoFields {

	public List<String> customSearhIngo = Arrays.asList(
		I_M_Product.Table_Name
	);

	public boolean isCustomSeachInfo(String tableName) {
		if (Util.isEmpty(tableName, true)) {
			return false;
		}
		if (customSearhIngo.contains(tableName)) {
			return true;
		}
		return false;
	}

}
