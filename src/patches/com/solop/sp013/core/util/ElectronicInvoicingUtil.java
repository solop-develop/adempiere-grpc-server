/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2019 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package com.solop.sp013.core.util;

import org.adempiere.core.domains.models.I_C_TaxGroup;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Util;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Add here all changes for core and static methods
 * Please rename this class and package
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class ElectronicInvoicingUtil {
	public static File getPdfFileFromUrl(String downloadUrl) {
		try {
			String invoicyEndpoint = null;
			Pattern regexD = Pattern.compile("(https?:\\/\\/)?(?:www\\.)?(:[0-9]+)?(.*)([^:\\/\\n]+)\\/.*\\/", Pattern.MULTILINE);
			Matcher regexMatcherD = regexD.matcher(downloadUrl);
			if (regexMatcherD.find()) {
				invoicyEndpoint = regexMatcherD.group();
			}
			String firstHtmlPage = getFirstHtml(downloadUrl);
			String validUrl = null;
			try {
				Document doc = Jsoup.parse(firstHtmlPage);
				Element iframeElement = doc.select("iframe[name=EMBPAGE1]").first();
				if (iframeElement != null) {
					validUrl = invoicyEndpoint +iframeElement.attr("src");
				}
			} catch (Exception e) {
				throw new AdempiereException(e.getMessage());
			}
			File validFile;
			InputStream inRepImpCFE = null;
			OutputStream fileOut = null;
			try {
				validFile = File.createTempFile("invoicy_", ".pdf");
				validFile.deleteOnExit();
				URLConnection connRepImpCFE = new URL(validUrl).openConnection();
				inRepImpCFE = connRepImpCFE.getInputStream();
				fileOut = new FileOutputStream(validFile);
				int b = 0;
				while (b != -1) {
					b = inRepImpCFE.read();
					if (b != -1) {
						fileOut.write(b);
					}
				}
			} catch (Exception e) {
				throw new AdempiereException(e.getMessage());
			} finally {
				if(fileOut != null) {
					fileOut.close();
				}
				if(inRepImpCFE != null) {
					inRepImpCFE.close();
				}
			}
			return validFile;
		} catch (Exception e) {
			throw new AdempiereException(e);
		}
	}

	private static String getFirstHtml(String downloadUrl) {
		URL url;
		InputStream is = null;
		BufferedReader br;
		String line;
		String firstHtmlPage = "";
		try {
			url = new URL(downloadUrl);
			is = url.openStream();
			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				firstHtmlPage += line;
			}
		} catch (Exception e) {
			throw new AdempiereException(e.getMessage());
		} finally {
			try {
				if (is != null){
					is.close();
				}
			} catch (Exception e) {
				throw new AdempiereException(e);
			}
		}
		return firstHtmlPage;
	}

	public static String getTaxIndicatorFromTax(Properties context, int taxTypeId) {
		if(taxTypeId <= 0) {
			throw new AdempiereException("@" + ElectronicInvoicingChanges.SP013_TaxType_ID + "@ @NotFound@");
		}
		PO taxType = new Query(context, ElectronicInvoicingChanges.SP013_TaxType, ElectronicInvoicingChanges.SP013_TaxType_ID + " = ?", null).setParameters(taxTypeId).first();
		if(taxType != null) {
			return taxType.get_ValueAsString("Value");
		}
		return null;
	}

	public static int getDocumentTypeFromTaxGroup(Properties context, String fiscalDocumentType, int taxGroupId, boolean isManual) {
		if(taxGroupId <= 0) {
			throw new AdempiereException("@" + ElectronicInvoicingChanges.SP013_TaxType_ID + "@ @NotFound@");
		}
		PO taxGroup = new Query(context, I_C_TaxGroup.Table_Name, "C_TaxGroup_ID = ?", null).setParameters(taxGroupId).first();
		if(taxGroup != null) {
			if (Util.isEmpty(fiscalDocumentType, true)){
				fiscalDocumentType = ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice;
			}
			if (isManual) {
				if(fiscalDocumentType.equals(ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice)) {
					return taxGroup.get_ValueAsInt(ElectronicInvoicingChanges.SP013_MInvoiceDocType_ID);
				} else if(fiscalDocumentType.equals(ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note)) {
					return taxGroup.get_ValueAsInt(ElectronicInvoicingChanges.SP013_MCreditDocType_ID);
				} else if(fiscalDocumentType.equals(ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note)) {
					return taxGroup.get_ValueAsInt(ElectronicInvoicingChanges.SP013_MDebitDocType_ID);
				} else {
					return taxGroup.get_ValueAsInt(ElectronicInvoicingChanges.SP013_MInvoiceDocType_ID);
				}
			} else {
				if(fiscalDocumentType.equals(ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice)) {
					return taxGroup.get_ValueAsInt(ElectronicInvoicingChanges.SP013_InvoiceDocType_ID);
				} else if(fiscalDocumentType.equals(ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note)) {
					return taxGroup.get_ValueAsInt(ElectronicInvoicingChanges.SP013_CreditDocType_ID);
				} else if(fiscalDocumentType.equals(ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note)) {
					return taxGroup.get_ValueAsInt(ElectronicInvoicingChanges.SP013_DebitDocType_ID);
				} else {
					return taxGroup.get_ValueAsInt(ElectronicInvoicingChanges.SP013_InvoiceDocType_ID);
				}
			}

		}
		return -1;
	}

	public static XMLGregorianCalendar Timestamp_to_XmlGregorianCalendar_OnlyDate(Timestamp timestamp) {
		try {
			GregorianCalendar gregorianCalendar = (GregorianCalendar) GregorianCalendar.getInstance();
			gregorianCalendar.setTime(timestamp);
			XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance()
					.newXMLGregorianCalendarDate(gregorianCalendar.get(Calendar.YEAR), gregorianCalendar.get(Calendar.MONTH)+1,
							gregorianCalendar.get(Calendar.DAY_OF_MONTH), DatatypeConstants.FIELD_UNDEFINED);

			return xmlGregorianCalendar;
		} catch (DatatypeConfigurationException e) {
			throw new AdempiereException(e);
		}
	}
}
