/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/

package com.solop.sp034.process;

import com.solop.sp034.util.Changes;
import org.compiere.model.*;
import org.compiere.util.Trx;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Generated Process for (Publishing Processing)
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class PublishingProcessing extends PublishingProcessingAbstract {
	private final AtomicInteger publications = new AtomicInteger();
	private final AtomicInteger errors = new AtomicInteger();
	private MTable publishingTable;
	@Override
	protected String doIt() throws Exception {
		publishingTable = MTable.get(getCtx(), Changes.Table_SP034_Publishing);
		getValidKeys().parallelStream().forEach(publishingId -> {
			try {
				Trx.run(transactionName -> {
					processPublishing(publishingId, transactionName);
				});
			} catch (Exception e) {
				log.warning(e.getLocalizedMessage());
				errors.incrementAndGet();
			}
		});
		return "@Processed@: " + publications + " @Errors@: " + errors;
	}

	private List<Integer> getValidKeys() {
		if (getSelectionKeys() == null || getSelectionKeys().isEmpty()) {
			if (getRecord_ID() > 0) {
				return List.of(getRecord_ID());
			}
			log.warning("No publishing selected");
			return List.of();
		}
		return getSelectionKeys();
	}

	private void processPublishing(int publishingId, String transactionName) {
		PO publishing = publishingTable.getPO(publishingId, transactionName);
		publishing.set_ValueOfColumn(Changes.SP034_PublishStatus, getPublishStatus());
		publishing.saveEx();
		publications.incrementAndGet();
	}
}
