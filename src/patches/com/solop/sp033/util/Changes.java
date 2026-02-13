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
package com.solop.sp033.util;

/**
 * Add here all changes for core and static methods
 * Please rename this class and package
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">...</a>
 */
public interface Changes {
	String ENTITY_TYPE = "SP033";
	String SP033_PayloadURL = "SP033_PayloadURL";
	String SP033_ContentType = "SP033_ContentType";
	String SP033_ContentType_AJ = "AJ";
	String SP033_ContentType_AX = "AX";
	String SP033_Method = "SP033_Method";
	String SP033_Webhook = "SP033_Webhook";
	String SP033_WebhookTable = "SP033_WebhookTable";
	/**	Persistence Events	*/
	String SP033_Insert = "SP033_Insert";
	String SP033_Update = "SP033_Update";
	String SP033_Delete = "SP033_Delete";
	/**	Document Events	*/
	String SP033_Prepare = "SP033_Prepare";
	String SP033_Complete = "SP033_Complete";
	String SP033_Reactivate = "SP033_Reactivate";
	String SP033_Close = "SP033_Close";
	String SP033_Void = "SP033_Void";
	String SP033_Reverse = "SP033_Reverse";
	String SP033_Post = "SP033_Post";
	String SP033_IsTest = "SP033_IsTest";
	String SP033_TestPayloadURL = "SP033_TestPayloadURL";
	String TYPE_AFTER_NEW = "TYPE_AFTER_NEW";
	String TYPE_AFTER_CHANGE = "TYPE_AFTER_CHANGE";
	String TYPE_AFTER_DELETE = "TYPE_AFTER_DELETE";
	String TIMING_AFTER_PREPARE = "TIMING_AFTER_PREPARE";
	String TIMING_AFTER_COMPLETE = "TIMING_AFTER_COMPLETE";
	String TIMING_AFTER_VOID = "TIMING_AFTER_VOID";
	String TIMING_AFTER_CLOSE = "TIMING_AFTER_CLOSE";
	String TIMING_AFTER_REACTIVATE = "TIMING_AFTER_REACTIVATE";
	String TIMING_AFTER_REVERSECORRECT = "TIMING_AFTER_REVERSECORRECT";
	String TIMING_AFTER_REVERSEACCRUAL = "TIMING_AFTER_REVERSEACCRUAL";
	String TIMING_AFTER_POST = "TIMING_AFTER_POST";
}
