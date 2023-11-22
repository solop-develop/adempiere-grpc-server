/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.grpc.service.form.payroll_action_notice;

import org.adempiere.core.domains.models.X_HR_Employee;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.eevolution.hr.model.MHREmployee;
import org.eevolution.hr.model.MHRProcess;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Util for backend of `Action Payroll Notice` form
 */
public class PayrollActionNoticeUtil {

	public static MBPartner validateAndGetBusinessPartner(int businessPartnerId) {
		if (businessPartnerId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_BPartner_ID@");
		}
		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		if (businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			throw new AdempiereException("@C_BPartner_ID@ @NotFound@");
		}
		if (!businessPartner.isActive()) {
			throw new AdempiereException("@C_BPartner_ID@ @NotActive@");
		}
		return businessPartner;
	}


	public static MHREmployee validateAndGetEmployee(int employeeId) {
		if (employeeId <= 0) {
			throw new AdempiereException("@FillMandatory@ @HR_Employee_ID@");
		}
		MHREmployee employee = MHREmployee.getById(Env.getCtx(), employeeId);
		if (employee == null || employee.getHR_Employee_ID() <= 0) {
			throw new AdempiereException("@HR_Employee_ID@ @NotFound@");
		}
		if (!employee.isActive()
			|| (!Util.isEmpty(employee.getEmployeeStatus(), true)
			&& !employee.getEmployeeStatus().equals(X_HR_Employee.EMPLOYEESTATUS_Active))) {
			throw new AdempiereException("@HR_Employee_ID@ @NotActive@");
		}
		return employee;
	}


	public static MHRProcess validateAndGetPayrollProcess(int payrollProcessId) {
		if (payrollProcessId <= 0) {
			throw new AdempiereException("@FillMandatory@ @HR_Process_ID@");
		}
		MHRProcess payrollProcess = new MHRProcess(Env.getCtx(), payrollProcessId, null);
		if (payrollProcess == null || payrollProcess.getHR_Process_ID() <= 0) {
			throw new AdempiereException("@HR_Process_ID@ @NotFound@");
		}
		if (!payrollProcess.isActive()) {
			throw new AdempiereException("@HR_Process_ID@ @NotActive@");
		}
		return payrollProcess;
	}

}
