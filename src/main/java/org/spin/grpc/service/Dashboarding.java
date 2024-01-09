/************************************************************************************
 * Copyright (C) 2012-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.grpc.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.apps.graph.GraphColumn;
import org.adempiere.core.domains.models.I_AD_ChangeLog;
import org.adempiere.core.domains.models.I_AD_Chart;
import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_AD_Menu;
import org.adempiere.core.domains.models.I_AD_Note;
import org.adempiere.core.domains.models.I_AD_Rule;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.I_AD_TreeNodeMM;
import org.adempiere.core.domains.models.I_AD_WF_Activity;
import org.adempiere.core.domains.models.I_AD_Window;
import org.adempiere.core.domains.models.I_PA_DashboardContent;
import org.adempiere.core.domains.models.I_PA_Goal;
import org.adempiere.core.domains.models.I_R_Request;
import org.adempiere.core.domains.models.X_AD_TreeNodeMM;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.MDocumentStatus;
import org.compiere.model.MChart;
import org.compiere.model.MColorSchema;
import org.compiere.model.MDashboardContent;
import org.compiere.model.MGoal;
import org.compiere.model.MMeasure;
import org.compiere.model.MMenu;
import org.compiere.model.MRule;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.dashboarding.Action;
import org.spin.backend.grpc.dashboarding.ChartData;
import org.spin.backend.grpc.dashboarding.ChartSerie;
import org.spin.backend.grpc.dashboarding.Dashboard;
import org.spin.backend.grpc.dashboarding.DashboardingGrpc.DashboardingImplBase;
import org.spin.backend.grpc.dashboarding.ExistsWindowDashboardsRequest;
import org.spin.backend.grpc.dashboarding.ExistsWindowDashboardsResponse;
import org.spin.backend.grpc.dashboarding.Favorite;
import org.spin.backend.grpc.dashboarding.GetMetricsRequest;
import org.spin.backend.grpc.dashboarding.GetWindowMetricsRequest;
import org.spin.backend.grpc.dashboarding.ListDashboardsRequest;
import org.spin.backend.grpc.dashboarding.ListDashboardsResponse;
import org.spin.backend.grpc.dashboarding.ListFavoritesRequest;
import org.spin.backend.grpc.dashboarding.ListFavoritesResponse;
import org.spin.backend.grpc.dashboarding.ListNotificationsRequest;
import org.spin.backend.grpc.dashboarding.ListNotificationsResponse;
import org.spin.backend.grpc.dashboarding.ListPendingDocumentsRequest;
import org.spin.backend.grpc.dashboarding.ListPendingDocumentsResponse;
import org.spin.backend.grpc.dashboarding.ListWindowDashboardsRequest;
import org.spin.backend.grpc.dashboarding.ListWindowDashboardsResponse;
import org.spin.backend.grpc.dashboarding.Metrics;
import org.spin.backend.grpc.dashboarding.Notification;
import org.spin.backend.grpc.dashboarding.PendingDocument;
import org.spin.backend.grpc.dashboarding.WindowDashboard;
import org.spin.backend.grpc.dashboarding.WindowDashboardParameter;
import org.spin.backend.grpc.dashboarding.WindowMetrics;
import org.spin.base.db.LimitUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.dashboarding.DashboardingConvertUtil;
import org.spin.eca50.controller.ChartBuilder;
import org.spin.eca50.data.ChartValue;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Value;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * https://itnext.io/customizing-grpc-generated-code-5909a2551ca1
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Business data service
 */
public class Dashboarding extends DashboardingImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(Dashboarding.class);



	@Override
	public void getMetrics(GetMetricsRequest request, StreamObserver<Metrics> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Metrics.Builder chart = getMetrics(request);
			responseObserver.onNext(chart.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * Convert chart and data
	 * @param request
	 * @return
	 */
	private Metrics.Builder getMetrics(GetMetricsRequest request) {
		Metrics.Builder builder = Metrics.newBuilder();
		MGoal goal = (MGoal) RecordUtil.getEntity(Env.getCtx(), I_PA_Goal.Table_Name, request.getId(), null);
		if(goal == null) {
			throw new AdempiereException("@PA_Goal_ID@ @NotFound@");
		}
		//	Load
		Map<String, List<ChartData>> chartSeries = new HashMap<String, List<ChartData>>();
		if(goal.get_ValueAsInt("AD_Chart_ID") > 0) {
			ChartValue chartData = ChartBuilder.getChartData(goal.get_ValueAsInt("AD_Chart_ID"), null);
			if(chartData.getSeries().size() > 0) {
				chartData.getSeries().forEach(serie -> {
					List<ChartData> serieStub = new ArrayList<ChartData>();
					serie.getDataSet().forEach(dataSet -> {
						ChartData.Builder chartDataBuilder = ChartData.newBuilder()
								.setName(dataSet.getName())
								.setValue(
									NumberManager.getBigDecimalToString(
										dataSet.getAmount()
									)
								)
							;
						serieStub.add(chartDataBuilder.build());
					});
					chartSeries.put(serie.getName(), serieStub);
				});
			}
		} else {
			//	Set values
			builder.setName(
					ValueManager.validateNull(goal.getName())
				)
				.setDescription(
					ValueManager.validateNull(goal.getDescription())
				)
				.setId(goal.getPA_Goal_ID())
				.setXAxisLabel(
					ValueManager.validateNull(goal.getXAxisText())
				)
				.setYAxisLabel(
					ValueManager.validateNull(goal.getName())
				)
			;

			MMeasure measure = goal.getMeasure();
			List<GraphColumn> chartData = measure.getGraphColumnList(goal);
			chartData.forEach(data -> {
				String key = "";
				if (data.getDate() != null) {
					Calendar cal = Calendar.getInstance();
					cal.setTime(data.getDate());
					key = Integer.toString(cal.get(Calendar.YEAR));
				}
				//	Get from map
				List<ChartData> serie = chartSeries.get(key);
				if (serie == null) {
					serie = new ArrayList<ChartData>();
				}
				//	Add
				serie.add(
					DashboardingConvertUtil.convertChartData(data).build()
				);
				chartSeries.put(key, serie);
			});
		}

		builder.setMeasureTarget(
				NumberManager.getBigDecimalToString(
					goal.getMeasureTarget()
				)
			)
			.setMeasureActual(
				NumberManager.getBigDecimalToString(
					goal.getMeasureActual()
				)
			)
			.setPerformanceGoal(
				NumberManager.getBigDecimalToString(
					goal.getGoalPerformance()
				)
			)
		;

		//	Add measure color
		MColorSchema colorSchema = goal.getColorSchema();
		builder.addAllColorSchemas(
			DashboardingConvertUtil.convertColorSchemasList(colorSchema)
		);

		//	Add all
		chartSeries.keySet().stream().sorted().forEach(serie -> {
			ChartSerie.Builder chartSerieBuilder = ChartSerie.newBuilder()
				.setName(serie)
				.addAllDataSet(chartSeries.get(serie))
			;
			builder.addSeries(chartSerieBuilder);
		});
		return builder;
	}



	@Override
	public void listPendingDocuments(ListPendingDocumentsRequest request, StreamObserver<ListPendingDocumentsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListPendingDocumentsResponse.Builder pendingDocumentsList = listPendingDocuments(request);
			responseObserver.onNext(pendingDocumentsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * Convert pending documents to gRPC
	 * @param request
	 * @return
	 */
	private ListPendingDocumentsResponse.Builder listPendingDocuments(ListPendingDocumentsRequest request) {
		ListPendingDocumentsResponse.Builder builder = ListPendingDocumentsResponse.newBuilder();

		Properties context = Env.getCtx();
		//	Get user
		int userId = Env.getAD_User_ID(context);
		//	Get role
		int roleId = Env.getAD_Role_ID(context);

		//	Get from document status
		Arrays.asList(MDocumentStatus.getDocumentStatusIndicators(context, userId, roleId)).forEach(documentStatus -> {
			PendingDocument.Builder pendingDocument = PendingDocument.newBuilder();
			pendingDocument.setDocumentName(
				ValueManager.validateNull(documentStatus.getName())
			);
			// for Reference
			if(documentStatus.getAD_Window_ID() > 0) {
				pendingDocument.setWindowId(documentStatus.getAD_Window_ID());
			} else if(documentStatus.getAD_Form_ID() > 0) {
				pendingDocument.setFormId(documentStatus.getAD_Form_ID());
			}
			//	Criteria
			MTable table = MTable.get(context, documentStatus.getAD_Table_ID());
			pendingDocument.setTableName(
				ValueManager.validateNull(
					table.getTableName()
				)
			);
			//	TODO: Add filter from SQL
//			pendingDocument
//					.setCriteria(Criteria.newBuilder()
//					.setTableName(ValueManager.validateNull(table.getTableName()))
//					.setWhereClause(ValueManager.validateNull(documentStatus.getWhereClause())));
			//	Set quantity
			pendingDocument.setRecordCount(MDocumentStatus.evaluate(documentStatus));
			//	TODO: Add description for interface
			builder.addPendingDocuments(pendingDocument);
		});
		//	Return
		return builder;
	}



	@Override
	public void listDashboards(ListDashboardsRequest request, StreamObserver<ListDashboardsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListDashboardsResponse.Builder dashboardsList = listDashboards(request);
			responseObserver.onNext(dashboardsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * Convert dashboards to gRPC
	 * @param request
	 * @return
	 */
	private ListDashboardsResponse.Builder listDashboards(ListDashboardsRequest request) {
		Properties context = Env.getCtx();

		//	Get role
		final int roleId = Env.getAD_Role_ID(context);

		ListDashboardsResponse.Builder builder = ListDashboardsResponse.newBuilder();
		int recordCount = 0;

		//	Get from Charts
		final String whereClauseChart = "((AD_User_ID IS NULL AND AD_Role_ID IS NULL)"
			+ " OR AD_Role_ID=?"	//	#1
			+ " OR EXISTS (SELECT 1 FROM AD_User_Roles ur "
			+ "WHERE ur.AD_User_ID=PA_Goal.AD_User_ID "
			+ "AND ur.AD_Role_ID = ? "	//	#2
			+ "AND ur.IsActive='Y')) "
		;
		Query queryCharts = new Query(
			context,
			I_PA_Goal.Table_Name,
			whereClauseChart,
			null
		)
			.setParameters(roleId, roleId)
			.setOnlyActiveRecords(true)
			.setClient_ID()
		;
		recordCount += queryCharts.count();
		queryCharts
			.setOrderBy(I_PA_Goal.COLUMNNAME_SeqNo)
			.<MGoal>list()
			.forEach(chartDefinition -> {
				Dashboard.Builder dashboardBuilder = Dashboard.newBuilder()
					.setId(chartDefinition.getPA_Goal_ID())
					.setName(
						ValueManager.validateNull(chartDefinition.getName())
					)
					.setDescription(
						ValueManager.validateNull(chartDefinition.getDescription())
					)
					.setDashboardType("chart")
					.setChartType(
						ValueManager.validateNull(chartDefinition.getChartType())
					)
					.setIsCollapsible(true)
					.setIsOpenByDefault(true)
				;
				//	Add to builder
				builder.addDashboards(dashboardBuilder);
			});

		//	Get from dashboard
		final String whereClauseDashboard = "EXISTS(SELECT 1 FROM AD_Dashboard_Access da WHERE "
			+ "da.PA_DashboardContent_ID = PA_DashboardContent.PA_DashboardContent_ID "
			+ "AND da.IsActive = 'Y' "
			+ "AND da.AD_Role_ID = ?)"
		;
		Query queryDashboard = new Query(
			context,
			I_PA_DashboardContent.Table_Name,
			whereClauseDashboard,
			null
		)
			.setParameters(roleId)
			.setOnlyActiveRecords(true)
		;
		recordCount += queryDashboard.count();
		queryDashboard
			.setOrderBy(
				I_PA_DashboardContent.COLUMNNAME_ColumnNo + ","
				+ I_PA_DashboardContent.COLUMNNAME_AD_Client_ID + "," 
				+ I_PA_DashboardContent.COLUMNNAME_Line)
			.<MDashboardContent>list()
			.forEach(dashboard -> {
				Dashboard.Builder dashboardBuilder = Dashboard.newBuilder()
					.setId(dashboard.getPA_DashboardContent_ID())
					.setName(ValueManager.validateNull(dashboard.getName()))
					.setDescription(ValueManager.validateNull(dashboard.getDescription()))
					.setHtml(ValueManager.validateNull(dashboard.getHTML()))
					.setColumnNo(dashboard.getColumnNo())
					.setLineNo(dashboard.getLine())
					.setIsEventRequired(dashboard.isEventRequired())
					.setIsCollapsible(dashboard.isCollapsible())
					.setIsOpenByDefault(dashboard.isOpenByDefault())
					.setDashboardType("dashboard")
				;
				//	For Window
				if(dashboard.getAD_Window_ID() != 0) {
					dashboardBuilder.setWindowId(dashboard.getAD_Window_ID());
				}
				//	For Smart Browser
				if(dashboard.getAD_Browse_ID() != 0) {
					dashboardBuilder.setBrowserId(dashboard.getAD_Browse_ID());
				}
				//	File Name
				String fileName = dashboard.getZulFilePath();
				if(!Util.isEmpty(fileName)) {
					int endIndex = fileName.lastIndexOf(".");
					int beginIndex = fileName.lastIndexOf("/");
					if(beginIndex == -1) {
						beginIndex = 0;
					} else {
						beginIndex++;
					}
					if(endIndex == -1) {
						endIndex = fileName.length();
					}
					//	Set
					dashboardBuilder.setFileName(
						ValueManager.validateNull(
							fileName.substring(beginIndex, endIndex))
						)
					;
				}
				builder.addDashboards(dashboardBuilder);
			});

		builder.setRecordCount(recordCount);

		//	Return
		return builder;
	}



	@Override
	public void listFavorites(ListFavoritesRequest request, StreamObserver<ListFavoritesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListFavoritesResponse.Builder favoritesList = listFavorites(request);
			responseObserver.onNext(favoritesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * Convert favorites to gRPC
	 * @param context
	 * @param request
	 * @return
	 */
	private ListFavoritesResponse.Builder listFavorites(ListFavoritesRequest request) {
		Properties context = Env.getCtx();
		//	Get user
		int userId = Env.getAD_User_ID(context);

		//	TODO: add tree criteria
		final String whereClause = "EXISTS(SELECT 1 "
			+ "FROM AD_TreeBar tb "
			+ "WHERE tb.AD_Tree_ID = AD_TreeNodeMM.AD_Tree_ID "
			+ "AND tb.Node_ID = AD_TreeNodeMM.Node_ID "
			+ "AND tb.AD_User_ID = ?)"
		;
		Query query = new Query(
			context,
			I_AD_TreeNodeMM.Table_Name,
			whereClause,
			null
		)
			.setParameters(userId)
			// .setClient_ID()
		;
		ListFavoritesResponse.Builder builder = ListFavoritesResponse.newBuilder();
		builder.setRecordCount(query.count());

		query
			.<X_AD_TreeNodeMM>list().forEach(treeNodeMenu -> {
				Favorite.Builder favorite = DashboardingConvertUtil.convertFavorite(treeNodeMenu);
				builder.addFavorites(favorite);
			});
		//	Return
		return builder;
	}



	@Override
	public void listNotifications(ListNotificationsRequest request, StreamObserver<ListNotificationsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListNotificationsResponse.Builder favoritesList = listNotifications(request);
			responseObserver.onNext(favoritesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	private ListNotificationsResponse.Builder listNotifications(ListNotificationsRequest request) {
		ListNotificationsResponse.Builder builderList = ListNotificationsResponse.newBuilder();

		builderList.addNotifications(getNotificationNotice());
		builderList.addNotifications(getIssueRequest());
		builderList.addNotifications(getNotificationWorkflowActivities());

		return builderList;
	}

	private Notification.Builder getNotificationNotice() {
		Notification.Builder builder = Notification.newBuilder();

		final String whereClause = "AD_Client_ID = ? "
			+ "AND AD_User_ID IN (0, ?) "
			+ "AND Processed = 'N'"
		;
		int clientId = Env.getAD_Client_ID(Env.getCtx());
		int userId = Env.getAD_User_ID(Env.getCtx());

		int quantity = new Query(
			Env.getCtx(),
			I_AD_Note.Table_Name,
			whereClause,
			null
		)
		.setParameters(clientId, userId)
		.count();
		builder.setQuantity(quantity);

		builder.setAction(Action.WINDOW);

		final String whereClauseMenu = "Name = 'Notice' AND IsSummary = 'N'";
		MMenu menu = new Query(
			Env.getCtx(),
			I_AD_Menu.Table_Name,
			whereClauseMenu,
			null
		).first();

		String name = menu.get_Translation(I_AD_Menu.COLUMNNAME_Name);
		if (Util.isEmpty(name, true)) {
			name = menu.getName();
		}
		builder.setName(
			ValueManager.validateNull(name)
		);
		String description = menu.get_Translation(I_AD_Menu.COLUMNNAME_Description);
		if (Util.isEmpty(description, true)) {
			description = menu.getDescription();
		}
		builder.setDescription(
			ValueManager.validateNull(description)
		);
		if (menu.getAction().equals(MMenu.ACTION_Window)) {
			if (menu.getAD_Window_ID() > 0) {
				builder.setActionId(menu.getAD_Window_ID());
			}
		}
		return builder;
	}

	private Notification.Builder getIssueRequest() {
		Notification.Builder builder = Notification.newBuilder();

		final String whereClause = "Processed='N' "
			+ "AND (SalesRep_ID=? OR AD_Role_ID = ?) "
			+ "AND (DateNextAction IS NULL "
			+ "OR TRUNC(DateNextAction, 'DD') <= TRUNC(SysDate, 'DD'))"
			+ "AND (R_Status_ID IS NULL "
			+ "OR R_Status_ID IN (SELECT R_Status_ID FROM R_Status WHERE IsClosed='N'))"
		;

		int userId = Env.getAD_User_ID(Env.getCtx());
		int roleId = Env.getAD_Role_ID(Env.getCtx());

		int quantity = new Query(
			Env.getCtx(),
			I_R_Request.Table_Name,
			whereClause,
			null
		)
		.setParameters(userId, roleId)
		.count();

		builder.setQuantity(quantity)
			.setAction(Action.WINDOW)
		;

		final String whereClauseMenu = "Name = 'Request' AND IsSummary = 'N'";
		MMenu menu = new Query(
			Env.getCtx(),
			I_AD_Menu.Table_Name,
			whereClauseMenu,
			null
		).first();

		String name = menu.get_Translation(I_AD_Menu.COLUMNNAME_Name);
		if (Util.isEmpty(name, true)) {
			name = menu.getName();
		}
		builder.setName(
			ValueManager.validateNull(name)
		);
		String description = menu.get_Translation(I_AD_Menu.COLUMNNAME_Description);
		if (Util.isEmpty(description, true)) {
			description = menu.getDescription();
		}
		builder.setDescription(
			ValueManager.validateNull(description)
		);
		if (menu.getAction().equals(MMenu.ACTION_Window)) {
			if (menu.getAD_Window_ID() > 0) {
				builder.setActionId(menu.getAD_Window_ID());
			}
		}

		return builder;
	}

	private Notification.Builder getNotificationWorkflowActivities() {
		Notification.Builder builder = Notification.newBuilder();

		final String whereClause = "Processed='N' AND WFState='OS' AND ("
			// Owner of Activity
			+ " AD_User_ID = ? " // #1
			// Invoker (if no invoker = all)
			+ "OR EXISTS ( "
			+ "SELECT * FROM AD_WF_Responsible r "
			+ "WHERE AD_WF_Activity.AD_WF_Responsible_ID = r.AD_WF_Responsible_ID "
			+ "AND COALESCE(r.AD_User_ID,0) = 0 "
			+ "AND COALESCE(r.AD_Role_ID,0) = 0 "
			+ "AND (AD_WF_Activity.AD_User_ID = ? OR AD_WF_Activity.AD_User_ID IS NULL) " // #2
			+ ") "
			// Responsible User
			+ "OR EXISTS ( "
			+ "SELECT * FROM AD_WF_Responsible r "
			+ "WHERE r.AD_User_ID = ? " // #3
			+ "AND AD_WF_Activity.AD_WF_Responsible_ID = r.AD_WF_Responsible_ID "
			+ ") "
			// Responsible Role
			+ "OR EXISTS ( "
			+ "SELECT * FROM AD_WF_Responsible r "
			+ "INNER JOIN AD_User_Roles ur ON (r.AD_Role_ID = ur.AD_Role_ID) "
			+ "WHERE ur.AD_User_ID = ? " // #4
			+ "AND AD_WF_Activity.AD_WF_Responsible_ID = r.AD_WF_Responsible_ID) "
			+ ")"
		;
		int userId = Env.getAD_User_ID(Env.getCtx());

		int quantity = new Query(
			Env.getCtx(),
			I_AD_WF_Activity.Table_Name,
			whereClause,
			null
		)
		.setParameters(userId, userId, userId, userId)
		.count();
		builder.setQuantity(quantity);

		builder.setAction(Action.FORM);

		final String whereClauseMenu = "Name = 'Workflow Activities' AND IsSummary = 'N'";
		MMenu menu = new Query(
			Env.getCtx(),
			I_AD_Menu.Table_Name,
			whereClauseMenu,
			null
		).first();

		String name = menu.get_Translation(I_AD_Menu.COLUMNNAME_Name);
		if (Util.isEmpty(name, true)) {
			name = menu.getName();
		}
		builder.setName(
			ValueManager.validateNull(name)
		);
		String description = menu.get_Translation(I_AD_Menu.COLUMNNAME_Description);
		if (Util.isEmpty(description, true)) {
			description = menu.getDescription();
		}
		builder.setDescription(
			ValueManager.validateNull(description)
		);
		if (menu.getAction().equals(MMenu.ACTION_Form)) {
			if (menu.getAD_Form_ID() > 0) {
				builder.setActionId(menu.getAD_Form_ID());
			}
		}
		return builder;
	}



	private final String whereClauseWindowChart = "AD_Window_ID = ? "	//	#1
		+ "AND (COALESCE(AD_Tab_ID, 0) = ? "	//	#2
		+ "OR COALESCE(AD_Tab_ID, 0) = 0) "	//	#3
		// validate access
		+ "AND EXISTS(SELECT 1 FROM ECA50_WindowChartAccess wca "
		+ "WHERE wca.AD_Chart_ID=ECA50_WindowChart.AD_Chart_ID "
		+ "AND wca.AD_Role_ID = ? "	//	#4
		+ "AND wca.IsActive='Y')"
	;

	@Override
	public void existsWindowDashboards(ExistsWindowDashboardsRequest request, StreamObserver<ExistsWindowDashboardsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ExistsWindowDashboardsResponse.Builder resourceReference = existsWindowDashboards(request);
			responseObserver.onNext(resourceReference.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	private ExistsWindowDashboardsResponse.Builder existsWindowDashboards(ExistsWindowDashboardsRequest request) {
		// validate window
		if (request.getWindowId() <= 0) {
			throw new AdempiereException("@AD_Window_ID@ @NotFound@");
		}
		MWindow window = (MWindow) RecordUtil.getEntity(Env.getCtx(), I_AD_Window.Table_Name, request.getWindowId(), null);
		if (window == null || window.getAD_Window_ID() <= 0) {
			throw new AdempiereException("@AD_Window_ID@ @NotFound@");
		}

		// validate tab
		int tabId = request.getTabId();
		// Get role
		int roleId = Env.getAD_Role_ID(Env.getCtx());

		//	Get from Charts
		int recordCount = new Query(
			Env.getCtx(),
			"ECA50_WindowChart",
			this.whereClauseWindowChart,
			null
		)
			.setParameters(window.getAD_Window_ID(), tabId, roleId)
			.setOnlyActiveRecords(true)
			.count()
		;

		return ExistsWindowDashboardsResponse.newBuilder()
			.setRecordCount(recordCount)
		;
	}



	@Override
	public void listWindowDashboards(ListWindowDashboardsRequest request, StreamObserver<ListWindowDashboardsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			
			ListWindowDashboardsResponse.Builder chartsList = listWindowDashboards(request);
			responseObserver.onNext(chartsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	ListWindowDashboardsResponse.Builder listWindowDashboards(ListWindowDashboardsRequest request) {
		Properties context = Env.getCtx();
		// validate window
		if (request.getWindowId() <= 0) {
			throw new AdempiereException("@AD_Window_ID@ @NotFound@");
		}
		MWindow window = (MWindow) RecordUtil.getEntity(context, I_AD_Window.Table_Name, request.getWindowId(), null);
		if (window == null || window.getAD_Window_ID() <= 0) {
			throw new AdempiereException("@AD_Window_ID@ @NotFound@");
		}

		// validate tab
		int tabId = request.getTabId();
		// Get role
		int roleId = Env.getAD_Role_ID(context);

		//	Get from Charts
		Query query = new Query(
			context,
			"ECA50_WindowChart",
			this.whereClauseWindowChart,
			null
		)
			.setParameters(window.getAD_Window_ID(), tabId, roleId)
			.setOnlyActiveRecords(true)
		;

		int recordCount = query.count();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListWindowDashboardsResponse.Builder builderList = ListWindowDashboardsResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				ValueManager.validateNull(nexPageToken)
			)
		;

		query
			.setOrderBy(I_PA_Goal.COLUMNNAME_SeqNo)
			.<PO>list()
			.forEach(windowChartAllocation -> {
				MChart chartDefinition = new MChart(
					context,
					windowChartAllocation.get_ValueAsInt(I_AD_Chart.COLUMNNAME_AD_Chart_ID),
					null
				);
				WindowDashboard.Builder chartBuilder = DashboardingConvertUtil.convertWindowDashboard(chartDefinition);
				chartBuilder.setId(windowChartAllocation.get_ID())
					.setSequence(
						windowChartAllocation.get_ValueAsInt(I_PA_Goal.COLUMNNAME_SeqNo)
					)
				;

				List<String> contextColumnsList = DashboardingConvertUtil.getContextColumnsByWindowChart(windowChartAllocation.get_ID());
				chartBuilder.addAllContextColumnNames(contextColumnsList);

				int ruleId = windowChartAllocation.get_ValueAsInt(I_AD_Rule.COLUMNNAME_AD_Rule_ID);
				if (ruleId > 0) {
					MRule rule = MRule.get(Env.getCtx(), ruleId);
					chartBuilder.setTransformationScript(
						ValueManager.validateNull(rule.getScript())
					);
				}

				new Query(
						context,
						"ECA50_WindowChartParameter",
						"ECA50_WindowChart_ID = ? AND ECA50_IsEnableSelection = 'Y' ",
						null
					)
					.setParameters(windowChartAllocation.get_ValueAsInt("ECA50_WindowChart_ID"))
					.setOnlyActiveRecords(true)
					.list()
					.forEach(windowChartParameter -> {
						WindowDashboardParameter.Builder windowChartParameterBuilder = DashboardingConvertUtil.convertWindowDashboardParameter(
							windowChartParameter
						);
						chartBuilder.addParameters(windowChartParameterBuilder);
					});
				;

				//	Add to builder
				builderList.addRecords(chartBuilder);
			});

		return builderList;
	}



	@Override
	public void getWindowMetrics(GetWindowMetricsRequest request, StreamObserver<WindowMetrics> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			WindowMetrics.Builder chart = getWindowMetrics(request);
			responseObserver.onNext(chart.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	WindowMetrics.Builder getWindowMetrics(GetWindowMetricsRequest request) {
		// fill context
		Properties context = Env.getCtx();
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		Map<String, Value> attributes = request.getContextAttributes().getFieldsMap();
		ContextManager.setContextWithAttributesFromStruct(windowNo, context, request.getContextAttributes());

		PO windowChart = RecordUtil.getEntity(context, "ECA50_WindowChart", request.getId(), null);
		if (windowChart == null || windowChart.get_ID() <= 0) {
			throw new AdempiereException("@ECA50_WindowChart_ID@ @NotFound@");
		}
		MChart chart = new MChart(context, windowChart.get_ValueAsInt(I_AD_Chart.COLUMNNAME_AD_Chart_ID), null);
		if (chart == null || chart.getAD_Chart_ID() <= 0) {
			throw new AdempiereException("@AD_Chart_ID@ @NotFound@");
		}

		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

		// validate record
		int recordId = request.getRecordId();
		if (recordId <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		Env.setContext(context, windowNo, I_AD_ChangeLog.COLUMNNAME_Record_ID, recordId);
		Env.setContext(context, windowNo, I_AD_Table.COLUMNNAME_TableName, table.getTableName());

		Map<String, Object> filtersList = new HashMap<String, Object>();
		attributes.entrySet().forEach(entry -> {
			PO chartParameter = new Query(
					context,
					"ECA50_WindowChartParameter",
					"ColumnName = ? AND ECA50_WindowChart_ID = ?",
					null
				)
				.setParameters(entry.getKey(), windowChart.get_ID())
				.first()
			;
			if (chartParameter != null) {
				filtersList.put(
					chartParameter.get_ValueAsString(I_AD_Column.COLUMNNAME_ColumnSQL),
					ValueManager.getObjectFromValue(entry.getValue())
				);
			}
		});
//	TODO: Add support to this
//		// prameters as client filters
//		Map<String, Object> clientFiltersList = DashboardingConvertUtil.convertFilterValuesToObjects(
//			request.getFiltersList()
//		);
//		clientFiltersList.entrySet().stream()
//			.forEach(entry -> {
//				PO chartParameter = new Query(
//						context,
//						"ECA50_WindowChartParameter",
//						"ColumnName = ? AND ECA50_WindowChart_ID = ?",
//						null
//					)
//					.setParameters(entry.getKey(), windowChart.get_ID())
//					.first()
//				;
//				if (chartParameter != null) {
//					Object value = entry.getValue();
//					filtersList.put(
//						chartParameter.get_ValueAsString(I_AD_Column.COLUMNNAME_ColumnSQL),
//						value
//					);
//				}
//			});

		//	Load
		Map<String, List<ChartData>> chartSeries = new HashMap<String, List<ChartData>>();

		ChartValue chartData = ChartBuilder.getChartData(chart.getAD_Chart_ID(), filtersList);
		if (chartData.getSeries().size() > 0) {
			chartData.getSeries().forEach(serie -> {
				List<ChartData> serieStub = new ArrayList<ChartData>();
				serie.getDataSet().forEach(dataSet -> {
					ChartData.Builder chartDataBuilder = ChartData.newBuilder()
						.setName(dataSet.getName())
						.setValue(
							NumberManager.getBigDecimalToString(
								dataSet.getAmount()
							)
						)
					;
					serieStub.add(chartDataBuilder.build());
				});
				chartSeries.put(serie.getName(), serieStub);
			});
		}

		//	Add all
		WindowMetrics.Builder builder = WindowMetrics.newBuilder()
			.setId(
				windowChart.get_ID()
			)
		;
		chartSeries.keySet().stream().sorted().forEach(serieKey -> {
			ChartSerie.Builder chartSerieBuilder = ChartSerie.newBuilder()
				.setName(serieKey)
				.addAllDataSet(
					chartSeries.get(serieKey)
				)
			;
			builder.addSeries(chartSerieBuilder);
		});

		return builder;
	}

}
