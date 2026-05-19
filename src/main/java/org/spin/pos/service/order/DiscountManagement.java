package org.spin.pos.service.order;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MCurrency;
import org.compiere.model.MOrder;
import org.compiere.model.MPOS;
import org.compiere.util.Env;
import org.spin.pos.service.pos.AccessManagement;
import org.spin.pos.service.pos.POS;
import org.spin.pos.util.ColumnsAdded;

public class DiscountManagement {

	/**
	 * Get discount based on base price and price list
	 * @param finalPrice
	 * @param basePrice
	 * @param precision
	 * @return
	 */
	public static BigDecimal getDiscount(BigDecimal basePrice, BigDecimal finalPrice, int precision) {
		finalPrice = Optional.ofNullable(finalPrice).orElse(Env.ZERO);
		basePrice = Optional.ofNullable(basePrice).orElse(Env.ZERO);
		BigDecimal discount = Env.ZERO;
		if (basePrice.compareTo(Env.ZERO) != 0) {
			discount = basePrice.subtract(finalPrice);
			discount = discount.divide(basePrice, MathContext.DECIMAL128);
			discount = discount.multiply(Env.ONEHUNDRED);
		}
		if (discount.scale() > precision) {
			discount = discount.setScale(precision, RoundingMode.HALF_UP);
		}
		return discount;
	}


	/**
	 * Get final price based on base price and discount applied
	 * @param basePrice
	 * @param discount
	 * @return
	 */
	public static BigDecimal getFinalPrice(BigDecimal basePrice, BigDecimal discount, int precision) {
		basePrice = Optional.ofNullable(basePrice).orElse(Env.ZERO);
		discount = Optional.ofNullable(discount).orElse(Env.ZERO);
		//	A = 100 - discount
		BigDecimal multiplier = Env.ONE.subtract(discount.divide(Env.ONEHUNDRED, MathContext.DECIMAL128));
		//	B = A / 100
		BigDecimal finalPrice = basePrice.multiply(multiplier);
		finalPrice = finalPrice.setScale(precision, RoundingMode.HALF_UP);
		return finalPrice;
	}


	/**
	 * Configure Discount for all lines
	 * @param order
	 */
	public static void configureDiscount(MOrder order, BigDecimal discountRate, String transactionName) {
		MPOS pos = POS.validateAndGetPOS(order.getC_POS_ID(), false);
		Arrays.asList(order.getLines())
			.forEach(orderLine -> {
				OrderManagement.updateOrderLine(
					transactionName,
					pos,
					orderLine.getC_OrderLine_ID(),
					null,
					null,
					discountRate,
					false,
					0,
					0
				);
			})
		;
	}

	/**
	 * Configure Discount Off for order
	 * @param order
	 */
	public static void configureDiscountRateOff(MOrder order, BigDecimal discountRateOff, String transactionName) {
		if(discountRateOff == null) {
			return;
		}
		order.set_ValueOfColumn("FlatDiscount", discountRateOff);
		order.saveEx(transactionName);
	}


	/**
	 * Configure Discount Off for order
	 * @param order
	 */
	public static void configureDiscountAmountOff(MOrder order, BigDecimal discountAmountOff, String transactionName) {
		if(discountAmountOff == null) {
			return;
		}
		MPOS pos = new MPOS(order.getCtx(), order.getC_POS_ID(), order.get_TrxName());
		int defaultDiscountChargeId = pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_DefaultDiscountCharge_ID);
		boolean isAllowsApplyDiscount = AccessManagement.getBooleanValueFromPOS(pos, Env.getAD_User_ID(Env.getCtx()), ColumnsAdded.COLUMNNAME_IsAllowsApplyDiscount);
		if(!isAllowsApplyDiscount) {
			throw new AdempiereException("@POS.ApplyDiscountNotAllowed@");
		}
		BigDecimal maximumDiscountAllowed = AccessManagement.getBigDecimalValueFromPOS(pos, Env.getAD_User_ID(Env.getCtx()), ColumnsAdded.COLUMNNAME_MaximumDiscountAllowed);
		BigDecimal baseAmount = Optional.ofNullable(Arrays.asList(order.getLines())
			.stream()
			.filter(orderLine -> {
				return orderLine.getC_Charge_ID() != defaultDiscountChargeId || defaultDiscountChargeId == 0;
			})
			.map(ordeLine -> ordeLine.getLineNetAmt())
			.reduce(BigDecimal.ZERO, BigDecimal::add)).orElse(Env.ZERO)
		;
		if(baseAmount.compareTo(Env.ZERO) <= 0) {
			return;
		}
		//
		int precision = MCurrency.getStdPrecision(order.getCtx(), order.getC_Currency_ID());
		BigDecimal discountRateOff = getDiscount(baseAmount, baseAmount.add(discountAmountOff), precision).negate();
		if(maximumDiscountAllowed.compareTo(Env.ZERO) > 0 && discountRateOff.compareTo(maximumDiscountAllowed) > 0) {
			throw new AdempiereException("@POS.MaximumDiscountAllowedExceeded@");
		}
		//	Set Discount Rate
		order.set_ValueOfColumn("FlatDiscount", discountRateOff);
		order.saveEx(transactionName);
	}

}
