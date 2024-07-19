/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2012-2024 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com                                         *
 *************************************************************************************/
package org.spin.base.util;

import java.util.List;

import org.adempiere.core.domains.models.I_AD_WF_EventAudit;
import org.adempiere.core.domains.models.I_AD_WF_NextCondition;
import org.adempiere.core.domains.models.I_AD_WF_Node;
import org.adempiere.core.domains.models.I_AD_WF_NodeNext;
import org.adempiere.core.domains.models.I_AD_Window;
import org.adempiere.core.domains.models.I_C_Order;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.MWindow;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.compiere.wf.MWFActivity;
import org.compiere.wf.MWFEventAudit;
import org.compiere.wf.MWFNextCondition;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFNodeNext;
import org.compiere.wf.MWFProcess;
import org.compiere.wf.MWFResponsible;
import org.compiere.wf.MWorkflow;
import org.spin.backend.grpc.wf.Action;
import org.spin.backend.grpc.wf.ConditionType;
import org.spin.backend.grpc.wf.DurationUnit;
import org.spin.backend.grpc.wf.Operation;
import org.spin.backend.grpc.wf.PublishStatus;
import org.spin.backend.grpc.wf.WorkflowActivity;
import org.spin.backend.grpc.wf.WorkflowCondition;
import org.spin.backend.grpc.wf.WorkflowDefinition;
import org.spin.backend.grpc.wf.WorkflowEvent;
import org.spin.backend.grpc.wf.WorkflowNode;
import org.spin.backend.grpc.wf.WorkflowProcess;
import org.spin.backend.grpc.wf.WorkflowState;
import org.spin.backend.grpc.wf.WorkflowTransition;
import org.spin.backend.grpc.wf.ZoomWindow;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.util.ASPUtil;

/**
 * Class for handle workflow conversion values
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class WorkflowUtil {
	
	/**
	 * Convert PO class from Workflow process to builder
	 * @param workflowProcess
	 * @return
	 */
	public static WorkflowProcess.Builder convertWorkflowProcess(MWFProcess workflowProcess) {
		MTable table = MTable.get(workflowProcess.getCtx(), workflowProcess.getAD_Table_ID());
		MWorkflow workflow = MWorkflow.get(workflowProcess.getCtx(), workflowProcess.getAD_Workflow_ID());

		WorkflowProcess.Builder builder = WorkflowProcess.newBuilder()
			.setProcessId(
				workflowProcess.getAD_WF_Process_ID()
			)
			.setWorkflowId(
				workflowProcess.getAD_Workflow_ID()
			)
			.setWorkflowName(
				ValueManager.validateNull(
					workflow.get_Translation(
						MWorkflow.COLUMNNAME_Name
					)
				)
			)
			.setId(
				workflowProcess.getRecord_ID()
			)
			.setTableName(
				ValueManager.validateNull(
					table.getTableName()
				)
			)
			.setTextMessage(
				ValueManager.validateNull(
					Msg.parseTranslation(
						workflowProcess.getCtx(),
						workflowProcess.getTextMsg()
					)
				)
			)
			.setProcessed(
				workflowProcess.isProcessed()
			)
			.setLogDate(
				ValueManager.getTimestampFromDate(
					workflowProcess.getCreated()
				)
			)
			.setPriorityValue(
				workflowProcess.getPriority()
			)
		;

		if(workflowProcess.getAD_WF_Responsible_ID() > 0) {
			MWFResponsible responsible = MWFResponsible.get(workflowProcess.getCtx(), workflowProcess.getAD_WF_Responsible_ID());
			builder.setResponsibleId(
					responsible.getAD_WF_Responsible_ID()
				)
				.setResponsibleName(
					ValueManager.validateNull(
						responsible.getName()
					)
				)
			;
		}
		if(workflowProcess.getAD_User_ID() != 0) {
			MUser user = MUser.get(workflowProcess.getCtx(), workflowProcess.getAD_User_ID());
			builder.setUserId(
					user.getAD_User_ID()
				)
				.setUserName(
					ValueManager.validateNull(
						user.getName()
					)
				)
			;
		}
		//	State
		if(!Util.isEmpty(workflowProcess.getWFState())) {
			if(workflowProcess.getWFState().equals(MWFProcess.WFSTATE_Running)) {
				builder.setWorkflowState(WorkflowState.RUNNING);
			} else if(workflowProcess.getWFState().equals(MWFProcess.WFSTATE_Completed)) {
				builder.setWorkflowState(WorkflowState.COMPLETED);
			} else if(workflowProcess.getWFState().equals(MWFProcess.WFSTATE_Aborted)) {
				builder.setWorkflowState(WorkflowState.ABORTED);
			} else if(workflowProcess.getWFState().equals(MWFProcess.WFSTATE_Terminated)) {
				builder.setWorkflowState(WorkflowState.TERMINATED);
			} else if(workflowProcess.getWFState().equals(MWFProcess.WFSTATE_Suspended)) {
				builder.setWorkflowState(WorkflowState.SUSPENDED);
			} else if(workflowProcess.getWFState().equals(MWFProcess.WFSTATE_NotStarted)) {
				builder.setWorkflowState(WorkflowState.NOT_STARTED);
			}
		}

		//	Get Events
		List<MWFEventAudit> workflowEventsList = new Query(workflowProcess.getCtx(), I_AD_WF_EventAudit.Table_Name, I_AD_WF_EventAudit.COLUMNNAME_AD_WF_Process_ID + " = ?", null)
			.setParameters(workflowProcess.getAD_WF_Process_ID())
			.<MWFEventAudit>list();
		//	populate
		for(MWFEventAudit eventAudit : workflowEventsList) {
			WorkflowEvent.Builder valueObject = convertWorkflowEventAudit(eventAudit);
			builder.addWorkflowEvents(valueObject.build());
		}
  		return builder;
	}
	
	/**
	 * Convert PO class from Workflow to builder
	 * @param workflow
	 * @return
	 */
	public static WorkflowDefinition.Builder convertWorkflowDefinition(MWorkflow workflow) {
		MTable table = MTable.get(workflow.getCtx(), workflow.getAD_Table_ID());

		WorkflowDefinition.Builder builder = WorkflowDefinition.newBuilder()
			.setId(
				workflow.getAD_Workflow_ID()
			)
			.setUuid(
				ValueManager.validateNull(
					workflow.getUUID()
				)
			)
			.setValue(
				ValueManager.validateNull(
					workflow.getValue()
				)
			)
			.setName(
				ValueManager.validateNull(
					workflow.get_Translation(
						MWorkflow.COLUMNNAME_Name
					)
				)
			)
			.setDescription(
				ValueManager.validateNull(
					workflow.get_Translation(
						MWorkflow.COLUMNNAME_Description
					)
				)
			)
			.setHelp(
				ValueManager.validateNull(
					workflow.get_Translation(
						MWorkflow.COLUMNNAME_Help
					)
				)
			)
			.setPriority(
				workflow.getPriority()
			)
			.setTableName(
				ValueManager.validateNull(
					table.getTableName()
				)
			)
			.setIsDefault(
				workflow.isDefault()
			)
			.setIsValid(
				workflow.isValid()
			)
		;

		if(workflow.getAD_WF_Responsible_ID() > 0) {
			MWFResponsible responsible = MWFResponsible.get(workflow.getCtx(), workflow.getAD_WF_Responsible_ID());
			builder.setResponsibleId(
					responsible.getAD_WF_Responsible_ID()
				)
				.setResponsibleName(
					ValueManager.validateNull(
						responsible.getName()
					)
				)
			;
		}
		if(workflow.getValidFrom() != null) {
			builder.setValidFrom(ValueManager.getTimestampFromDate(workflow.getValidFrom()));
		}
		//	Duration Unit
		if(!Util.isEmpty(workflow.getDurationUnit())) {
			if(workflow.getDurationUnit().equals(MWorkflow.DURATIONUNIT_Day)) {
				builder.setDurationUnitValue(DurationUnit.HOUR_VALUE);
			} else if(workflow.getDurationUnit().equals(MWorkflow.DURATIONUNIT_Minute)) {
				builder.setDurationUnitValue(DurationUnit.MINUTE_VALUE);
			} else if(workflow.getDurationUnit().equals(MWorkflow.DURATIONUNIT_Month)) {
				builder.setDurationUnitValue(DurationUnit.MONTH_VALUE);
			} else if(workflow.getDurationUnit().equals(MWorkflow.DURATIONUNIT_Second)) {
				builder.setDurationUnitValue(DurationUnit.SECOND_VALUE);
			} else if(workflow.getDurationUnit().equals(MWorkflow.DURATIONUNIT_Year)) {
				builder.setDurationUnitValue(DurationUnit.YEAR_VALUE);
			}
		}
		//	Publish Status
		if(!Util.isEmpty(workflow.getPublishStatus())) {
			if(workflow.getPublishStatus().equals(MWorkflow.PUBLISHSTATUS_Released)) {
				builder.setPublishStatusValue(PublishStatus.RELEASED_VALUE);
			} else if(workflow.getPublishStatus().equals(MWorkflow.PUBLISHSTATUS_Test)) {
				builder.setDurationUnitValue(PublishStatus.TEST_VALUE);
			} else if(workflow.getPublishStatus().equals(MWorkflow.PUBLISHSTATUS_UnderRevision)) {
				builder.setDurationUnitValue(PublishStatus.UNDER_REVISION_VALUE);
			} else if(workflow.getPublishStatus().equals(MWorkflow.PUBLISHSTATUS_Void)) {
				builder.setDurationUnitValue(PublishStatus.VOID_VALUE);
			}
		}
		//	Next node
		if(workflow.getAD_WF_Node_ID() != 0) {
			MWFNode startNode = MWFNode.get(workflow.getCtx(), workflow.getAD_WF_Node_ID());
			builder.setStartNode(
				convertWorkflowNode(startNode)
			);
		}
		//	Get Events
		List<MWFNode> workflowNodeList = new Query(workflow.getCtx(), I_AD_WF_Node.Table_Name, I_AD_WF_Node.COLUMNNAME_AD_Workflow_ID + " = ?", null)
			.setParameters(workflow.getAD_Workflow_ID())
			.<MWFNode>list();
		//	populate
		for(MWFNode node : workflowNodeList) {
			WorkflowNode.Builder valueObject = convertWorkflowNode(node);
			builder.addWorkflowNodes(valueObject.build());
		}
  		return builder;
	}
	
	/**
	 * Convert PO class from Workflow node to builder
	 * @param node
	 * @return
	 */
	public static WorkflowNode.Builder convertWorkflowNode(MWFNode node) {
		WorkflowNode.Builder builder = WorkflowNode.newBuilder()
			.setId(
				node.getAD_WF_Node_ID()
			)
			.setValue(
				ValueManager.validateNull(
					node.getValue()
				)
			)
			.setName(
				ValueManager.validateNull(
					node.get_Translation(MWFNode.COLUMNNAME_Name)
				)
			)
			.setDescription(
				ValueManager.validateNull(
					node.get_Translation(MWFNode.COLUMNNAME_Description)
				))
			.setHelp(
				ValueManager.validateNull(
					node.get_Translation(MWFNode.COLUMNNAME_Help)
				)
			)
			.setPriority(
				node.getPriority()
			)
		;

		if(node.getAD_WF_Responsible_ID() > 0) {
			MWFResponsible responsible = MWFResponsible.get(node.getCtx(), node.getAD_WF_Responsible_ID());
			builder.setResponsibleId(
					responsible.getAD_WF_Responsible_ID()
				)
				.setResponsibleName(
					ValueManager.validateNull(
						responsible.getName()
					)
				)
			;
		}

		// set action node
		switch (node.getAction()) {
			case MWFNode.ACTION_UserChoice:
				builder.setAction(Action.USER_CHOICE);
				break;
			case MWFNode.ACTION_DocumentAction:
				builder.setAction(Action.DOCUMENT_ACTION);
				break;
			case MWFNode.ACTION_SubWorkflow:
				builder.setAction(Action.SUB_WORKFLOW);
				break;
			case MWFNode.ACTION_EMail:
				builder.setAction(Action.EMAIL);
				break;
			case MWFNode.ACTION_AppsProcess:
				builder.setAction(Action.APPS_PROCESS);
				break;
			case MWFNode.ACTION_SmartView:
				builder.setAction(Action.SMART_VIEW);
				break;
			case MWFNode.ACTION_AppsReport:
				builder.setAction(Action.APPS_REPORT);
				break;
			case MWFNode.ACTION_SmartBrowse:
				builder.setAction(Action.SMART_BROWSE);
				break;
			case MWFNode.ACTION_AppsTask:
				builder.setAction(Action.APPS_TASK);
				break;
			case MWFNode.ACTION_SetVariable:
				builder.setAction(Action.SET_VARIABLE);
				break;
			case MWFNode.ACTION_UserWindow:
				builder.setAction(Action.USER_WINDOW);
				break;
			case MWFNode.ACTION_UserForm:
				builder.setAction(Action.USER_FORM);
				break;
			case MWFNode.ACTION_WaitSleep:
			default:
				builder.setAction(Action.WAIT_SLEEP);
				break;
		}

		//	Get Events
		List<MWFNodeNext> workflowNodeTransitionList = new Query(
				node.getCtx(),
				I_AD_WF_NodeNext.Table_Name,
				I_AD_WF_NodeNext.COLUMNNAME_AD_WF_Node_ID + " = ?",
				null
			)
			.setParameters(node.getAD_WF_Node_ID())
			.<MWFNodeNext>list();
		//	populate
		for(MWFNodeNext nodeNext : workflowNodeTransitionList) {
			WorkflowTransition.Builder valueObject = convertTransition(nodeNext);
			builder.addTransitions(valueObject.build());
		}
  		return builder;
	}
	
	/**
	 * Convert PO class from Transition to builder
	 * @param transition
	 * @return
	 */
	public static WorkflowTransition.Builder convertTransition(MWFNodeNext transition) {
		MWFNode nodeNext = MWFNode.get(transition.getCtx(), transition.getAD_WF_Next_ID());

		WorkflowTransition.Builder builder = WorkflowTransition.newBuilder()
			.setNodeNextId(
				nodeNext.getAD_WF_Node_ID()
			)
			.setNodeNextName(
				ValueManager.validateNull(
					nodeNext.getName()
				)
			)
			.setId(
				transition.getAD_WF_NodeNext_ID()
			)
			.setUuid(
				ValueManager.validateNull(
					transition.getUUID()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					transition.getDescription()
				)
			)
			.setSequence(
				transition.getSeqNo()
			)
			.setIsStdUserWorkflow(
				transition.isStdUserWorkflow()
			)
		;

		//	Get Events
		List<MWFNextCondition> workflowNodeTransitionList = new Query(
				transition.getCtx(),
				I_AD_WF_NextCondition.Table_Name,
				I_AD_WF_NextCondition.COLUMNNAME_AD_WF_NodeNext_ID + " = ?", null
			)
			.setParameters(transition.getAD_WF_Node_ID())
			.<MWFNextCondition>list();
		//	populate
		for(MWFNextCondition nextCondition : workflowNodeTransitionList) {
			WorkflowCondition.Builder valueObject = convertWorkflowCondition(nextCondition);
			builder.addWorkflowConditions(valueObject.build());
		}
  		return builder;
	}
	
	/**
	 * Convert PO class from Workflow condition to builder
	 * @param condition
	 * @return
	 */
	public static WorkflowCondition.Builder convertWorkflowCondition(MWFNextCondition condition) {
		WorkflowCondition.Builder builder = WorkflowCondition.newBuilder()
			.setId(
				condition.getAD_WF_NextCondition_ID()
			)
			.setUuid(
				ValueManager.validateNull(
					condition.getUUID()
				)
			)
			.setSequence(
				condition.getSeqNo()
			)
			.setValue(
				ValueManager.validateNull(
					condition.getValue()
				)
			)
		;

		MColumn column = MColumn.get(condition.getCtx(), condition.getAD_Column_ID());
		builder.setColumnName(
			ValueManager.validateNull(
				column.getColumnName()
			)
		);

		//	Condition Type
		if(!Util.isEmpty(condition.getAndOr())) {
			if(condition.getAndOr().equals(MWFNextCondition.ANDOR_And)) {
				builder.setConditionTypeValue(ConditionType.AND_VALUE);
			} else if(condition.getAndOr().equals(MWFNextCondition.ANDOR_Or)) {
				builder.setConditionTypeValue(ConditionType.OR_VALUE);
			}
		}
		//	Operation
		if(!Util.isEmpty(condition.getOperation())) {
			if(condition.getOperation().equals(MWFNextCondition.OPERATION_Eq)) {
				builder.setOperation(Operation.EQUAL);
			} else if(condition.getOperation().equals(MWFNextCondition.OPERATION_NotEq)) {
				builder.setOperation(Operation.NOT_EQUAL);
			} else if(condition.getOperation().equals(MWFNextCondition.OPERATION_Like)) {
				builder.setOperation(Operation.LIKE);
			} else if(condition.getOperation().equals(MWFNextCondition.OPERATION_Gt)) {
				builder.setOperation(Operation.GREATER);
			} else if(condition.getOperation().equals(MWFNextCondition.OPERATION_GtEq)) {
				builder.setOperation(Operation.GREATER_EQUAL);
			} else if(condition.getOperation().equals(MWFNextCondition.OPERATION_Le)) {
				builder.setOperation(Operation.LESS);
			} else if(condition.getOperation().equals(MWFNextCondition.OPERATION_LeEq)) {
				builder.setOperation(Operation.LESS_EQUAL);
			} else if(condition.getOperation().equals(MWFNextCondition.OPERATION_X)) {
				builder.setOperation(Operation.BETWEEN);
			} else if(condition.getOperation().equals(MWFNextCondition.OPERATION_Sql)) {
				builder.setOperation(Operation.SQL);
			}
		}
  		return builder;
	}
	
	/**
	 * Convert PO class from Workflow event audit to builder
	 * @param workflowEventAudit
	 * @return
	 */
	public static WorkflowEvent.Builder convertWorkflowEventAudit(MWFEventAudit workflowEventAudit) {
		MTable table = MTable.get(workflowEventAudit.getCtx(), workflowEventAudit.getAD_Table_ID());
		MWFNode node = MWFNode.get(workflowEventAudit.getCtx(), workflowEventAudit.getAD_WF_Node_ID());

		WorkflowEvent.Builder builder = WorkflowEvent.newBuilder()
			.setNodeId(
				node.getAD_WF_Node_ID()
			)
			.setNodeName(
				ValueManager.validateNull(
					node.get_Translation(
						MWFNode.COLUMNNAME_Name
					)
				)
			)
			.setId(
				workflowEventAudit.getRecord_ID()
			)
			.setTableName(
				ValueManager.validateNull(
					table.getTableName()
				)
			)
			.setTextMessage(
				ValueManager.validateNull(
					Msg.parseTranslation(workflowEventAudit.getCtx(), workflowEventAudit.getTextMsg())
				)
			)
			.setLogDate(
				ValueManager.getTimestampFromDate(
					workflowEventAudit.getCreated()
				)
			)
			.setAttributeName(
				ValueManager.validateNull(
					workflowEventAudit.getAttributeName()
				)
			)
			.setOldValue(
				ValueManager.validateNull(
					workflowEventAudit.getOldValue()
				)
			)
			.setNewValue(
				ValueManager.validateNull(
					workflowEventAudit.getNewValue()
				)
			)
		;

		if(workflowEventAudit.getAD_WF_Responsible_ID() > 0) {
			MWFResponsible responsible = MWFResponsible.get(workflowEventAudit.getCtx(), workflowEventAudit.getAD_WF_Responsible_ID());
			builder.setResponsibleId(
					responsible.getAD_WF_Responsible_ID()
				)
				.setResponsibleName(
					ValueManager.validateNull(
						responsible.getName()
					)
				)
			;
		}
		if(workflowEventAudit.getAD_User_ID() != 0) {
			MUser user = MUser.get(workflowEventAudit.getCtx(), workflowEventAudit.getAD_User_ID());
			builder.setUserId(
					user.getAD_User_ID()
				)
				.setUserName(
					ValueManager.validateNull(
						user.getName()
					)
				)
			;
		}
		if(workflowEventAudit.getElapsedTimeMS() != null) {
			builder.setTimeElapsed(workflowEventAudit.getElapsedTimeMS().longValue());
		}
		//	State
		if(!Util.isEmpty(workflowEventAudit.getWFState())) {
			if(workflowEventAudit.getWFState().equals(MWFProcess.WFSTATE_Running)) {
				builder.setWorkflowState(WorkflowState.RUNNING);
			} else if(workflowEventAudit.getWFState().equals(MWFProcess.WFSTATE_Completed)) {
				builder.setWorkflowState(WorkflowState.COMPLETED);
			} else if(workflowEventAudit.getWFState().equals(MWFProcess.WFSTATE_Aborted)) {
				builder.setWorkflowState(WorkflowState.ABORTED);
			} else if(workflowEventAudit.getWFState().equals(MWFProcess.WFSTATE_Terminated)) {
				builder.setWorkflowState(WorkflowState.TERMINATED);
			} else if(workflowEventAudit.getWFState().equals(MWFProcess.WFSTATE_Suspended)) {
				builder.setWorkflowState(WorkflowState.SUSPENDED);
			} else if(workflowEventAudit.getWFState().equals(MWFProcess.WFSTATE_NotStarted)) {
				builder.setWorkflowState(WorkflowState.NOT_STARTED);
			}
		}
		//	
  		return builder;
	}

	/**
	 * Convert Activity for gRPC
	 * @param workflowActivity
	 * @return
	 */
	public static WorkflowActivity.Builder convertWorkflowActivity(MWFActivity workflowActivity) {
		MTable table = MTable.get(workflowActivity.getCtx(), workflowActivity.getAD_Table_ID());
		MWorkflow workflow = MWorkflow.get(workflowActivity.getCtx(), workflowActivity.getAD_Workflow_ID());
		MWFProcess workflowProcess = (MWFProcess) workflowActivity.getAD_WF_Process();
		MWFNode workflowNode = MWFNode.get(Env.getCtx(), workflowActivity.getAD_WF_Node_ID());

		WorkflowActivity.Builder builder = WorkflowActivity.newBuilder()
			.setWorkflowProcess(
				WorkflowUtil.convertWorkflowProcess(
					workflowProcess
				)
			)
			.setWorkflow(
				WorkflowUtil.convertWorkflowDefinition(
					workflow
				)
			)
			.setNode(
				WorkflowUtil.convertWorkflowNode(
					workflowNode
				)
			)
			.setId(
				workflowActivity.getAD_WF_Activity_ID()
			)
			.setUuid(
				ValueManager.validateNull(
					workflowActivity.getUUID()
				)
			)
			.setTextMessage(
				ValueManager.validateNull(
					Msg.parseTranslation(workflowActivity.getCtx(), workflowActivity.getTextMsg())
				)
			)
			.setProcessed(
				workflowActivity.isProcessed()
			)
			.setCreated(
				ValueManager.getTimestampFromDate(
					workflowActivity.getCreated()
				)
			)
		;

		if(workflowActivity.getAD_WF_Responsible_ID() > 0) {
			MWFResponsible responsible = MWFResponsible.get(workflowActivity.getCtx(), workflowActivity.getAD_WF_Responsible_ID());
			builder.setResponsibleId(
					responsible.getAD_WF_Responsible_ID()
				)
				.setResponsibleName(
					ValueManager.validateNull(
						responsible.getName()
					)
				)
			;
		}
		if(workflowActivity.getAD_User_ID() != 0) {
			MUser user = MUser.get(workflowActivity.getCtx(), workflowActivity.getAD_User_ID());
			builder.setUserId(
					user.getAD_User_ID()
				)
				.setUserName(
					ValueManager.validateNull(
						user.getName()
					)
				)
			;
		}

		// record values
		builder.setRecordId(
				workflowActivity.getRecord_ID()
			)
			.setTableName(
				ValueManager.validateNull(
					table.getTableName()
				)
			)
		;
		PO entity = table.getPO(workflowActivity.getRecord_ID(), null);
		if (entity != null && entity.get_ColumnIndex(I_C_Order.COLUMNNAME_IsSOTrx) >= 0) {
			// to zoom 
			builder.setIsSalesTransaction(
				entity.get_ValueAsBoolean(
					I_C_Order.COLUMNNAME_IsSOTrx
				)
			);
		}

		if (table.getAD_Window_ID() > 0) {
			ZoomWindow.Builder builderZoom = convertZoomWindow(table.getAD_Window_ID());
			builder.addZoomWindows(builderZoom);
		}
		// Purchase Window
		if (table.getPO_Window_ID() > 0) {
			ZoomWindow.Builder builderZoom = convertZoomWindow(table.getPO_Window_ID());
			builder.addZoomWindows(builderZoom);
		}

		if(workflowActivity.getDateLastAlert() != null) {
			builder.setLastAlert(ValueManager.getTimestampFromDate(workflowActivity.getDateLastAlert()));
		}
		//	
  		return builder;
	}


	public static ZoomWindow.Builder convertZoomWindow(int windowId) {
		MWindow window = ASPUtil.getInstance(Env.getCtx()).getWindow(windowId); // new MWindow(context, windowId, null);

		//	Return
		return ZoomWindow.newBuilder()
			.setId(
				window.getAD_Window_ID()
			)
			.setUuid(
				ValueManager.validateNull(
					window.getUUID()
				)
			)
			.setName(
				ValueManager.validateNull(
					window.get_Translation(
						I_AD_Window.COLUMNNAME_Name
					)
				)
			)
			.setDescription(
				ValueManager.validateNull(
					window.get_Translation(
						I_AD_Window.COLUMNNAME_Description
					)
				)
			)
			.setIsSalesTransaction(
				window.isSOTrx()
			)
		;
	}
}
