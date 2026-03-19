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
package org.spin.eca59.engine.parallel;

import org.adempiere.core.domains.models.I_HR_Movement;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCurrency;
import org.compiere.model.MRule;
import org.compiere.model.Query;
import org.compiere.model.Scriptlet;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.eevolution.hr.model.MHRAttribute;
import org.eevolution.hr.model.MHRConcept;
import org.eevolution.hr.model.MHRContract;
import org.eevolution.hr.model.MHREmployee;
import org.eevolution.hr.model.MHRMovement;
import org.eevolution.hr.model.MHRPayroll;
import org.eevolution.hr.model.MHRPayrollConcept;
import org.eevolution.hr.model.MHRPeriod;
import org.eevolution.hr.model.MHRProcess;
import org.eevolution.hr.services.HRProcessActionMsg;
import org.spin.eca59.concept.PayrollConcept;
import org.spin.eca59.employee.PayrollEmployee;
import org.spin.eca59.engine.PayrollEngine;
import org.spin.eca59.payroll_process.PayrollProcess;
import org.spin.eca59.rule.DefaultRuleResult;
import org.spin.eca59.rule.RuleContext;
import org.spin.eca59.rule.RuleResult;
import org.spin.eca59.rule.RuleRunner;
import org.spin.eca59.rule.RuleRunnerFactory;
import org.spin.hr.util.PayrollCalculationEngine;
import org.spin.hr.util.PayrollEngineHandler;
import org.spin.hr.util.RuleInterface;
import org.spin.util.RuleEngineUtil;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Default payroll process implementation
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class ParallelEngine implements PayrollEngine {

	/**	Static Logger	*/
	private static CLogger logger = CLogger.getCLogger (ParallelEngine.class);
	/** HR_Concept_ID->MHRMovement */
	public Map<String, MHRMovement> movements = Collections.synchronizedMap(new HashMap<String, MHRMovement>());
	/* stack of concepts executing rules - to check loop in recursion */
	private Map<String, Boolean> activeConceptRule = Collections.synchronizedMap(new HashMap<String, Boolean>());
	/**	Used for break employee running	*/
	public Map<Integer, Boolean> breakEmployee = Collections.synchronizedMap(new HashMap<Integer, Boolean>());
	/**	Used for break concept running	*/
	public Map<String, Boolean> breakConcept = Collections.synchronizedMap(new HashMap<String, Boolean>());
	private MHRProcess process;
	
	@Override
	public boolean validate() {
		return true;
	}

	@Override
	public MHRProcess getProcess() {
		return process;
	}

	public ParallelEngine(MHRProcess process) {
		this.process = process;
	}
	private String getMovementKey(int partnerId, int conceptId) {
		return partnerId + "|" + conceptId;
	}
	
	@Override
	public boolean run() {
		deleteMovements();
		logger.info("createParallelMovements #");
		long startTime = System.currentTimeMillis();
		List<Integer> payrollConcepts = getPayrollConceptIds();
		List<Integer> employees = getEmployeeIds();
		loadMovements();
		PayrollProcess payrollProcess = PayrollProcess.newInstance(getProcess());
		employees.parallelStream().forEach(businessPartnerId -> {
			MBPartner businessPartner = new MBPartner(getCtx(), businessPartnerId, null);
			logger.info("Employee # " + businessPartner.getValue() + " - " + businessPartner.getName() +  " " + businessPartner.getName2());
			long employeeStartTime = System.currentTimeMillis();
			PayrollEmployee payrollEmployee = PayrollEmployee.newInstance(MHREmployee.getActiveEmployee(getProcess().getCtx(), businessPartnerId, null));
			Trx.run(transactionName -> {
				for(int payrollConceptId : payrollConcepts) {
					MHRPayrollConcept payrollConceptReference = new MHRPayrollConcept(getCtx(), payrollConceptId, null);
					PayrollConcept payrollConcept = PayrollConcept.newInstance(payrollConceptReference);
					createMovementFromConcept(payrollProcess, payrollEmployee, payrollConcept, transactionName);
					if(breakEmployee.containsKey(payrollEmployee.getBusinessPartnerId())) {
						breakEmployee.remove(payrollEmployee.getBusinessPartnerId());
						logger.info("Skip Employee # " + businessPartner.getValue() + " - " + businessPartner.getName() +  " " + businessPartner.getName2() + " Time elapsed: " + TimeUtil.formatElapsed(System.currentTimeMillis() - employeeStartTime));
						break;
					}
				}
			});
			logger.info("Employee # " + businessPartner.getValue() + " - " + businessPartner.getName() +  " " + businessPartner.getName2() + " Time elapsed: " + TimeUtil.formatElapsed(System.currentTimeMillis() - employeeStartTime));
		});
		logger.info("Calculation for createParallelMovements # Time elapsed: " + TimeUtil.formatElapsed(System.currentTimeMillis() - startTime));
		return true;
	}
	
	/**
	 * Load HR_Movements and store them in a HR_Concept_ID->MHRMovement table
	 * @param partnerId
	 */
	private void loadMovements() {
		List<Integer> movementsList = new Query(getCtx(), I_HR_Movement.Table_Name, I_HR_Movement.COLUMNNAME_HR_Process_ID + " = ?", null)
				.setParameters(getProcess().getHR_Process_ID())
				.getIDsAsList();
		movementsList.parallelStream().forEach(movementId -> {
			MHRMovement movement = new MHRMovement(getCtx(), movementId, null);
			if(movements.containsKey(getMovementKey(movement.getC_BPartner_ID(), movement.getHR_Concept_ID()))) {
				MHRMovement lastM = movements.get(getMovementKey(movement.getC_BPartner_ID(), movement.getHR_Concept_ID()));
				MHRConcept concept = MHRConcept.getById(getCtx(), lastM.getHR_Concept_ID(), null);
				String columntype = concept.getColumnType();
				if (columntype.equals(MHRConcept.COLUMNTYPE_Amount)) {
					movement.addAmount(lastM.getAmount());
				} else if (columntype.equals(MHRConcept.COLUMNTYPE_Quantity)) {
					movement.addQty(lastM.getQty());
				}
			}
			movements.put(getMovementKey(movement.getC_BPartner_ID(), movement.getHR_Concept_ID()), movement);
		});
	}
	
	public Properties getCtx() {
		return getProcess().getCtx();
	}
	
	/**
	 * Method use to create a movemeningBuffer whereClause = new StringBuffer();
		StringBuffer orderByClause = new StringBuffer(MHRAttribute.COLUMNNAME_ValidFrom).append(ORDERVALUE);
		//	Add for updated
		orderByClause.append(", " + MHRAttribute.COLUMNNAME_Updated).append(ORDERVALUE);t
	 * @param concept
	 * @param isPrinted
	 * @return
	 */
	public MHRMovement createMovementFromConcept(PayrollProcess payrollProcess, PayrollEmployee employee, PayrollConcept payrollConcept, String transactionName) {
		MHRMovement movement = movements.get(getMovementKey(employee.getBusinessPartnerId(), payrollConcept.getId()));
		if(movement != null) {
			return movement;
		}
		logger.info("Calculating -> Concept "+ payrollConcept.getValue() + " -> " + payrollConcept.getName());
		long startTime = System.currentTimeMillis();
		MHRAttribute attribute = MHRAttribute.getByConceptAndEmployee(payrollConcept.getConcept() , employee.getEmployee(), payrollProcess.getPayrollId(), payrollProcess.getValidFrom(), payrollProcess.getValidTo());
		if (attribute == null || payrollConcept.isManual()) {
			createDummyMovement(employee, payrollConcept, transactionName);
			return null;
		}

		logger.info("Concept : " + payrollConcept.getName());
		movement = createMovement(payrollProcess, payrollConcept, employee, attribute, transactionName);
		if (MHRConcept.TYPE_RuleEngine.equals(payrollConcept.getType())) {
			logger.info("Processing -> Rule to Concept " + payrollConcept.getValue());
			if (activeConceptRule.containsKey(getMovementKey(employee.getBusinessPartnerId(), payrollConcept.getId()))) {
				throw new AdempiereException("Recursion loop detected in concept " + payrollConcept.getValue());
			}
			activeConceptRule.put(getMovementKey(employee.getBusinessPartnerId(), payrollConcept.getId()), true);
			RuleResult result;
			if (MHRAttribute.CALCULATIONTYPEVALUE_Expression
					.equals(attribute.getCalculationTypeValue())) {
				// SP099 — Expresión embebida
				RuleContext ruleCtx = new ParallelContext(
						this, payrollProcess, employee, payrollConcept, transactionName);
				Object exprResult = PayrollCalculationEngine.calculate(
						attribute,
						getScriptParameters(payrollProcess, employee, payrollConcept, ruleCtx));
				result = new DefaultRuleResult(exprResult, null);
			} else {
				result = executeScript(payrollProcess, employee, payrollConcept,
						attribute.getAD_Rule_ID(), transactionName);
			}
			activeConceptRule.remove(getMovementKey(employee.getBusinessPartnerId(), payrollConcept.getId()));
			if(result != null) {
				movement.setColumnValue(result.getResult()); // double rounded in MHRMovement.setColumnValue
				if (result.getDescription() != null) {
					movement.setDescription(result.getDescription());
				}
			}
		}
		movement.setHR_Payroll_ID(payrollProcess.getPayrollId());
//		movement.setHR_PayrollConcept_ID(payrollConceptReference.getHR_PayrollConcept_ID());
		movement.setPeriodNo(payrollProcess.getPeriodNo());
		movement.setProcessed(true);
		//	Break it
		if(breakConcept.containsKey(getMovementKey(employee.getBusinessPartnerId(), payrollConcept.getId()))) {
			breakConcept.remove(getMovementKey(employee.getBusinessPartnerId(), payrollConcept.getId()));
			logger.info("Skip Concept because is break from rule " + payrollConcept.getValue() + " - " + payrollConcept.getName() + " Time elapsed: " + TimeUtil.formatElapsed(System.currentTimeMillis() - startTime));
			return null;
		}
		movements.put(getMovementKey(employee.getBusinessPartnerId(), payrollConcept.getId()), movement);
		logger.info("Concept runned from rule " + payrollConcept.getValue() + " - " + payrollConcept.getName() + " Time elapsed: " + TimeUtil.formatElapsed(System.currentTimeMillis() - startTime));
		//	Save Movement
		long startSavingMovementTime = System.currentTimeMillis();
		if (payrollConcept.isManual()) {
			logger.fine("Skip saving " + movement);
		} else {
			boolean saveThisRecord = (payrollConcept.isSaveInHistoric() 
											|| movement.isPrinted() 
											|| payrollConcept.isPaid() 
											|| payrollConcept.isPrinted()) 
									&& (!payrollConcept.isNotSaveInHistoryIfNull() || !movement.isEmpty());
			if (saveThisRecord) {
				movement.saveEx(transactionName);
			}
		}
		logger.info("Saving Concept " + payrollConcept.getValue() + " - " + payrollConcept.getName() + " Time elapsed: " + TimeUtil.formatElapsed(System.currentTimeMillis() - startSavingMovementTime));
		return movement;
	}
	
	private RuleResult executeScriptEngine(PayrollProcess payrollProcess, PayrollEmployee employee, PayrollConcept concept, MRule rule, String transactionName) {
		long startTime = System.currentTimeMillis();
		RuleResult result = null;
		RuleContext ruleContext = new ParallelContext(this, payrollProcess, employee, concept, transactionName);
		try {
			String text = "";
			if (rule.getScript() != null) {
				String rawScript = rule.getScript().trim();
				String engineName = rule.getEngineName();
				boolean isJvmEngine = "groovy".equalsIgnoreCase(engineName)
						|| "beanshell".equalsIgnoreCase(engineName)
						|| "jython".equalsIgnoreCase(engineName);
				text = isJvmEngine
						? rawScript.replaceAll("\\bget", "ruleContext.getEngineHelper().get")
						           .replace(".ruleContext.getEngineHelper().get", ".get")
						: rawScript;
			}
			boolean needsJavaImports = "groovy".equalsIgnoreCase(rule.getEngineName())
					|| "beanshell".equalsIgnoreCase(rule.getEngineName())
					|| "jython".equalsIgnoreCase(rule.getEngineName());
			final String script = needsJavaImports
					? (" import org.eevolution.model.*;" 
							+ Env.NL + "import org.eevolution.hr.model.*;"
							+ Env.NL + "import org.eevolution.hr.util.*;"
							+ Env.NL + "import org.compiere.model.*;"
							+ Env.NL + "import org.adempiere.model.*;"
							+ Env.NL + "import org.compiere.util.*;"
							+ Env.NL + "import org.spin.model.*;"
							+ Env.NL + "import org.spin.hr.model.*;"
							+ Env.NL + "import org.spin.tar.model.*;"
							+ Env.NL + "import org.spin.util.*;"
							+ Env.NL + "import org.spin.hr.util.*;"
							+ Env.NL + "import java.util.*;" 
							+ Env.NL + "import java.math.*;"
							+ Env.NL + "import java.sql.*;"
							+ Env.NL + text)
					: text;

			ScriptEngine engine = rule.getScriptEngine();
			final ScriptContext context = new SimpleScriptContext();
			 getScriptParameters(payrollProcess, employee, concept, ruleContext).entrySet().stream().forEach(entry -> context.setAttribute(entry.getKey(), entry.getValue(), ScriptContext.ENGINE_SCOPE));
		    context.setAttribute("description", "", ScriptContext.ENGINE_SCOPE);
			//	Yamel Senih Add DefValue to another Types
			Object defaultValue = 0.0;
			if  (MHRAttribute.COLUMNTYPE_Date.equals(concept.getColumnType())
					|| MHRAttribute.COLUMNTYPE_Text.equals(concept.getColumnType())) {
				defaultValue = null;
			}
			context.setAttribute("result", defaultValue, ScriptContext.ENGINE_SCOPE);
		    Object engineResult = engine.eval(script, context);
			if (engineResult != null && "@Error@".equals(engineResult.toString())) {
				throw new AdempiereException("@AD_Rule_ID@ @HR_Concept_ID@ "+ concept.getValue() + "" + concept.getName()+ "	 @@Error@ " + engineResult);
			}
			//	
			Object description = context.getAttribute("description");
			result = new DefaultRuleResult(engineResult, description);
			long elapsed = System.currentTimeMillis() - startTime;
			logger.info("ScriptResult -> Concept Name " + concept.getName() + " = " + engineResult + " Time elapsed: " + TimeUtil.formatElapsed(elapsed));
		} catch (Exception e) {
			throw new AdempiereException(e.getLocalizedMessage());
		}
		return result;
	}

	/**
	 * Execute the script
	 * @param ruleId
	 * @param columnType Column Type
	 * @return Object
	 */
	private RuleResult executeScript(PayrollProcess payrollProcess, PayrollEmployee employee, PayrollConcept concept, int ruleId, String transactionName) {
		long startTime = System.currentTimeMillis();
		MRule rule = MRule.get(getCtx(), ruleId);
		RuleResult result = null;
		RuleContext ruleContext = new ParallelContext(this, payrollProcess, employee, concept, transactionName);
		try {
			if (rule == null
					|| rule.getAD_Rule_ID() <= 0) {
				logger.log(Level.WARNING, " @AD_Rule_ID@ @NotFound@");
				return null;
			}
			if (!(rule.getEventType().equals(MRule.EVENTTYPE_HumanResourcePayroll)
					&& rule.getRuleType().equals(MRule.RULETYPE_JSR223ScriptingAPIs))) {
				logger.log(Level.WARNING, " must be of type JSR 223 and event human resource");
				return null;
			}
			boolean isRunned = false;
			if(rule.isRuleClassGenerated()) {
				try {
					RuleRunner runner = RuleRunnerFactory.getRuleRunnerInstance(rule);
					if(runner != null) {
						isRunned = true;
						result = runner.run(ruleContext);
					} else {
						String className = RuleEngineUtil.getCompleteClassName(rule);
						if(!Util.isEmpty(className)) {
							RuleInterface ruleEngine = PayrollEngineHandler.getInstance().getRuleEngine(rule);
							if(ruleEngine != null) {
								isRunned = true;
								Object engineResult = ruleEngine.run(getProcess(), getScriptParameters(payrollProcess, employee, concept, ruleContext));
								Object description = ruleEngine.getDescription();
								result = new DefaultRuleResult(engineResult, description);
							}
						}
					}
				} catch (ClassNotFoundException e) {	//	For Class not found
					logger.log(Level.WARNING, e.getLocalizedMessage());
				} catch (Exception e) {	//	For other exception
					throw new AdempiereException(e);
				}
			}
			
			//	if the class is not loaded then run from rule
			if(!isRunned) {
				if (rule.getEngineName() != null)
					return  executeScriptEngine(payrollProcess, employee, concept, rule, transactionName);

				String text = "";
				if (rule.getScript() != null) {
					text = rule.getScript().trim().replaceAll("\\bget", "engineHelper.get")
					.replace(".engineHelper.get", ".get");
				}
				String resultType = "double";
				//	Yamel Senih Add DefValue to another Types
				String defValue = "0";
				if  (MHRAttribute.COLUMNTYPE_Date.equals(concept.getColumnType())) {
					resultType = "Timestamp";
					defValue = "null";
				} else if  (MHRAttribute.COLUMNTYPE_Text.equals(concept.getColumnType())) {
					resultType = "String";
					defValue = "null";
				}
				final String script =
								" import org.eevolution.model.*;" 
								+ Env.NL + "import org.eevolution.hr.model.*;"
								+ Env.NL + "import org.eevolution.hr.util.*;"
								+ Env.NL + "import org.compiere.model.*;"
								+ Env.NL + "import org.adempiere.model.*;"
								+ Env.NL + "import org.compiere.util.*;"
								+ Env.NL + "import org.spin.model.*;"
								+ Env.NL + "import org.spin.hr.model.*;"
								+ Env.NL + "import org.spin.tar.model.*;"
								+ Env.NL + "import org.spin.util.*;"
								+ Env.NL + "import org.spin.hr.util.*;"
								+ Env.NL + "import java.util.*;" 
								+ Env.NL + "import java.math.*;"
								+ Env.NL + "import java.sql.*;"
								+ Env.NL + resultType + " result = "+ defValue +";"
								+ Env.NL + "String description = null;"
								+ Env.NL + text;
				Scriptlet engine = new Scriptlet (Scriptlet.VARIABLE, script, getScriptParameters(payrollProcess, employee, concept, ruleContext));
				Exception ex = engine.execute();
				if (ex != null) {
					throw ex;
				}
				Object engineResult = engine.getResult(false);
				Object description = engine.getDescription();
				result = new DefaultRuleResult(engineResult, description);
			}
			long elapsed = System.currentTimeMillis() - startTime;
			logger.info("ScriptResult -> Concept Name " + concept.getName() + " = " + result + " Time elapsed: " + TimeUtil.formatElapsed(elapsed));
		} catch (Exception e) {
			throw new AdempiereException("@HR_Employee_ID@ : " + employee.getName() + " " + employee.getLastName() 
			+ " \n @HR_Concept_ID@ " + concept.getValue() + " -> " + concept.getName()
			+ " \n @AD_Rule_ID@=" + rule.getValue() + "\n  Script : " + rule.getScript() + " \n Execution error : \n" + e.getLocalizedMessage());
		}
		return result;
	}
	
	private HashMap<String, Object> getScriptParameters(PayrollProcess payrollProcess, PayrollEmployee employee, PayrollConcept concept, RuleContext ruleContext) {
		/** the context for rules */
		HashMap<String, Object> scriptCtx = new HashMap<String, Object>();
		scriptCtx.put("ruleContext", ruleContext);
		scriptCtx.put("engineHelper", ruleContext.getEngineHelper());
		scriptCtx.put("payrollEngine", this);
		scriptCtx.put("process", getProcess());
		scriptCtx.put("_Process", payrollProcess.getId());
		scriptCtx.put("_Period", payrollProcess.getPeriodId());
		scriptCtx.put("_Payroll", payrollProcess.getPayrollId());
		scriptCtx.put("_Department", payrollProcess.getDepartmentId());
		scriptCtx.put("_From", payrollProcess.getValidFrom());
		scriptCtx.put("_To", payrollProcess.getValidTo());
		scriptCtx.put("_Period", payrollProcess.getPeriodNo());
		scriptCtx.put("_PeriodNo", payrollProcess.getPeriodNo());
		scriptCtx.put("_HR_Period_ID", payrollProcess.getPeriodId());
		scriptCtx.put("_HR_Payroll_Value", payrollProcess.getPayrollValue());
		//	Scope
		scriptCtx.put("SCOPE_PROCESS", HRProcessActionMsg.SCOPE_PROCESS);
		scriptCtx.put("SCOPE_EMPLOYEE", HRProcessActionMsg.SCOPE_EMPLOYEE);
		scriptCtx.put("SCOPE_CONCEPT", HRProcessActionMsg.SCOPE_CONCEPT);
		scriptCtx.put("PERSISTENCE_SAVE", HRProcessActionMsg.PERSISTENCE_SAVE);
		scriptCtx.put("PERSISTENCE_IGNORE", HRProcessActionMsg.PERSISTENCE_IGNORE);
		scriptCtx.put("ACTION_BREAK", HRProcessActionMsg.ACTION_BREAK);
		//	Employee
		scriptCtx.put("_DateStart", employee.getStartDate());
		scriptCtx.put("_DateEnd", employee.getEndDate() == null ? payrollProcess.getValidTo() == null ? payrollProcess.getDateAcct() : payrollProcess.getValidTo() : employee.getEndDate());
		scriptCtx.put("_Days", TimeUtil.getDaysBetween(payrollProcess.getValidFrom(), payrollProcess.getValidTo()) + 1);
		scriptCtx.put("_C_BPartner_ID", employee.getBusinessPartnerId());
		scriptCtx.put("_HR_Employee_ID", employee.getId());
		scriptCtx.put("_C_BPartner", employee.getBusinessPartner());
		scriptCtx.put("_HR_Employee", employee.getEmployee());
		if(employee.getPayrollValue() != null) {
			scriptCtx.put("_HR_Employee_Payroll_Value", employee.getPayrollValue());
			MHRContract contract = MHRContract.getById(getCtx(), employee.getContractId(), null);
			scriptCtx.put("_HR_Employee_Contract", contract);
		}
		Timestamp employeeValidFrom = payrollProcess.getValidFrom();
		Timestamp employeeValidTo = payrollProcess.getValidTo();
		//	Valid from for employee
		if(employee.getStartDate() != null && payrollProcess.getValidFrom() != null && employee.getStartDate().getTime() > payrollProcess.getValidFrom().getTime()) {
			employeeValidFrom = employee.getStartDate();
		}
		//  Valid to for employee
		if(employee.getEndDate() != null && payrollProcess.getValidTo() != null && employee.getEndDate().getTime() < payrollProcess.getValidTo().getTime()) {
			employeeValidTo = employee.getEndDate();
		}
		//	Get Employee valid from and to
		scriptCtx.put("_HR_Employee_ValidFrom", employeeValidFrom);
		scriptCtx.put("_HR_Employee_ValidTo", employeeValidTo);
		scriptCtx.put("_HR_Concept_ID", concept.getId());
		scriptCtx.put("_HR_Concept", concept.getConcept());
		return scriptCtx;
	}
	
	/**
	 * Create movement based on concept , attribute and is printed
	 * @param concept
	 * @param attribute
	 * @param isPrinted
	 * @return
	 */
	private MHRMovement createMovement(PayrollProcess payrollProcess, PayrollConcept concept, PayrollEmployee employee, MHRAttribute attribute, String transactionName) {
		MHRPeriod payrollPeriod = MHRPeriod.getById(getCtx(), payrollProcess.getPeriodId(), null);
		MHRMovement movement = new MHRMovement (getCtx(), 0, transactionName);
		movement.setAD_Org_ID(employee.getOrganizationId());
		movement.setSeqNo(concept.getSequence());
		movement.setHR_Attribute_ID(attribute.getHR_Attribute_ID());
		movement.setDescription(attribute.getDescription());
		movement.setReferenceNo(attribute.getReferenceNo());
		Optional.ofNullable(payrollPeriod).ifPresent(period -> movement.setPeriodNo(period.getPeriodNo()));
		movement.setC_BPartner_ID(employee.getBusinessPartnerId());
		movement.setC_BP_Relation_ID(attribute.getC_BP_Relation_ID());
		movement.setHR_Concept_ID(concept.getId());
		movement.setHR_Concept_Category_ID(concept.getCategoryId());
		movement.setHR_Concept_Type_ID(concept.getTypeId());
		movement.setHR_Process_ID(payrollProcess.getId());
		movement.setAD_Rule_ID(attribute.getAD_Rule_ID());
		movement.setValidFrom(payrollProcess.getValidFrom());
		movement.setValidTo(payrollProcess.getValidTo());
		movement.setIsPrinted(concept.isPrinted());
		movement.setIsManual(concept.isManual());
		movement.setC_BP_Group_ID(employee.getBusinessPartnerGroupId());
		movement.setEmployee(employee.getEmployee());
		if (!MHRConcept.TYPE_RuleEngine.equals(concept.getType())) {
			int precision = MCurrency.getStdPrecision(getCtx(), Env.getContextAsInt(getCtx(), "#C_Currency_ID"));
			if(concept.getPrecision() > 0) {
				precision = concept.getPrecision();
			}
			BigDecimal rate = Env.ONE;
			BigDecimal amount = attribute.getAmount();
			if(attribute.isConvertedAmount() && attribute.getC_Currency_ID() != payrollProcess.getCurrencyId()) {
				rate = MConversionRate.getRate(attribute.getC_Currency_ID(), payrollProcess.getCurrencyId(), payrollProcess.getDateAcct(), payrollProcess.getConversionTypeId(), payrollProcess.getClientId(), payrollProcess.getOrganizationId());
				if(rate != null) {
					amount = rate.multiply(Optional.ofNullable(amount).orElse(Env.ZERO))
							.setScale(precision, RoundingMode.HALF_UP);	
				}
			}
			movement.setAmount(amount);
			movement.setQty(attribute.getQty());
			movement.setTextMsg(attribute.getTextMsg());
			movement.setServiceDate(attribute.getServiceDate());
		}
		return movement;
	}
	
	private void createDummyMovement(PayrollEmployee employee, PayrollConcept payrollConcept, String transactionName) {
		MHRMovement dummyMovement = new MHRMovement (getCtx(), 0, transactionName);
		dummyMovement.setSeqNo(payrollConcept.getSequence());
		dummyMovement.setIsManual(true); // to avoid landing on movement table
		movements.put(getMovementKey(employee.getBusinessPartnerId(), payrollConcept.getId()), dummyMovement);
		return;
	}
	
	private int deleteMovements() {
		AtomicInteger no = new AtomicInteger();
		Trx.run(transactionName -> {
			// RE-Process, delete movement except concept type Incidence
			no.set(DB.executeUpdateEx("DELETE FROM HR_Movement m WHERE HR_Process_ID=? AND IsManual<>?", new Object[]{getProcess().getHR_Process_ID(), true}, transactionName));
			logger.info("Movements Deleted #" + no);
		});
		return no.get();
	}
	
	private List<Integer> getEmployeeIds() {
		List<Object> params = new ArrayList<Object>();
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("EXISTS(SELECT 1 FROM HR_Employee e " + 
				"WHERE e.C_BPartner_ID = C_BPartner.C_BPartner_ID " +
				"AND (e.EmployeeStatus = ? OR EmployeeStatus IS NULL) ");
		MHRProcess process = getProcess();
		//	For Active
		params.add(MHREmployee.EMPLOYEESTATUS_Active);
		//	look if it is a not regular payroll
		MHRPayroll payroll = MHRPayroll.getById(process.getCtx(), process.getHR_Payroll_ID(), null);
		// This payroll not content periods, NOT IS a Regular Payroll > ogi-cd 28Nov2007
		if(process.getHR_Payroll_ID() != 0 && process.getHR_Period_ID() != 0 && !payroll.isIgnoreDefaultPayroll())
		{
			whereClause.append(" AND (e.HR_Payroll_ID IS NULL OR e.HR_Payroll_ID=?) " );
			params.add(process.getHR_Payroll_ID());
		}
		//	Organization
		if(process.getAD_OrgTrx_ID() != 0) {
			whereClause.append(" AND e.AD_OrgTrx_ID=? " );
			params.add(process.getAD_OrgTrx_ID());
		}
		//	Project
		if(process.getC_Project_ID() != 0) {
			whereClause.append(" AND e.C_Project_ID=? " );
			params.add(process.getC_Project_ID());
		}
		//	Activity
		if(process.getC_Activity_ID() != 0) {
			whereClause.append(" AND e.C_Activity_ID=? " );
			params.add(process.getC_Activity_ID());
		}
		//	Campaign
		if(process.getC_Campaign_ID() != 0) {
			whereClause.append(" AND e.C_Campaign_ID=? " );
			params.add(process.getC_Campaign_ID());
		}
		//	Sales Region
		if(process.getC_SalesRegion_ID() != 0) {
			whereClause.append(" AND e.C_SalesRegion_ID=? " );
			params.add(process.getC_SalesRegion_ID());
		}
		//	Active Record
		whereClause.append(" AND e.IsActive = 'Y' " );
		// HR Period
		if(process.getHR_Period_ID() == 0) {
			whereClause.append(" AND e.StartDate <=? ");
			params.add(process.getDateAcct());
		} else {
			MHRPeriod period = new MHRPeriod(process.getCtx(), process.getHR_Period_ID(), process.get_TrxName());
			whereClause.append(" AND e.StartDate <=? ");
			params.add(period.getEndDate());
			whereClause.append(" AND (e.EndDate IS NULL OR e.EndDate >=?) ");
			params.add(period.getStartDate());
		}
		
		// Selected Department
		if (process.getHR_Department_ID() != 0) {
			whereClause.append(" AND e.HR_Department_ID =? ");
			params.add(process.getHR_Department_ID());
		}		
		// Selected Job add
		if (process.getHR_Job_ID() != 0) {
			whereClause.append(" AND e.HR_Job_ID =? ");
			params.add(process.getHR_Job_ID());
		}
		
		whereClause.append(" ) "); // end select from HR_Employee
		
		// Selected Employee
		if (process.getC_BPartner_ID() != 0) {
			whereClause.append(" AND C_BPartner_ID =? ");
			params.add(process.getC_BPartner_ID());
		}
		
		//client
		whereClause.append(" AND AD_Client_ID =? ");
		params.add(process.getAD_Client_ID());
		//	
		return new Query(process.getCtx(), MBPartner.Table_Name, whereClause.toString(), process.get_TrxName())
								.setParameters(params)
								.setOnlyActiveRecords(true)
								.setOrderBy(MBPartner.COLUMNNAME_Name)
								.getIDsAsList();
	}
	
	private List<Integer> getPayrollConceptIds() {
		MHRProcess process = getProcess();
		return new Query(process.getCtx(), MHRPayrollConcept.Table_Name, MHRPayroll.COLUMNNAME_HR_Payroll_ID+"=?", null)
												.setOnlyActiveRecords(true)
												.setParameters(process.getHR_Payroll_ID())
												.setOrderBy(MHRPayrollConcept.COLUMNNAME_SeqNo)
												.getIDsAsList();
	}

	@Override
	public void breakEmployeeRunning(int businessPartnerId) {
		breakEmployee.put(businessPartnerId, true);
	}

	@Override
	public void breakConceptRunning(int businessPartnerId, int conceptId) {
		breakConcept.put(getMovementKey(businessPartnerId, conceptId), true);
	}

	@Override
	public void breakProcessRunning() {
		//	TODO: allows break process
	}
}
