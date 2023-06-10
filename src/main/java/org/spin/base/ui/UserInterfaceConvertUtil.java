package org.spin.base.ui;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.X_AD_Tree;
import org.compiere.model.MRefList;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.TreeType;
import org.spin.base.util.ValueUtil;

public class UserInterfaceConvertUtil {

	public static TreeType.Builder convertTreeType(String value) {
		TreeType.Builder builder = TreeType.newBuilder();
		if (Util.isEmpty(value, true)) {
			return builder;
		}
		MRefList treeType = MRefList.get(
			Env.getCtx(),
			X_AD_Tree.TREETYPE_AD_Reference_ID,
			value,
			null
		);
		return convertTreeType(treeType);
	}

	public static TreeType.Builder convertTreeType(MRefList treeType) {
		TreeType.Builder builder = TreeType.newBuilder();
		if (treeType == null || treeType.getAD_Ref_List_ID() <= 0) {
			return builder;
		}

		String name = treeType.getName();
		String description = treeType.getDescription();

		// set translated values
		if (!Env.isBaseLanguage(Env.getCtx(), "")) {
			name = treeType.get_Translation(I_AD_Ref_List.COLUMNNAME_Name);
			description = treeType.get_Translation(I_AD_Ref_List.COLUMNNAME_Description);
		}

		builder.setId(treeType.getAD_Ref_List_ID())
			.setUuid(ValueUtil.validateNull(treeType.getUUID()))
			.setValue(ValueUtil.validateNull(treeType.getValue()))
			.setName(
				ValueUtil.validateNull(name)
			)
			.setDescription(
				ValueUtil.validateNull(description)
			)
		;

		return builder;
	}

}
