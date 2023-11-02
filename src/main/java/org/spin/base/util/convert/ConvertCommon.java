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
package org.spin.base.util.convert;

import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.util.Env;
import org.spin.backend.grpc.common.BankAccount;
import org.spin.backend.grpc.common.BusinessPartner;
import org.spin.backend.grpc.common.Currency;
import org.spin.backend.grpc.common.DocumentType;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * This class was created for add all convert methods for Common proto definition
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class ConvertCommon {
	
	/**
	 * Convert Currency
	 * @param currency
	 * @return
	 */
	public static Currency.Builder convertCurrency(int currencyId) {
		Currency.Builder builder = Currency.newBuilder();
		if(currencyId <= 0) {
			return builder;
		}
		//	Set values
		MCurrency currency = MCurrency.get(Env.getCtx(), currencyId);
		return convertCurrency(
			currency
		);
	}
	/**
	 * Convert Currency
	 * @param currency
	 * @return
	 */
	public static Currency.Builder convertCurrency(MCurrency currency) {
		Currency.Builder builder = Currency.newBuilder();
		if(currency == null) {
			return builder;
		}
		//	Set values
		return builder.setId(currency.getC_Currency_ID())
			.setIsoCode(ValueManager.validateNull(currency.getISO_Code()))
			.setCurSymbol(ValueManager.validateNull(currency.getCurSymbol()))
			.setDescription(ValueManager.validateNull(currency.getDescription()))
			.setStandardPrecision(currency.getStdPrecision())
			.setCostingPrecision(currency.getCostingPrecision()
		);
	}


	/**
	 * Convert Document Type
	 * @param documentTypeId
	 * @return
	 */
	public static DocumentType.Builder convertDocumentType(int documentTypeId) {
		DocumentType.Builder builder = DocumentType.newBuilder();
		if (documentTypeId <= 0) {
			return builder;
		}
		MDocType documenType = MDocType.get(Env.getCtx(), documentTypeId);
		return convertDocumentType(documenType);
	}
	/**
	 * Convert Document Type
	 * @param documentType
	 * @return
	 */
	public static DocumentType.Builder convertDocumentType(MDocType documentType) {
		if (documentType == null) {
			return DocumentType.newBuilder();
		}

		return DocumentType.newBuilder()
			.setId(documentType.getC_DocType_ID())
			.setName(ValueManager.validateNull(documentType.getName()))
			.setDescription(ValueManager.validateNull(documentType.getDescription()))
			.setPrintName(ValueManager.validateNull(documentType.getPrintName())
		);
	}


	/**
	 * Convert Bank Account to gRPC stub class
	 * @param bankAccountId
	 * @return
	 */
	public static BankAccount.Builder convertBankAccount(int bankAccountId) {
		if(bankAccountId <= 0) {
			return BankAccount.newBuilder();
		}
		MBankAccount bankAccount = MBankAccount.get(Env.getCtx(), bankAccountId);
		return convertBankAccount(bankAccount);
	}
	/**
	 * Convert Bank Account to gRPC stub class
	 * @param bankAccount
	 * @return
	 */
	public static BankAccount.Builder convertBankAccount(MBankAccount bankAccount) {
		BankAccount.Builder builder = BankAccount.newBuilder();
		if(bankAccount == null) {
			return builder;
		}
		//	
		return builder.setId(bankAccount.getC_BankAccount_ID())
			.setAccountNo(ValueManager.validateNull(bankAccount.getAccountNo()))
			.setName(ValueManager.validateNull(bankAccount.getName()))
			.setDescription(ValueManager.validateNull(bankAccount.getDescription()))
			.setIsDefault(bankAccount.isDefault())
			.setBban(ValueManager.validateNull(bankAccount.getBBAN()))
			.setIban(ValueManager.validateNull(bankAccount.getIBAN()))
			.setBankAccountType(
				bankAccount.getBankAccountType().equals(MBankAccount.BANKACCOUNTTYPE_Checking) ?
					BankAccount.BankAccountType.CHECKING : BankAccount.BankAccountType.SAVINGS
			)
			.setCreditLimit(ValueManager.getValueFromBigDecimal(bankAccount.getCreditLimit()))
			.setCurrentBalance(ValueManager.getValueFromBigDecimal(bankAccount.getCurrentBalance()))
			//	Foreign
			.setCurrency(
				convertCurrency(
					bankAccount.getC_Currency_ID())
				)
			.setBusinessPartner(
				convertBusinessPartner(
					bankAccount.getC_BPartner_ID()
				)
			)
			.setBankId(
				bankAccount.getC_Bank_ID()
			)
		;
	}


	/**
	 * Convert business partner
	 * @param businessPartnerId
	 * @return
	 */
	public static BusinessPartner.Builder convertBusinessPartner(int businessPartnerId) {
		if(businessPartnerId <= 0) {
			return BusinessPartner.newBuilder();
		}
		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		return convertBusinessPartner(
			businessPartner
		);
	}
	/**
	 * Convert business partner
	 * @param businessPartner
	 * @return
	 */
	public static BusinessPartner.Builder convertBusinessPartner(MBPartner businessPartner) {
		if(businessPartner == null) {
			return BusinessPartner.newBuilder();
		}
		return BusinessPartner.newBuilder()
			.setId(businessPartner.getC_BPartner_ID())
			.setValue(ValueManager.validateNull(businessPartner.getValue()))
			.setTaxId(ValueManager.validateNull(businessPartner.getTaxID()))
			.setDuns(ValueManager.validateNull(businessPartner.getDUNS()))
			.setNaics(ValueManager.validateNull(businessPartner.getNAICS()))
			.setName(ValueManager.validateNull(businessPartner.getName()))
			.setLastName(ValueManager.validateNull(businessPartner.getName2()))
			.setDescription(ValueManager.validateNull(businessPartner.getDescription())
		);
	}

}
