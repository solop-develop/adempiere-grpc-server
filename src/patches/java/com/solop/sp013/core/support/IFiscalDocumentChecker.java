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
 * Interface for providers whose fiscal documents are validated asynchronously: the document is
 * first sent ({@link IFiscalSender#sendFiscalDocument}) and later its final state is retrieved
 * from the provider/tax authority. The electronic document queue calls
 * {@link #checkFiscalDocument(IFiscalDocument)} on the following passes until the document is
 * accepted (e.g. NubeFact GRE: generar_guia then consultar_guia).
 *
 * @author Gabriel Escalona
 */
public interface IFiscalDocumentChecker extends IAppSupport {

	/**
	 * Query the current state of an already sent fiscal document.
	 * @param fiscalDocument the document to check
	 * @return the provider response; when accepted it carries the download url, otherwise it is
	 * still pending and the queue will check again.
	 */
	IFiscalSenderResponse checkFiscalDocument(IFiscalDocument fiscalDocument);

}
