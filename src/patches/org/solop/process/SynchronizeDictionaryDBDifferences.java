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

package org.solop.process;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.util.DB;
import org.eevolution.services.dsl.ProcessBuilder;

/** Generated Process for (Synchronize Dictionary DataBase Differences)
 *  @author Gabriel Escalona
 *  @version Release 3.9.4
 */
public class SynchronizeDictionaryDBDifferences extends SynchronizeDictionaryDBDifferencesAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		//For missing Tables, Sync first column from each table
		String sql = "SELECT DB_Element_ID FROM DictionaryInconsistency " +
			"WHERE DB_ElementType = 'TA'";
		DB.runResultSet(get_TrxName(),sql, null, resultSet ->{
			String whereClause = "AD_Table_ID = ?";
			int columnId = new Query(getCtx(), MColumn.Table_Name, whereClause, get_TrxName())
				.setParameters(resultSet.getInt(0))
				.setOnlyActiveRecords(true)
				.firstId();
			ProcessInfo processInfo = ProcessBuilder.create(getCtx())
				.process(org.compiere.process.ColumnSync.class)
				.withTitle("Sync Database")
				.withRecordId(MColumn.Table_ID , columnId)
				.execute(get_TrxName());

		}).onFailure( throwable -> {throw new AdempiereException(throwable);});

		//Mark every existing Column with Differences as RequiresSync
		sql = "UPDATE AD_Column c SET RequiresSync = 'Y' " +
				"WHERE EXISTS (SELECT 1 FROM DictionaryInconsistency di " +
				"WHERE di.DB_ElementType = 'CO' AND di.DB_Element_ID = c.AD_Column_ID AND di.DB_InconsistencyType <> 'NE'" +
				")";
		DB.executeUpdate(sql, get_TrxName());

		//Sync every remaining column with differences (Exisiting and Non-Existing)
		sql = "SELECT DB_Element_ID FROM DictionaryInconsistency " +
				"WHERE DB_ElementType = 'CO'";
		DB.runResultSet(get_TrxName(), sql, null, resultSet -> {
			ProcessInfo processInfo = ProcessBuilder.create(getCtx())
				.process(org.compiere.process.ColumnSync.class)
				.withTitle("Sync Database")
				.withRecordId(MColumn.Table_ID , resultSet.getInt(0))
				.execute(get_TrxName());
		}).onFailure(throwable -> {throw new AdempiereException(throwable);});
		return "";
	}
}