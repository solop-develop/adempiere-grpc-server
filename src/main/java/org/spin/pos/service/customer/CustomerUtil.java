package org.spin.pos.service.customer;

import java.util.Map;

import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Value;

public class CustomerUtil {

	/**
	 * 
	 * @param businessPartnerLocation
	 * @param transactionName
	 * @return
	 * @return MUser
	 */
	public static MUser getOfBusinessPartnerLocation(MBPartnerLocation businessPartnerLocation, String transactionName) {
		return new Query(
			businessPartnerLocation.getCtx(),
			MUser.Table_Name,
			"C_BPartner_Location_ID = ?",
			transactionName
		)
			.setParameters(businessPartnerLocation.getC_BPartner_Location_ID())
			.first()
		;
	}


	/**
	 * Get reference from column name and table
	 * @param tableId
	 * @param columnName
	 * @return
	 */
	private static int getReferenceId(int tableId, String columnName) {
		MColumn column = MTable.get(Env.getCtx(), tableId).getColumn(columnName);
		if(column == null) {
			return -1;
		}
		return column.getAD_Reference_ID();
	}

	/**
	 * Set additional attributes
	 * @param entity
	 * @param attributes
	 * @return void
	 */
	public static void setAdditionalAttributes(PO entity, Map<String, Value> attributes) {
		if(attributes != null) {
			attributes.keySet().forEach(key -> {
				Value attribute = attributes.get(key);
				int referenceId = getReferenceId(entity.get_Table_ID(), key);
				Object value = null;
				if(referenceId > 0) {
					value = ValueManager.getObjectFromProtoValue(
						attribute,
						referenceId
					);
				} 
				if(value == null) {
					value = ValueManager.getObjectFromProtoValue(
						attribute
					);
				}
				entity.set_ValueOfColumn(key, value);
			});
		}
	}

}
