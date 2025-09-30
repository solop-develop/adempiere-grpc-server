package org.spin.pos.service.order;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MCurrency;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
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
		MPOS pos = new MPOS(order.getCtx(), order.getC_POS_ID(), order.get_TrxName());
		if(pos.get_ValueAsInt("DefaultDiscountCharge_ID") <= 0) {
			throw new AdempiereException("@DefaultDiscountCharge_ID@ @NotFound@");
		}
		//	Validate Discount
		Optional<BigDecimal> baseAmount = Arrays.asList(order.getLines())
			.stream()
			.filter(ordeLine -> {
				return ordeLine.getC_Charge_ID() != pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_DefaultDiscountCharge_ID);
			})
			.map(ordeLine -> ordeLine.getLineNetAmt())
			.collect(Collectors.reducing(BigDecimal::add))
		;
		//	Get Base amount
		if(baseAmount.isPresent()
				&& baseAmount.get().compareTo(Env.ZERO) > 0) {
			int precision = MCurrency.getStdPrecision(order.getCtx(), order.getC_Currency_ID());
			BigDecimal finalPrice = getFinalPrice(baseAmount.get(), discountRateOff, precision);
			createDiscountLine(pos, order, baseAmount.get().subtract(finalPrice), transactionName);
			//	Set Discount Rate
			order.set_ValueOfColumn("FlatDiscount", discountRateOff);
			order.saveEx(transactionName);
		} else {
			deleteDiscountLine(pos, order, transactionName);
		}
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
		if(pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_DefaultDiscountCharge_ID) <= 0) {
			throw new AdempiereException("@DefaultDiscountCharge_ID@ @NotFound@");
		}
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
			deleteDiscountLine(pos, order, transactionName);
			return;
		}
		//	
		int precision = MCurrency.getStdPrecision(order.getCtx(), order.getC_Currency_ID());
		BigDecimal discountRateOff = getDiscount(baseAmount, baseAmount.add(discountAmountOff), precision).negate();
		if(maximumDiscountAllowed.compareTo(Env.ZERO) > 0 && discountRateOff.compareTo(maximumDiscountAllowed) > 0) {
			throw new AdempiereException("@POS.MaximumDiscountAllowedExceeded@");
		}
		//	Create Discount line
		createDiscountLine(pos, order, discountAmountOff, transactionName);
		//	Set Discount Rate
		order.set_ValueOfColumn("FlatDiscount", discountRateOff);
		order.saveEx(transactionName);
	}
	
	/**
	 * Delete Discount Line
	 * @param pos
	 * @param order
	 * @param transactionName
	 */
	private static void deleteDiscountLine(MPOS pos, MOrder order, String transactionName) {
		Optional<MOrderLine> maybeOrderLine = Arrays.asList(order.getLines())
			.parallelStream()
			.filter(ordeLine -> {
				return ordeLine.getC_Charge_ID() == pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_DefaultDiscountCharge_ID);
			})
			.findFirst()
		;
		maybeOrderLine.ifPresent(discountLine -> discountLine.deleteEx(true,transactionName));
	}
	
	/**
	 * Create Discount Line
	 * @param pos
	 * @param order
	 * @param amount
	 * @param transactionName
	 */
	private static void createDiscountLine(MPOS pos, MOrder order, BigDecimal amount, String transactionName) {
		Optional<MOrderLine> maybeOrderLine = Arrays.asList(order.getLines())
			.parallelStream()
			.filter(ordeLine -> {
				return ordeLine.getC_Charge_ID() == pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_DefaultDiscountCharge_ID);
			})
			.findFirst()
		;
		MOrderLine discountOrderLine = null;
		if(maybeOrderLine.isPresent()) {
			discountOrderLine = maybeOrderLine.get();
		} else {
			discountOrderLine = new MOrderLine(order);
			discountOrderLine.setC_Charge_ID(pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_DefaultDiscountCharge_ID));
		}
		discountOrderLine.setQty(Env.ONE);
		discountOrderLine.setPrice(amount.negate());
		discountOrderLine.setM_AttributeSetInstance_ID(0);
		//	
		discountOrderLine.saveEx(transactionName);
	}

}
