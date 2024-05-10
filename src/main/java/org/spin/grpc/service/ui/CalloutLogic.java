package org.spin.grpc.service.ui;

import org.adempiere.core.domains.models.I_AD_Element;
import org.compiere.model.GridField;

public class CalloutLogic {

	/**
	 * Verify if a value has been changed
	 * @param gridField
	 * @return
	 */
	public static boolean isValidChange(GridField gridField) {
		//	Standard columns
		if(gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_Created)
				|| gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_CreatedBy)
				|| gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_Updated)
				|| gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_UpdatedBy)
				|| gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_UUID)) {
			return false;
		}
		//	Oly Displayed
		if(!gridField.isDisplayed()) {
			return false;
		}
		//	Key
		if(gridField.isKey()) {
			return false;
		}

		//	validate with old value
		if(gridField.getOldValue() != null
				&& gridField.getValue() != null
				&& gridField.getValue().equals(gridField.getOldValue())) {
			return false;
		}
		//	Default
		return true;
	}

}
