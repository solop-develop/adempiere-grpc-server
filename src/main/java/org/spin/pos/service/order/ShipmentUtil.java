package org.spin.pos.service.order;

import java.math.BigDecimal;
import java.util.Optional;

import org.adempiere.core.domains.models.I_C_OrderLine;
import org.adempiere.core.domains.models.I_M_InOutLine;
import org.compiere.model.Query;
import org.compiere.util.Env;

public class ShipmentUtil {

	/**
	 * Get Available Quantity for Return Order
	 * @param sourceOrderLineId
	 * @param sourceQuantity
	 * @param quantityToReturn
	 * @return
	 */
	public static BigDecimal getAvailableQuantityForShipment(int sourceOrderLineId, BigDecimal sourceQuantity, BigDecimal quantityToShipment) {
		BigDecimal quantity = getShipmentQuantity(sourceOrderLineId);
		BigDecimal availableQuantity = Optional.ofNullable(sourceQuantity).orElse(Env.ZERO)
			.subtract(Optional.ofNullable(quantity).orElse(Env.ZERO))
		;
		if(availableQuantity.compareTo(quantityToShipment) <= 0) {
			return availableQuantity;
		}
		return quantityToShipment;
	}

	public static BigDecimal getAvailableQuantityForShipment(int sourceOrderLineId, int shipmentLineId, BigDecimal sourceQuantity, BigDecimal quantityToReturn) {
		BigDecimal quantity = getReturnedQuantityExcludeShipment(sourceOrderLineId, shipmentLineId);
		BigDecimal availableQuantity = Optional.ofNullable(sourceQuantity).orElse(Env.ZERO)
			.subtract(Optional.ofNullable(quantity).orElse(Env.ZERO))
		;
		if(availableQuantity.compareTo(Env.ZERO) <= 0) {
			return Env.ZERO;
		}
		return availableQuantity;
	}

	/**
	 * Get sum of complete returned quantity for order
	 * @param sourceOrderLineId
	 * @return
	 */
	public static BigDecimal getShipmentQuantity(int sourceOrderLineId) {
		BigDecimal quantity = new Query(
			Env.getCtx(),
			I_C_OrderLine.Table_Name,
			I_M_InOutLine.COLUMNNAME_C_OrderLine_ID + " = ? "
				+ "AND EXISTS(SELECT 1 FROM C_Order o WHERE o.C_Order_ID = C_OrderLine.C_Order_ID)",
			null
		)
			.setParameters(sourceOrderLineId)
			.aggregate(I_C_OrderLine.COLUMNNAME_QtyEntered, Query.AGGREGATE_SUM)
		;
		return Optional.ofNullable(quantity).orElse(Env.ZERO);
	}

	public static BigDecimal getReturnedQuantityExcludeShipment(int sourceOrderLineId, int shipmentLineId) {
		final String whereClause = ""
			+ "M_InOutLine_ID <> ? "
			+ "AND C_OrderLine_ID = ? "
			+ "AND EXISTS("
			+ "	SELECT 1 "
			+ "	FROM C_Order AS o "
			+ "	INNER JOIN C_OrderLine AS ol "
			+ "	ON ol.C_Order_ID = o.C_Order_ID "
			+ "	WHERE o.C_Order_ID = ol.C_Order_ID "
			+ "	AND ol.C_OrderLine_ID = M_InOutLine.C_OrderLine_ID "
			+ ")"
		;
		BigDecimal quantity = new Query(
			Env.getCtx(),
			I_M_InOutLine.Table_Name,
			whereClause,
			null
		)
			.setParameters(shipmentLineId, sourceOrderLineId)
			.aggregate(I_M_InOutLine.COLUMNNAME_QtyEntered, Query.AGGREGATE_SUM)
		;
		return Optional.ofNullable(quantity).orElse(Env.ZERO);
	}

}
