/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2026 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 *****************************************************************************/
package com.solop.sp013.core.support;

import com.solop.sp013.core.documents.IFiscalDocument;
import org.spin.util.support.IAppSupport;

/**
 * Interface for providers that can cancel/void an already issued fiscal document at the tax
 * authority (e.g. NubeFact: comunicación/resumen de baja via generar_anulacion). The electronic
 * invoicing ModelValidator calls {@link #cancelFiscalDocument(IFiscalDocument, String)} when an
 * electronic document that was already sent is voided in the ERP. Providers that cancel only through
 * credit notes (e.g. Invoicy) simply do not implement this interface.
 *
 * @author Gabriel Escalona
 */
public interface IFiscalDocumentCanceller extends IAppSupport {

	/**
	 * Cancel (void / comunicación de baja) an already sent fiscal document at the tax authority.
	 * @param fiscalDocument the document to cancel
	 * @param reason the cancellation reason (motivo)
	 * @return the provider response
	 */
	IFiscalSenderResponse cancelFiscalDocument(IFiscalDocument fiscalDocument, String reason);

}
