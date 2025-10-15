/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/

package org.spin.pos.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import com.google.protobuf.Empty;
import org.adempiere.core.domains.models.I_AD_PrintFormatItem;
import org.adempiere.core.domains.models.I_C_OrderLine;
import org.adempiere.core.domains.models.I_C_POS;
import org.adempiere.core.domains.models.I_M_DiscountSchema;
import org.adempiere.core.domains.models.I_M_InOutLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDiscountSchema;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPayment;
import org.compiere.model.MTable;
import org.compiere.model.MUOMConversion;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.AvailableDiscountSchema;
import org.spin.backend.grpc.pos.AvailableOrderLine;
import org.spin.backend.grpc.pos.CancelOnlinePaymentRequest;
import org.spin.backend.grpc.pos.CancelOnlinePaymentResponse;
import org.spin.backend.grpc.pos.CreateGiftCardLineRequest;
import org.spin.backend.grpc.pos.CreateGiftCardRequest;
import org.spin.backend.grpc.pos.DeleteGiftCardLineRequest;
import org.spin.backend.grpc.pos.DeleteGiftCardRequest;
import org.spin.backend.grpc.pos.GetValidGiftCardRequest;
import org.spin.backend.grpc.pos.GiftCard;
import org.spin.backend.grpc.pos.GiftCardLine;
import org.spin.backend.grpc.pos.InfoOnlinePaymentRequest;
import org.spin.backend.grpc.pos.InfoOnlinePaymentResponse;
import org.spin.backend.grpc.pos.ListAvailableDiscountsRequest;
import org.spin.backend.grpc.pos.ListAvailableDiscountsResponse;
import org.spin.backend.grpc.pos.ListAvailableOrderLinesForGiftCardRequest;
import org.spin.backend.grpc.pos.ListAvailableOrderLinesForGiftCardResponse;
import org.spin.backend.grpc.pos.ListAvailableOrderLinesForRMARequest;
import org.spin.backend.grpc.pos.ListAvailableOrderLinesForRMAResponse;
import org.spin.backend.grpc.pos.ListGiftCardLinesRequest;
import org.spin.backend.grpc.pos.ListGiftCardLinesResponse;
import org.spin.backend.grpc.pos.ListGiftCardsRequest;
import org.spin.backend.grpc.pos.ListGiftCardsResponse;
import org.spin.backend.grpc.pos.ProcessOnlinePaymentRequest;
import org.spin.backend.grpc.pos.ProcessOnlinePaymentResponse;
import org.spin.backend.grpc.pos.ShipmentLine;
import org.spin.backend.grpc.pos.UpdateGiftCardLineRequest;
import org.spin.backend.grpc.pos.UpdateGiftCardRequest;
import org.spin.backend.grpc.pos.UpdateShipmentLineRequest;
import org.spin.base.util.DocumentUtil;
import org.spin.pos.service.order.RMAUtil;
import org.spin.pos.service.order.ShipmentUtil;
import org.spin.pos.util.POSConvertUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.base.RecordUtil;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;

public class POSLogic {

	public static ListAvailableDiscountsResponse.Builder listAvailableDiscounts(ListAvailableDiscountsRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}

		ListAvailableDiscountsResponse.Builder builderList = ListAvailableDiscountsResponse.newBuilder();
		final String TABLE_NAME = "C_POSDiscountAllocation";
		if (MTable.getTable_ID(TABLE_NAME) <= 0) {
			return builderList;
		}

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Dynamic where clause
		//	Aisle Seller
		int posId = request.getPosId();
		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			TABLE_NAME,
			"C_POS_ID = ?",
			null
		)
			.setParameters(posId)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setOrderBy(I_AD_PrintFormatItem.COLUMNNAME_SeqNo)
		;

		int count = query.count();
		query
			.setLimit(limit, offset)
			.list()
			.forEach(availableDiscountSchema -> {
				MDiscountSchema discountSchema = MDiscountSchema.get(
					Env.getCtx(),
					availableDiscountSchema.get_ValueAsInt(
						I_M_DiscountSchema.COLUMNNAME_M_DiscountSchema_ID
					)
				);

				AvailableDiscountSchema.Builder builder = AvailableDiscountSchema.newBuilder()
					.setId(
						discountSchema.getM_DiscountSchema_ID()
					)
					.setKey(
						StringManager.getValidString(
							discountSchema.getName()
						)
					)
					.setName(
						StringManager.getValidString(
							discountSchema.getName()
						)
					)
					.setIsPosRequiredPin(
						availableDiscountSchema.get_ValueAsBoolean(
							I_C_POS.COLUMNNAME_IsPOSRequiredPIN
						)
					)
					.setFlatDiscountPercetage(
						NumberManager.getBigDecimalToString(
							discountSchema.getFlatDiscount()
						)
					)
				;
				builderList.addDiscounts(builder);
			})
		;
		//	
		builderList.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builderList;
	}



	public static ShipmentLine.Builder updateShipmentLine(UpdateShipmentLineRequest request) {
		//	Validate Order
		if(request.getShipmentId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @M_InOut_ID@");
		}
		//	Validate Product and charge
		if(request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @M_InOutLine_ID@");
		}

		MInOut shipmentHeader = new MInOut(Env.getCtx(), request.getShipmentId(), null);
		if (shipmentHeader == null || shipmentHeader.getM_InOut_ID() <= 0) {
			throw new AdempiereException("@M_InOut_ID@ @NotFound@");
		}
		if(!DocumentUtil.isDrafted(shipmentHeader)) {
			throw new AdempiereException("@M_InOut_ID@ @Processed@");
		}

		AtomicReference<MInOutLine> shipmentLineReference = new AtomicReference<MInOutLine>();
		Trx.run(transactionName -> {
			MInOutLine shipmentLine = new Query(
				Env.getCtx(),
				I_M_InOutLine.Table_Name,
				I_M_InOutLine.COLUMNNAME_M_InOutLine_ID + " = ?",
				transactionName
			)
				.setParameters(request.getId())
				.setClient_ID()
				.first()
			;

			if (shipmentLine == null || shipmentLine.getM_InOutLine_ID() <= 0) {
				throw new AdempiereException("@M_InOutLine_ID@ @NotFound@");
			}
			//	Validate processed Order
			if(shipmentLine.isProcessed()) {
				throw new AdempiereException("@M_InOutLine_ID@ @Processed@");
			}

			// Validate quantity
			MOrderLine sourcerOrderLine = new MOrderLine(Env.getCtx(), shipmentLine.getC_OrderLine_ID(), transactionName);
			if(sourcerOrderLine == null || sourcerOrderLine.getC_OrderLine_ID() <= 0) {
				throw new AdempiereException("@C_OrderLine_ID@ @NotFound@");
			}

			BigDecimal quantity = Optional.ofNullable(
				NumberManager.getBigDecimalFromString(
					request.getQuantity()
				)
			).orElse(Env.ZERO);
			BigDecimal availableQuantity = ShipmentUtil.getAvailableQuantityForShipment(
				sourcerOrderLine.getC_OrderLine_ID(),
				shipmentLine.getM_InOutLine_ID(),
				sourcerOrderLine.getQtyEntered(),
				quantity
			);
			if (availableQuantity.compareTo(Env.ZERO) <= 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}
			if (availableQuantity.compareTo(quantity) < 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}

			BigDecimal convertedQuantity = MUOMConversion.convertProductFrom(
				shipmentLine.getCtx(),
				shipmentLine.getM_Product_ID(),
				shipmentLine.getC_UOM_ID(),
				quantity
			);
			shipmentLine.setQty(convertedQuantity);

			shipmentLine.setDescription(
				request.getDescription()
			);

			//	Save Line
			shipmentLine.saveEx();
			shipmentLineReference.set(shipmentLine);
		});

		//	Convert Line
		return POSConvertUtil.convertShipmentLine(
			shipmentLineReference.get()
		);
	}


	public static GiftCard.Builder getValidGiftCard(GetValidGiftCardRequest request) {
		if (Util.isEmpty(request.getSearchValue(), true)) {
			throw new AdempiereException("@FillMandatory@ @SearchValue@");
		}
		PO giftCard = new Query(
			Env.getCtx(),
			"ECA14_GiftCard",
			"UPPER(UUID) = UPPER(?)",
			null
		)
			.setParameters(request.getSearchValue())
			.first()
		;
		if (giftCard == null || giftCard.get_ID() <= 0) {
			throw new AdempiereException("@ECA14_GiftCard_ID@ @NotFound@");
		}
		if (giftCard.get_ValueAsBoolean("Processing")) {
			throw new AdempiereException("@ECA14_GiftCard_ID@ @Processing@");
		}
		if (giftCard.get_ValueAsBoolean("Processed")) {
			throw new AdempiereException("@ECA14_GiftCard_ID@ @Processed@");
		}

		return POSConvertUtil.convertGiftCard(giftCard);
	}


	public static GiftCard.Builder getGiftCard(int id, String transactionName) {
		PO giftCard = RecordUtil.getEntity(
			Env.getCtx(),
			"ECA14_GiftCard",
			id,
			transactionName
		);
		return POSConvertUtil.convertGiftCard(giftCard);
	}
	public static GiftCard.Builder createGiftCard(CreateGiftCardRequest request) {
		Properties context = Env.getCtx();
		MTable table = MTable.get(context, "ECA14_GiftCard");
		if (table == null) {
			throw new AdempiereException("@TableName@: ECA14_GiftCard @NotFound@");
		}
		if (request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		if (request.getOrderId() <= 0) {
			throw new AdempiereException("@C_Order_ID@ @NotFound@");
		}
		if (request.getIsPrepayment()) {
			BigDecimal amount = NumberManager.getBigDecimalFromString(
				request.getAmount()
			);
			if (amount == null || amount.signum() == 0) {
				throw new AdempiereException("@FillMandatory@ @Amount@");
			}
		}
		validateCanCreateGiftCard(request.getOrderId(), request.getIsPrepayment());
		AtomicReference<PO> maybeGiftCard = new AtomicReference<PO>();
		Trx.run(transactionName -> {
			MOrder order = new MOrder(context, request.getOrderId(), transactionName);
			if (!DocumentUtil.isCompleted(order)
					&& !DocumentUtil.isClosed(order)) {
				throw new AdempiereException("@DocStatus@ @InValid@");
			}
			// final String whereClause = "Processed = 'N' "
			// 	+ "AND Processing = 'N' "
			// 	+ "AND C_Order_ID = ?"
			// ;
			PO giftCard = table.getPO(0, transactionName);

			BigDecimal amount =  Optional.ofNullable(NumberManager.getBigDecimalFromString(request.getAmount())).orElse(Env.ZERO);
			giftCard.set_ValueOfColumn("Description", order.getDescription());
			giftCard.set_ValueOfColumn("C_Order_ID", request.getOrderId());
			giftCard.set_ValueOfColumn("C_BPartner_ID", order.getC_BPartner_ID());
			giftCard.set_ValueOfColumn("C_ConversionType_ID", order.getC_ConversionType_ID());
			giftCard.set_ValueOfColumn("C_Currency_ID" , order.getC_Currency_ID());
			giftCard.set_ValueOfColumn("DateDoc", order.getDateOrdered());
			giftCard.set_ValueOfColumn("Amount", amount);
			giftCard.set_ValueOfColumn("IsPrepayment", request.getIsPrepayment());
			giftCard.saveEx(transactionName);
			//TODO: Check how to validate amount for Prepayment Gift Card
			if (!request.getIsPrepayment() && request.getIsCreateLinesFromOrder()) {
				if (!createGiftCardLines(giftCard, transactionName)) {
					throw new AdempiereException("@QtyInsufficient@");
				}
			}
			maybeGiftCard.set(giftCard);
		});

		return POSConvertUtil.convertGiftCard(maybeGiftCard.get());
	}
	private static void validateCanCreateGiftCard(int orderId, boolean isPrepayment) {
		if (isPrepayment) {
			//TODO: Prepayment Validations
			;
		} else {
			if (orderId <= 0) {
				throw new AdempiereException("@C_Order_ID@ @NotFound@");
			}
			String whereClause = "C_Order_ID = ?";
			BigDecimal orderQtyEntered = new Query(Env.getCtx(), MOrderLine.Table_Name,whereClause, null)
				.setParameters(orderId)
				.sum(I_C_OrderLine.COLUMNNAME_QtyEntered)
			;

			whereClause = "C_OrderLine_ID IN (SELECT C_OrderLine_ID FROM C_OrderLine ol WHERE ol.C_Order_ID = ?)";
			BigDecimal giftCardQtyEntered = new Query(Env.getCtx(), "ECA14_GiftCardLine",whereClause, null)
				.setParameters(orderId)
				.sum(I_C_OrderLine.COLUMNNAME_QtyEntered)
			;
			if (giftCardQtyEntered.compareTo(orderQtyEntered) >= 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}
		}
	}
	public static GiftCardLine.Builder getGiftCardLine (int id, String transactionName) {
		PO giftCardLine = RecordUtil.getEntity(
			Env.getCtx(),
			"ECA14_GiftCardLine",
			id,
			transactionName
		);
		return POSConvertUtil.convertGiftCardLine(giftCardLine);
	}

	private static boolean createGiftCardLines(PO giftCard, String transactionName) {
		Properties context = Env.getCtx();
		MTable table = MTable.get(context, "ECA14_GiftCardLine");
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@TableName@ ECA14_GiftCardLine @NotFound@");
		}
		int orderId = giftCard.get_ValueAsInt(MOrder.COLUMNNAME_C_Order_ID);
		if (orderId <=0 ) {
			throw new AdempiereException("@C_Order_ID@ @NotFound@");
		}
		MOrder order = new MOrder(context, orderId, null);
		if (!order.getDocStatus().equals(MOrder.DOCSTATUS_Completed)
				&& !order.getDocStatus().equals(MOrder.DOCSTATUS_Closed)) {
			throw new AdempiereException("@DocStatus@ @InValid@");
		}
		// TODO: Validate POS Information
		int giftCardId = giftCard.get_ID();
		List<MOrderLine> orderLines = Arrays.asList(order.getLines());
		String whereClause = MOrderLine.COLUMNNAME_C_OrderLine_ID + " = ? " +
			" AND ECA14_GiftCard_ID = ? "
		;
		AtomicReference<Boolean> existsLineOrAvailableQty = new AtomicReference<>(false);
		orderLines.forEach(orderLine ->{
			PO giftCardLine = new Query(
				context,
				table.getTableName(),
				whereClause,
				transactionName
			)
				.setParameters(orderLine.getC_OrderLine_ID(), giftCardId)
				.first()
			;
			BigDecimal availableQty = getAvailableQtyForGiftCardLine(
				orderLine.getC_OrderLine_ID(),
				orderLine.getQtyOrdered()
			);
			if (giftCardLine == null) {
				if (availableQty.signum() <= 0) {
					return;
				}
				giftCardLine = table.getPO(0, transactionName);
				giftCardLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_M_Product_ID, orderLine.getM_Product_ID());
				giftCardLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_Description, orderLine.getDescription());
				giftCardLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_C_OrderLine_ID, orderLine.getC_OrderLine_ID());
				giftCardLine.set_ValueOfColumn("ECA14_GiftCard_ID", giftCardId);
			} else {
				//In case the GiftCardLine already existed and was evaluated for the available quantity
				BigDecimal giftCardLineQty = Optional.ofNullable(
					(BigDecimal) giftCardLine.get_Value("QtyEntered")
				)
				.orElse(Env.ZERO);
				availableQty = availableQty.add(giftCardLineQty);
			}

			giftCardLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_QtyEntered, orderLine.getQtyEntered().min(availableQty));
			giftCardLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_QtyOrdered , orderLine.getQtyOrdered());
			giftCardLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_C_UOM_ID, orderLine.getC_UOM_ID());
			giftCardLine.set_ValueOfColumn("Amount", orderLine.getLineNetAmt());
			giftCardLine.saveEx();
			existsLineOrAvailableQty.set(true);
		});
		return existsLineOrAvailableQty.get();
	}

	public static ListGiftCardsResponse.Builder listGiftCards(ListGiftCardsRequest request) {
		if (request.getOrderId() <= 0) {
			throw new AdempiereException("@C_Order_ID@ @NotFound@");
		}
		ListGiftCardsResponse.Builder builder = ListGiftCardsResponse.newBuilder();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		MTable table = MTable.get(Env.getCtx(), "ECA14_GiftCard");
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@TableName@ ECA14_GiftCard @NotFound@");
		}
		//TODO: validate other parameters for filter
		String whereClause =  "C_Order_ID = ? ";
		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			table.getTableName(),
			whereClause,
			null
		)
			.setParameters(request.getOrderId())
			.setClient_ID()
			.setOnlyActiveRecords(true)
		;
		int count = query.count();
		query
			.setLimit(limit, offset)
			.<MInOutLine>list()
			.forEach(line -> {
				GiftCard.Builder giftCardBuilder = POSConvertUtil.convertGiftCard(line);
				builder.addGiftCards(giftCardBuilder);
			});
		//
		builder.setRecordCount(count);
		//	Set page token
		if (LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;
	}
	public static GiftCard.Builder updateGiftCard (UpdateGiftCardRequest request) {
		//TODO: Implement
		return GiftCard.newBuilder();
	}
	public static Empty.Builder deleteGiftCard(DeleteGiftCardRequest request) {
		//TODO: Implement
		return Empty.newBuilder();
	}




	//TODO: Validate how will the qty and amounts be created
	public static GiftCardLine.Builder createAndConvertGiftCardLine(CreateGiftCardLineRequest request) {
		Properties ctx = Env.getCtx();
		MTable table = MTable.get(ctx, "ECA14_GiftCardLine");
		if (table == null) {
			throw new AdempiereException("@TableName@ @NotFound@");
		}
		if (request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		if (request.getGiftCardId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @ECA14_GiftCard_ID@");
		}
		if (request.getOrderId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Order_ID@");
		}
		if (request.getOrderLineId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_OrderLine_ID@");
		}
		//TODO: Validate POS Information
		AtomicReference<PO> maybeGiftCardLine = new AtomicReference<PO>();
		Trx.run( transactionName -> {
			MOrderLine orderLine = new MOrderLine(ctx, request.getOrderLineId(), transactionName);
			BigDecimal availableQty = getAvailableQtyForGiftCardLine(request.getOrderLineId(), orderLine.getQtyOrdered());
			if (availableQty.signum() <= 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}
			PO giftCardLine = new Query(
				Env.getCtx(),
				table.getTableName(),
				"Processed = 'N' " +
				" AND C_OrderLine_ID = ? " +
				" AND ECA14_GiftCard_ID = ? ",
				transactionName
			)
				.setParameters(request.getOrderLineId(), request.getGiftCardId())
				.first()
			;
			BigDecimal qtyEntered = Optional.ofNullable(NumberManager.getBigDecimalFromString(request.getQuantityEntered()))
				.orElse(orderLine.getQtyEntered());
			if (qtyEntered.compareTo(Env.ZERO) == 0){
				throw new AdempiereException("@QtyInsufficient@");
			}

			BigDecimal qtyOrdered = Optional.ofNullable(NumberManager.getBigDecimalFromString(request.getQuantityOrdered()))
					.orElse(orderLine.getQtyOrdered());
			if (giftCardLine == null) {
				if (availableQty.subtract(qtyEntered).signum() < 0) {
					throw new AdempiereException("@QtyInsufficient@");
				}
				giftCardLine = table.getPO(0, transactionName);
				giftCardLine.set_ValueOfColumn("M_Product_ID", orderLine.getM_Product_ID());
				giftCardLine.set_ValueOfColumn("C_OrderLine_ID", request.getOrderLineId());
				giftCardLine.set_ValueOfColumn("ECA14_GiftCard_ID", request.getGiftCardId());
				giftCardLine.set_ValueOfColumn("Description", orderLine.getDescription());
			} else {
				//In case the GiftCardLine already existed and was evaluated for the available quantity
				BigDecimal giftCardLineQty = Optional.ofNullable((BigDecimal) giftCardLine.get_Value("QtyEntered"))
					.orElse(Env.ZERO);
				availableQty = availableQty.add(giftCardLineQty);
				giftCardLineQty = giftCardLineQty.add(Env.ONE);
				if (availableQty.compareTo(giftCardLineQty) < 0) {
					throw new AdempiereException("@QtyInsufficient@");
				}
				qtyEntered = giftCardLineQty;
			}

			giftCardLine.set_ValueOfColumn("QtyEntered", qtyEntered);
			giftCardLine.set_ValueOfColumn("QtyOrdered", qtyOrdered);
			giftCardLine.set_ValueOfColumn("C_UOM_ID", orderLine.getC_UOM_ID());
			giftCardLine.set_ValueOfColumn("Amount", NumberManager.getBigDecimalFromString(request.getAmount()));
			giftCardLine.saveEx();
			maybeGiftCardLine.set(giftCardLine);
		});
		return POSConvertUtil.convertGiftCardLine(maybeGiftCardLine.get());
	}

	private static BigDecimal getAvailableQtyForGiftCardLine(int orderLineId, BigDecimal qtyOrdered) {
		BigDecimal availableQty = Env.ZERO;
		if (qtyOrdered == null || qtyOrdered.signum() == 0) {
			return  availableQty;
		}
		// Validate already existing GiftCards for the Order Line
		String whereClause = MOrderLine.COLUMNNAME_C_OrderLine_ID + " = ? ";
		BigDecimal usedQty = Optional.ofNullable(
			new Query(Env.getCtx(), "ECA14_GiftCardLine", whereClause, null)
				.setParameters(orderLineId)
				.sum("QtyEntered")
		).orElse(Env.ZERO);
		availableQty = qtyOrdered.subtract(usedQty);
		return availableQty;
	}
	public static ListGiftCardLinesResponse.Builder listGiftCardLines(ListGiftCardLinesRequest request) {
		if(request.getGiftCardId() <= 0) {
			throw new AdempiereException("@ECA14_GiftCard_ID@ @NotFound@");
		}
		ListGiftCardLinesResponse.Builder builder = ListGiftCardLinesResponse.newBuilder();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		MTable table = MTable.get(Env.getCtx(), "ECA14_GiftCardLine");
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@TableName@ ECA14_GiftCardLine @NotFound@");
		}
		String whereClause = "ECA14_GiftCard_ID = ? ";
		// TODO: validate other parameters for filter
		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			table.getTableName(),
			whereClause,
			null
		)
			.setParameters(request.getGiftCardId())
			.setClient_ID()
			.setOnlyActiveRecords(true)
		;
		int count = query.count();
		query
			.setLimit(limit, offset)
			.<PO>list()
			.forEach(line -> {
				GiftCardLine.Builder giftCardLinetBuilder = POSConvertUtil.convertGiftCardLine(line);
				builder.addGiftCardLines(giftCardLinetBuilder);
			});
		//
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;

	}
	public static GiftCardLine.Builder updateGiftCardLine (UpdateGiftCardLineRequest request) {
		Properties ctx = Env.getCtx();
		if (request.getGiftCardId() <= 0 ) {
			throw new AdempiereException("@FillMandatory@ @ECA14_GiftCard_ID@");
		}
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @ECA14_GiftCardLine_ID@");
		}
		//TODO: Validate POS Information
		int giftCardLineID = request.getId();

		MTable table = MTable.get(ctx, "ECA14_GiftCardLine");
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@TableName@ ECA14_GiftCardLine @NotFound@");
		}
		AtomicReference<PO> giftCardLineReference = new AtomicReference<>();

		Trx.run( transactionName -> {
			PO giftCardLine = new Query(
				ctx,
				table.getTableName(),
				" ECA14_GiftCardLine_ID = ? ",
				transactionName
			)
				.setParameters(giftCardLineID)
				.setClient_ID()
				.first()
			;
			if (giftCardLine == null || giftCardLine.get_ValueAsInt("ECA14_GiftCardLine_ID") <= 0) {
				throw new AdempiereException("@ECA14_GiftCardLine_ID@ @NotFound@");
			}
			//	Validate processed Order
			if (giftCardLine.get_ValueAsBoolean("Processed")) {
				throw new AdempiereException("@ECA14_GiftCardLine_ID@ @Processed@");
			}

			MOrderLine orderLine = new MOrderLine(ctx, giftCardLine.get_ValueAsInt("C_OrderLine_ID"), transactionName);
			BigDecimal availableQty = getAvailableQtyForGiftCardLine(
				orderLine.getC_OrderLine_ID(),
				orderLine.getQtyOrdered()
			);
			// availableQty already subtracted GiftCardLine Qty
			BigDecimal giftCardLineQty = Optional.ofNullable(
				(BigDecimal) giftCardLine.get_Value("QtyEntered")
			)
			.orElse(Env.ZERO);
			availableQty = availableQty.add(giftCardLineQty);

			BigDecimal newQtyEntered = Optional.ofNullable(
				NumberManager.getBigDecimalFromString(
					request.getQuantityEntered()
				)
			).orElse(Env.ZERO);
			if (newQtyEntered.signum() <= 0) {
				// TODO: Validate if there is a better message
				throw new AdempiereException("@FillMandatory@ @Qty@");
			}
			if (availableQty.compareTo(newQtyEntered) < 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}
			giftCardLine.set_ValueOfColumn("QtyEntered", newQtyEntered);
			BigDecimal newAmount = Optional.ofNullable(
				NumberManager.getBigDecimalFromString(
					request.getAmount()
				)
			).orElse(Env.ZERO);
			if (newAmount.compareTo(orderLine.getLineNetAmt()) > 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}
			giftCardLine.set_ValueOfColumn("Amount", newAmount);
			giftCardLine.saveEx();
			giftCardLineReference.set(giftCardLine);
		});
		//	Return
		return POSConvertUtil.convertGiftCardLine(giftCardLineReference.get());
	}
	public static Empty.Builder deleteGiftCardLine(DeleteGiftCardLineRequest request) {
		Properties context = Env.getCtx();
		int giftCardLineID = request.getId();
		if (giftCardLineID <= 0) {
			return Empty.newBuilder();
		}
		MTable table = MTable.get(context, "ECA14_GiftCardLine");
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@TableName@ ECA14_GiftCardLine @NotFound@");
		}
		//TODO: Validate POS Information
		PO giftCardLine = new Query(
			context,
			table.getTableName(),
			" ECA14_GiftCardLine_ID = ? ",
			null
		)
			.setParameters(giftCardLineID)
			.setClient_ID()
			.first()
		;
		if (giftCardLine == null || giftCardLine.get_ValueAsInt("ECA14_GiftCardLine_ID") == 0) {
			return Empty.newBuilder();
		}
		//	Validate processed Order
		if (giftCardLine.get_ValueAsBoolean("Processed") || giftCardLine.get_ValueAsBoolean("Processing")) {
			throw new AdempiereException("@ECA14_GiftCardLine_ID@ @Processed@");
		}

		giftCardLine.deleteEx(true);

		//	Return
		return Empty.newBuilder();
	}

	public static ListAvailableOrderLinesForGiftCardResponse.Builder listAvailableOrderLinesForGiftCard(ListAvailableOrderLinesForGiftCardRequest request) {
		if(request.getOrderId() <= 0) {
			throw new AdempiereException("@C_Order_ID@ @NotFound@");
		}
		ListAvailableOrderLinesForGiftCardResponse.Builder builder = ListAvailableOrderLinesForGiftCardResponse.newBuilder();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		String whereClause = "C_Order_ID = ? ";
		//	Get Product list
		Query query = new Query(
				Env.getCtx(),
				MOrderLine.Table_Name,
				whereClause,
				null
		)
				.setParameters(request.getOrderId())
				.setClient_ID()
				.setOnlyActiveRecords(true)
			;
		query
			.setLimit(limit, offset)
			.<MOrderLine>list()
			.forEach(line -> {
				BigDecimal availableQty = getAvailableQtyForGiftCardLine(line.getC_OrderLine_ID(), line.getQtyEntered());
				if (availableQty.signum() <= 0) {
					return;
				}
				AvailableOrderLine.Builder availableOrderLineBuilder = POSConvertUtil.convertAvailableOrderLine(line, availableQty);
				builder.addOrderLines(availableOrderLineBuilder);
			});
		//
		int count = builder.getOrderLinesCount();
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
				StringManager.getValidString(nexPageToken)
		);
		return builder;

	}

	public static ListAvailableOrderLinesForRMAResponse.Builder listAvailableOrderLinesForRMA(ListAvailableOrderLinesForRMARequest request) {
		if(request.getOrderId() <= 0) {
			throw new AdempiereException("@" + MOrder.COLUMNNAME_C_Order_ID + "@ @NotFound@");
		}
		ListAvailableOrderLinesForRMAResponse.Builder builder = ListAvailableOrderLinesForRMAResponse.newBuilder();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		String whereClause = "C_Order_ID = ? ";
		//	Get Product list
		Query query = new Query(
				Env.getCtx(),
				MOrderLine.Table_Name,
				whereClause,
				null
		)
				.setParameters(request.getOrderId())
				.setClient_ID()
				.setOnlyActiveRecords(true)
				;
		query
				.setLimit(limit, offset)
				.<MOrderLine>list()
				.forEach(line -> {
					BigDecimal returnedQty = RMAUtil.getReturnedQuantity(line.getC_OrderLine_ID());
					BigDecimal availableQty = Optional.ofNullable(line.getQtyEntered()).orElse(Env.ZERO).subtract(Optional.ofNullable(returnedQty).orElse(Env.ZERO));
					if (availableQty.signum() <= 0) {
						return;
					}
					AvailableOrderLine.Builder availableOrderLineBuilder = POSConvertUtil.convertAvailableOrderLine(line, availableQty);
					builder.addOrderLines(availableOrderLineBuilder);
				});
		//
		int count = builder.getOrderLinesCount();
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
				StringManager.getValidString(nexPageToken)
		);
		return builder;

	}



	public static ProcessOnlinePaymentResponse.Builder processOnlinePayment(ProcessOnlinePaymentRequest request) {
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Payment_ID@");
		}
		AtomicReference<ProcessOnlinePaymentResponse.Builder> builderReference = new AtomicReference<>();
		Trx.run( transactionName ->{
			ProcessOnlinePaymentResponse.Builder builder = ProcessOnlinePaymentResponse.newBuilder();
			MPayment payment = new MPayment(Env.getCtx(), request.getId(), transactionName);
			payment.processOnline();
			String message = payment.get_ValueAsString("ResponseMessage");
			String status = payment.get_ValueAsString("ResponseStatus");
			boolean isError = "E".equals(status) || "R".equals(status);
			builder
				.setIsError(isError)
				.setMessage(
					StringManager.getValidString(
						message
					)
				)
				.setStatus(
					StringManager.getValidString(
						status
					)
				)
				.setNextRequestTime(
					payment.get_ValueAsInt("NextRequestTime")
				)
			;
			builderReference.set(builder);

		});
		return builderReference.get();
	}


	public static InfoOnlinePaymentResponse.Builder infoOnlinePayment(InfoOnlinePaymentRequest request) {
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Payment_ID@");
		}
		AtomicReference<InfoOnlinePaymentResponse.Builder> builderReference = new AtomicReference<>();
		Trx.run(transactionName -> {
			InfoOnlinePaymentResponse.Builder builder = InfoOnlinePaymentResponse.newBuilder();
			MPayment payment = new MPayment(Env.getCtx(), request.getId(), transactionName);
			boolean isForcedStatus = payment.getOnlineStatus();
			String message = payment.get_ValueAsString("ResponseMessage");
			String status = payment.get_ValueAsString("ResponseStatus");
			boolean isError = "E".equals(status) || "R".equals(status);
			if(isForcedStatus) {
				isError = false;
				status = "R";
			}
			builder
				.setIsError(isError)
				.setMessage(
					StringManager.getValidString(
						message
					)
				)
				.setStatus(
					StringManager.getValidString(
						status
					)
				)
				.setNextRequestTime(
					payment.get_ValueAsInt("NextRequestTime")
				)
			;
			;
			builderReference.set(builder);
		});
		return builderReference.get();
	}


	public static CancelOnlinePaymentResponse.Builder cancelOnlinePayment(CancelOnlinePaymentRequest request) {
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Payment_ID@");
		}
		AtomicReference<CancelOnlinePaymentResponse.Builder> builderReference = new AtomicReference<>();
		Trx.run(transactionName -> {
			MPayment payment = new MPayment(Env.getCtx(), request.getId(), null);
			CancelOnlinePaymentResponse.Builder builder = CancelOnlinePaymentResponse.newBuilder();
			boolean wasReversed = payment.reverseOnlineTransaction();
			String message = payment.get_ValueAsString("ResponseMessage");
			String status = payment.get_ValueAsString("ResponseStatus");
			boolean isError = "E".equals(status) || "R".equals(status);
			if(wasReversed) {
				isError = false;
			}
			builder
				.setIsError(isError)
				.setMessage(
					StringManager.getValidString(
						message
					)
				)
				.setStatus(
					StringManager.getValidString(
						status
					)
				)
			;
			builderReference.set(builder);
		});

		return builderReference.get();
	}



}
