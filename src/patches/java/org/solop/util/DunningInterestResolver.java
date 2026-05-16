/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 *****************************************************************************/

package org.solop.util;

import org.adempiere.core.domains.models.I_C_DunningInterestRate;
import org.adempiere.core.domains.models.I_C_DunningInterestVersion;
import org.adempiere.core.domains.models.X_C_DunningInterestRate;
import org.adempiere.core.domains.models.X_C_DunningInterestType;
import org.adempiere.core.domains.models.X_C_DunningInterestVersion;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MInvoice;
import org.compiere.model.Query;
import org.compiere.util.Msg;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Properties;

/**
 * Lightweight resolver for the {@code C_DunningInterestType /
 * C_DunningInterestVersion / C_DunningInterestRate} model.
 * <p>
 * Built with the {@link Builder} pattern. After {@code build()} the
 * resolver exposes the type and version applicable for a given invoice
 * and date; rates are fetched on demand via
 * {@link #getRate(int, int)} because they depend on the invoice
 * currency and overdue days.
 * <p>
 * Callers that already resolved the type and/or version (e.g. through
 * their own cache) can pass them via {@link Builder#setType} /
 * {@link Builder#setVersion} to skip the corresponding DB lookups.
 */
public class DunningInterestResolver {

	private static final BigDecimal DAYS_PER_MONTH = new BigDecimal(30);

	private final Properties ctx;
	private final String trxName;
	private final boolean strict;
	private X_C_DunningInterestType type;
	private X_C_DunningInterestVersion version;
	private boolean applicable;

	private DunningInterestResolver(Builder b) {
		this.ctx = b.ctx;
		this.trxName = b.trxName;
		this.strict = b.strict;
		this.type = b.type;
		this.version = b.version;
		Timestamp dateDoc = b.dateDoc != null ? b.dateDoc : new Timestamp(System.currentTimeMillis());
		resolve(b.invoice, b.overrideTypeId, dateDoc);
	}

	public static Builder newBuilder(Properties ctx, String trxName) {
		return new Builder(ctx, trxName);
	}

	public boolean isApplicable() { return applicable; }
	public X_C_DunningInterestType getType() { return type; }
	public X_C_DunningInterestVersion getVersion() { return version; }

	/** Returns the applicable rate row for the given currency and overdue days,
	 *  or {@code null} if none applies (or throws in strict mode). Rates with
	 *  no currency assigned (NULL or 0) act as fallback for any currency. */
	public X_C_DunningInterestRate getRate(int currencyId, int daysDue) {
		if (!applicable)
			return null;
		X_C_DunningInterestRate rate = new Query(ctx, I_C_DunningInterestRate.Table_Name,
				"C_DunningInterestVersion_ID=?"
				+ " AND (C_Currency_ID=? OR C_Currency_ID IS NULL OR C_Currency_ID=0)"
				+ " AND DaysFrom<=? AND (DaysTo IS NULL OR DaysTo=0 OR DaysTo>=?)",
				trxName)
				.setParameters(version.getC_DunningInterestVersion_ID(), currencyId, daysDue, daysDue)
				.setOnlyActiveRecords(true)
				.setOrderBy("CASE WHEN C_Currency_ID=" + currencyId + " THEN 0 ELSE 1 END, DaysFrom DESC")
				.first();
		if (rate == null && strict)
			throw new AdempiereException(Msg.parseTranslation(ctx,
					"@NotFound@ @C_DunningInterestRate_ID@ @C_Currency_ID@=" + currencyId
					+ " @DaysDue@=" + daysDue));
		return rate;
	}

	/** {@code (daysDue / 30) * rate}, scale 2 HALF_UP. */
	public static BigDecimal computeFinalRate(BigDecimal daysDue, BigDecimal rate) {
		return daysDue.divide(DAYS_PER_MONTH, 2, RoundingMode.HALF_UP).multiply(rate);
	}

	/** {@code base * finalRate / 100}, rounded HALF_UP at the currency's
	 *  standard precision. Callers should pass
	 *  {@code MCurrency.getStdPrecision(ctx, currencyId)}. */
	public static BigDecimal computeDunningAmount(BigDecimal base, BigDecimal finalRate, int currencyPrecision) {
		return base.multiply(finalRate).divide(new BigDecimal(100), currencyPrecision, RoundingMode.HALF_UP);
	}

	private void resolve(MInvoice invoice, int overrideTypeId, Timestamp dateDoc) {
		if (invoice == null)
			throw new AdempiereException("DunningInterestResolver: invoice is required");

		if (type == null) {
			MBPartner bPartner = (MBPartner) invoice.getC_BPartner();
			int typeId = overrideTypeId > 0
					? overrideTypeId
					: bPartner.get_ValueAsInt("C_DunningInterestType_ID");

			if (typeId <= 0) {
				if (strict)
					throw new AdempiereException(Msg.parseTranslation(ctx,
							"@NotFound@ @C_DunningInterestType_ID@ @C_BPartner_ID@: "
							+ bPartner.getValue() + "_" + bPartner.getName()));
				return;
			}

			type = new X_C_DunningInterestType(ctx, typeId, trxName);
			if (type.getC_DunningInterestType_ID() <= 0) {
				if (strict)
					throw new AdempiereException(Msg.parseTranslation(ctx,
							"@NotFound@ @C_DunningInterestType_ID@=" + typeId));
				type = null;
				return;
			}
		}

		if (type.getC_Charge_ID() <= 0) {
			if (strict)
				throw new AdempiereException(Msg.parseTranslation(ctx,
						"@NotFound@ @C_Charge_ID@ @C_DunningInterestType_ID@: " + type.getName()));
			return;
		}

		if (version == null) {
			version = new Query(ctx, I_C_DunningInterestVersion.Table_Name,
					"C_DunningInterestType_ID=? AND ValidFrom<=? AND (ValidTo IS NULL OR ValidTo>=?)",
					trxName)
					.setParameters(type.getC_DunningInterestType_ID(), dateDoc, dateDoc)
					.setOnlyActiveRecords(true)
					.first();
			if (version == null) {
				if (strict)
					throw new AdempiereException(Msg.parseTranslation(ctx,
							"@NotFound@ @C_DunningInterestVersion_ID@ @C_DunningInterestType_ID@: "
							+ type.getName()));
				return;
			}
		}

		applicable = true;
	}

	public static class Builder {
		private final Properties ctx;
		private final String trxName;
		private MInvoice invoice;
		private Timestamp dateDoc;
		private int overrideTypeId = 0;
		private boolean strict = true;
		private X_C_DunningInterestType type;
		private X_C_DunningInterestVersion version;

		private Builder(Properties ctx, String trxName) {
			this.ctx = ctx;
			this.trxName = trxName;
		}

		public Builder setInvoice(MInvoice invoice) { this.invoice = invoice; return this; }
		public Builder setDateDoc(Timestamp dateDoc) { this.dateDoc = dateDoc; return this; }
		public Builder setOverrideTypeId(int typeId) { this.overrideTypeId = typeId; return this; }
		public Builder setStrict(boolean strict) { this.strict = strict; return this; }
		/** Skip type lookup. */
		public Builder setType(X_C_DunningInterestType type) { this.type = type; return this; }
		/** Skip version lookup. */
		public Builder setVersion(X_C_DunningInterestVersion version) { this.version = version; return this; }

		public DunningInterestResolver build() {
			return new DunningInterestResolver(this);
		}
	}
}
