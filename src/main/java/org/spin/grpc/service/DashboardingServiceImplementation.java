/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program.	If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.grpc.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.apps.graph.GraphColumn;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.MBrowse;
import org.adempiere.model.MDocumentStatus;
import org.adempiere.core.domains.models.I_AD_Chart;
import org.adempiere.core.domains.models.I_AD_Menu;
import org.adempiere.core.domains.models.I_AD_Note;
import org.adempiere.core.domains.models.I_AD_Role;
import org.adempiere.core.domains.models.I_AD_Rule;
import org.adempiere.core.domains.models.I_AD_Tab;
import org.adempiere.core.domains.models.I_AD_TreeNodeMM;
import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.core.domains.models.I_AD_WF_Activity;
import org.adempiere.core.domains.models.I_AD_Window;
import org.adempiere.core.domains.models.I_PA_DashboardContent;
import org.adempiere.core.domains.models.I_PA_Goal;
import org.adempiere.core.domains.models.I_R_Request;
import org.compiere.model.MChart;
import org.compiere.model.MColorSchema;
import org.compiere.model.MDashboardContent;
import org.compiere.model.MForm;
import org.compiere.model.MGoal;
import org.compiere.model.MMeasure;
import org.compiere.model.MMenu;
import org.compiere.model.MProcess;
import org.compiere.model.MRule;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.adempiere.core.domains.models.X_AD_TreeNodeMM;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.jfree.data.category.CategoryDataset;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;
import org.spin.backend.grpc.dashboarding.Action;
import org.spin.backend.grpc.dashboarding.Chart;
import org.spin.backend.grpc.dashboarding.ChartData;
import org.spin.backend.grpc.dashboarding.ChartSerie;
import org.spin.backend.grpc.common.Criteria;
import org.spin.backend.grpc.dashboarding.Dashboard;
import org.spin.backend.grpc.dashboarding.ExistsWindowChartsRequest;
import org.spin.backend.grpc.dashboarding.ExistsWindowChartsResponse;
import org.spin.backend.grpc.dashboarding.DashboardingGrpc.DashboardingImplBase;
import org.spin.backend.grpc.dashboarding.Favorite;
import org.spin.backend.grpc.dashboarding.GetChartRequest;
import org.spin.backend.grpc.dashboarding.GetWindowMetricsRequest;
import org.spin.backend.grpc.dashboarding.ListDashboardsRequest;
import org.spin.backend.grpc.dashboarding.ListDashboardsResponse;
import org.spin.backend.grpc.dashboarding.ListFavoritesRequest;
import org.spin.backend.grpc.dashboarding.ListFavoritesResponse;
import org.spin.backend.grpc.dashboarding.ListNotificationsRequest;
import org.spin.backend.grpc.dashboarding.ListNotificationsResponse;
import org.spin.backend.grpc.dashboarding.ListPendingDocumentsRequest;
import org.spin.backend.grpc.dashboarding.ListPendingDocumentsResponse;
import org.spin.backend.grpc.dashboarding.ListWindowChartsRequest;
import org.spin.backend.grpc.dashboarding.ListWindowChartsResponse;
import org.spin.backend.grpc.dashboarding.Notification;
import org.spin.backend.grpc.dashboarding.PendingDocument;
import org.spin.backend.grpc.dashboarding.WindowChart;
import org.spin.backend.grpc.dashboarding.WindowMetrics;
import org.spin.dashboarding.DashboardingConvertUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * https://itnext.io/customizing-grpc-generated-code-5909a2551ca1
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Business data service
 */
public class DashboardingServiceImplementation extends DashboardingImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(DashboardingServiceImplementation.class);
	@Override
	public void listPendingDocuments(ListPendingDocumentsRequest request, StreamObserver<ListPendingDocumentsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListPendingDocumentsResponse.Builder pendingDocumentsList = convertPendingDocumentList(Env.getCtx(), request);
			responseObserver.onNext(pendingDocumentsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void listFavorites(ListFavoritesRequest request, StreamObserver<ListFavoritesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListFavoritesResponse.Builder favoritesList = convertFavoritesList(Env.getCtx(), request);
			responseObserver.onNext(favoritesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}



	@Override
	public void getChart(GetChartRequest request, StreamObserver<Chart> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			
			Chart.Builder chart = convertChart(request);
			responseObserver.onNext(chart.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	/**
	 * Convert chart and data
	 * @param request
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Chart.Builder convertChart(GetChartRequest request) {
		Chart.Builder builder = Chart.newBuilder();
		MGoal goal = (MGoal) RecordUtil.getEntity(Env.getCtx(), I_PA_Goal.Table_Name, request.getUuid(), request.getId(), null);
		if(goal == null) {
			throw new AdempiereException("@PA_Goal_ID@ @NotFound@");
		}
		//	Load
		Map<String, List<ChartData>> chartSeries = new HashMap<String, List<ChartData>>();
		if(goal.get_ValueAsInt("AD_Chart_ID") > 0) {
			MChart chart = new MChart(Env.getCtx(), goal.get_ValueAsInt("AD_Chart_ID"), null);
			CategoryDataset dataSet = chart.getCategoryDataset();

			dataSet.getColumnKeys().forEach(column -> {
				dataSet.getRowKeys().forEach(row -> {
					//	Get from map
					List<ChartData> serie = chartSeries.get(row);
					if (serie == null) {
						serie = new ArrayList<ChartData>();
					}
					//	Add
					Number value = dataSet.getValue((Comparable<?>)row, (Comparable<?>)column);
					BigDecimal numberValue = (value != null? new BigDecimal(value.doubleValue()): Env.ZERO);
					ChartData.Builder chartDataBuilder = ChartData.newBuilder()
						.setName(column.toString())
						.setValue(
							ValueUtil.getDecimalFromBigDecimal(numberValue)
						)
					;
					serie.add(
						chartDataBuilder.build()
					);
					chartSeries.put(row.toString(), serie);
				});
			});
		} else {
			//	Set values
			builder.setName(ValueUtil.validateNull(goal.getName()));
			builder.setDescription(ValueUtil.validateNull(goal.getDescription()));
			builder.setId(goal.getPA_Goal_ID());
			builder.setUuid(ValueUtil.validateNull(goal.getUUID()));
			builder.setXAxisLabel(ValueUtil.validateNull(goal.getXAxisText()));
			builder.setYAxisLabel(ValueUtil.validateNull(goal.getName()));

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
			ValueUtil.getDecimalFromBigDecimal(goal.getMeasureTarget())
		);

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


	/**
	 * Convert pending documents to gRPC
	 * @param context
	 * @param request
	 * @return
	 */
	private ListPendingDocumentsResponse.Builder convertPendingDocumentList(Properties context, ListPendingDocumentsRequest request) {
		ListPendingDocumentsResponse.Builder builder = ListPendingDocumentsResponse.newBuilder();
		//	Get entity
		if(request.getUserId() <= 0
				&& Util.isEmpty(request.getUserUuid())
				&& request.getRoleId() <= 0
				&& Util.isEmpty(request.getRoleUuid())) {
			throw new AdempiereException("@AD_User_ID@ / @AD_Role_ID@ @NotFound@");
		}
		//	Get user
		int userId = request.getUserId();
		if(userId <= 0) {
			userId = RecordUtil.getIdFromUuid(I_AD_User.Table_Name, request.getUserUuid(), null);
		}
		//	Get role
		int roleId = request.getRoleId();
		if(roleId <= 0) {
			roleId = RecordUtil.getIdFromUuid(I_AD_Role.Table_Name, request.getRoleUuid(), null);
		}
		//	Get from document status
		Arrays.asList(MDocumentStatus.getDocumentStatusIndicators(context, userId, roleId)).forEach(documentStatus -> {
			PendingDocument.Builder pendingDocument = PendingDocument.newBuilder();
			pendingDocument.setDocumentName(ValueUtil.validateNull(documentStatus.getName()));
			// for Reference
			if(documentStatus.getAD_Window_ID() != 0) {
				MWindow window = MWindow.get(context, documentStatus.getAD_Window_ID());
				pendingDocument.setWindowUuid(ValueUtil.validateNull(window.getUUID()));
			} else if(documentStatus.getAD_Form_ID() != 0) {
				MForm form = new MForm(context, documentStatus.getAD_Form_ID(), null);
				pendingDocument.setFormUuid(ValueUtil.validateNull(form.getUUID()));
			}
			//	Criteria
			MTable table = MTable.get(context, documentStatus.getAD_Table_ID());
			pendingDocument.setCriteria(Criteria.newBuilder()
					.setTableName(ValueUtil.validateNull(table.getTableName()))
					.setWhereClause(ValueUtil.validateNull(documentStatus.getWhereClause())));
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
			responseObserver.onError(Status.INTERNAL
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
		//	Get entity
		if(request.getRoleId() <= 0
				&& Util.isEmpty(request.getRoleUuid())) {
			throw new AdempiereException("@AD_Role_ID@ @NotFound@");
		}

		//	Get role
		int roleId = request.getRoleId();
		if(roleId <= 0) {
			roleId = RecordUtil.getIdFromUuid(I_AD_Role.Table_Name, request.getRoleUuid(), null);
		}

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
				Dashboard.Builder dashboardBuilder = Dashboard.newBuilder();
				dashboardBuilder.setId(chartDefinition.getPA_Goal_ID());
				dashboardBuilder.setUuid(ValueUtil.validateNull(chartDefinition.getUUID()));
				dashboardBuilder.setName(ValueUtil.validateNull(chartDefinition.getName()));
				dashboardBuilder.setDescription(ValueUtil.validateNull(chartDefinition.getDescription()));
				dashboardBuilder.setDashboardType("chart");
				dashboardBuilder.setChartType(ValueUtil.validateNull(chartDefinition.getChartType()));
				dashboardBuilder.setIsCollapsible(true);
				dashboardBuilder.setIsOpenByDefault(true);
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
				Dashboard.Builder dashboardBuilder = Dashboard.newBuilder();
				dashboardBuilder.setId(dashboard.getPA_DashboardContent_ID());
				dashboardBuilder.setUuid(ValueUtil.validateNull(dashboard.getUUID()));
				dashboardBuilder.setName(ValueUtil.validateNull(dashboard.getName()));
				dashboardBuilder.setDescription(ValueUtil.validateNull(dashboard.getDescription()));
				dashboardBuilder.setHtml(ValueUtil.validateNull(dashboard.getHTML()));
				dashboardBuilder.setColumnNo(dashboard.getColumnNo());
				dashboardBuilder.setLineNo(dashboard.getLine());
				dashboardBuilder.setIsEventRequired(dashboard.isEventRequired());
				dashboardBuilder.setIsCollapsible(dashboard.isCollapsible());
				dashboardBuilder.setIsOpenByDefault(dashboard.isOpenByDefault());
				dashboardBuilder.setDashboardType("dashboard");
				//	For Window
				if(dashboard.getAD_Window_ID() != 0) {
					MWindow window = MWindow.get(context, dashboard.getAD_Window_ID());
					dashboardBuilder.setWindowUuid(ValueUtil.validateNull(window.getUUID()));
				}
				//	For Smart Browser
				if(dashboard.getAD_Browse_ID() != 0) {
					MBrowse browser = MBrowse.get(context, dashboard.getAD_Browse_ID());
					dashboardBuilder.setWindowUuid(ValueUtil.validateNull(browser.getUUID()));
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
					dashboardBuilder.setFileName(ValueUtil.validateNull(fileName.substring(beginIndex, endIndex)));
				}
				builder.addDashboards(dashboardBuilder);
			});

		builder.setRecordCount(recordCount);

		//	Return
		return builder;
	}
	
	/**
	 * Convert favorites to gRPC
	 * @param context
	 * @param request
	 * @return
	 */
	private ListFavoritesResponse.Builder convertFavoritesList(Properties context, ListFavoritesRequest request) {
		ListFavoritesResponse.Builder builder = ListFavoritesResponse.newBuilder();
		//	Get entity
		if(request.getUserId() <= 0
				&& Util.isEmpty(request.getUserUuid())) {
			throw new AdempiereException("@AD_User_ID@ @NotFound@");
		}
		//	Get user
		int userId = request.getUserId();
		if(userId <= 0) {
			userId = RecordUtil.getIdFromUuid(I_AD_Role.Table_Name, request.getUserUuid(), null);
		}
		//	TODO: add tree criteria
		new Query(context, I_AD_TreeNodeMM.Table_Name, "EXISTS(SELECT 1 "
				+ "FROM AD_TreeBar tb "
				+ "WHERE tb.AD_Tree_ID = AD_TreeNodeMM.AD_Tree_ID "
				+ "AND tb.Node_ID = AD_TreeNodeMM.Node_ID "
				+ "AND tb.AD_User_ID = ?)", null)
			.setParameters(userId)
			.setClient_ID()
			.<X_AD_TreeNodeMM>list().forEach(treeNodeMenu -> {
				Favorite.Builder favorite = Favorite.newBuilder();
				String menuName = "";
				String menuDescription = "";
				MMenu menu = MMenu.getFromId(context, treeNodeMenu.getNode_ID());
				favorite.setMenuUuid(ValueUtil.validateNull(menu.getUUID()));
				String action = MMenu.ACTION_Window;
				if(!menu.isCentrallyMaintained()) {
					menuName = menu.getName();
					menuDescription = menu.getDescription();
					if(!Env.isBaseLanguage(context, "")) {
						String translation = menu.get_Translation("Name");
						if(!Util.isEmpty(translation)) {
							menuName = translation;
						}
						translation = menu.get_Translation("Description");
						if(!Util.isEmpty(translation)) {
							menuDescription = translation;
						}
					}
				}
				//	Supported actions
				if(!Util.isEmpty(menu.getAction())) {
					action = menu.getAction();
					String referenceUuid = null;
					if(menu.getAction().equals(MMenu.ACTION_Form)) {
						if(menu.getAD_Form_ID() > 0) {
							MForm form = new MForm(context, menu.getAD_Form_ID(), null);
							referenceUuid = form.getUUID();
							if(menu.isCentrallyMaintained()) {
								menuName = form.getName();
								menuDescription = form.getDescription();
								if(!Env.isBaseLanguage(context, "")) {
									String translation = form.get_Translation("Name");
									if(!Util.isEmpty(translation)) {
										menuName = translation;
									}
									translation = form.get_Translation("Description");
									if(!Util.isEmpty(translation)) {
										menuDescription = translation;
									}
								}
							}
						}
					} else if(menu.getAction().equals(MMenu.ACTION_Window)) {
						if(menu.getAD_Window_ID() > 0) {
							MWindow window = new MWindow(context, menu.getAD_Window_ID(), null);
							referenceUuid = window.getUUID();
							if(menu.isCentrallyMaintained()) {
								menuName = window.getName();
								menuDescription = window.getDescription();
								if(!Env.isBaseLanguage(context, "")) {
									String translation = window.get_Translation("Name");
									if(!Util.isEmpty(translation)) {
										menuName = translation;
									}
									translation = window.get_Translation("Description");
									if(!Util.isEmpty(translation)) {
										menuDescription = translation;
									}
								}
							}
						}
					} else if(menu.getAction().equals(MMenu.ACTION_Process)
						|| menu.getAction().equals(MMenu.ACTION_Report)) {
						if(menu.getAD_Process_ID() > 0) {
							MProcess process = MProcess.get(context, menu.getAD_Process_ID());
							referenceUuid = process.getUUID();
							if(menu.isCentrallyMaintained()) {
								menuName = process.getName();
								menuDescription = process.getDescription();
								if(!Env.isBaseLanguage(context, "")) {
									String translation = process.get_Translation("Name");
									if(!Util.isEmpty(translation)) {
										menuName = translation;
									}
									translation = process.get_Translation("Description");
									if(!Util.isEmpty(translation)) {
										menuDescription = translation;
									}
								}
							}
						}
					} else if(menu.getAction().equals(MMenu.ACTION_SmartBrowse)) {
						if(menu.getAD_Browse_ID() > 0) {
							MBrowse smartBrowser = MBrowse.get(context, menu.getAD_Browse_ID());
							referenceUuid = smartBrowser.getUUID();
							if(menu.isCentrallyMaintained()) {
								menuName = smartBrowser.getName();
								menuDescription = smartBrowser.getDescription();
								if(!Env.isBaseLanguage(context, "")) {
									String translation = smartBrowser.get_Translation("Name");
									if(!Util.isEmpty(translation)) {
										menuName = translation;
									}
									translation = smartBrowser.get_Translation("Description");
									if(!Util.isEmpty(translation)) {
										menuDescription = translation;
									}
								}
							}
						}
					}
					favorite.setReferenceUuid(ValueUtil.validateNull(referenceUuid));
					favorite.setAction(ValueUtil.validateNull(action));
				}
				//	Set name and description
				favorite.setMenuName(ValueUtil.validateNull(menuName));
				favorite.setMenuDescription(ValueUtil.validateNull(menuDescription));
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
		builderList.addNotifications(getNoticeRequest());
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
			ValueUtil.validateNull(name)
		);
		String description = menu.get_Translation(I_AD_Menu.COLUMNNAME_Description);
		if (Util.isEmpty(description, true)) {
			description = menu.getDescription();
		}
		builder.setDescription(
			ValueUtil.validateNull(description)
		);

		String actionUuid = "";
		if (menu.getAction().equals(MMenu.ACTION_Window)) {
			if (menu.getAD_Window_ID() > 0) {
				MWindow window = new MWindow(Env.getCtx(), menu.getAD_Window_ID(), null);
				actionUuid = window.getUUID();
			}
		}
		builder.setActionUuid(
			ValueUtil.validateNull(actionUuid)
		);

		return builder;
	}

	private Notification.Builder getNoticeRequest() {
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
		builder.setQuantity(quantity);

		builder.setAction(Action.WINDOW);

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
			ValueUtil.validateNull(name)
		);
		String description = menu.get_Translation(I_AD_Menu.COLUMNNAME_Description);
		if (Util.isEmpty(description, true)) {
			description = menu.getDescription();
		}
		builder.setDescription(
			ValueUtil.validateNull(description)
		);

		String actionUuid = "";
		if (menu.getAction().equals(MMenu.ACTION_Window)) {
			if (menu.getAD_Window_ID() > 0) {
				MWindow window = new MWindow(Env.getCtx(), menu.getAD_Window_ID(), null);
				actionUuid = window.getUUID();
			}
		}
		builder.setActionUuid(
			ValueUtil.validateNull(actionUuid)
		);

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
			ValueUtil.validateNull(name)
		);
		String description = menu.get_Translation(I_AD_Menu.COLUMNNAME_Description);
		if (Util.isEmpty(description, true)) {
			description = menu.getDescription();
		}
		builder.setDescription(
			ValueUtil.validateNull(description)
		);

		String actionUuid = "";
		if (menu.getAction().equals(MMenu.ACTION_Form)) {
			if (menu.getAD_Form_ID() > 0) {
				MForm form = new MForm(Env.getCtx(), menu.getAD_Form_ID(), null);
				actionUuid = form.getUUID();
			}
		}
		builder.setActionUuid(
			ValueUtil.validateNull(actionUuid)
		);

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
	public void existsWindowCharts(ExistsWindowChartsRequest request, StreamObserver<ExistsWindowChartsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ExistsWindowChartsResponse.Builder resourceReference = existsWindowCharts(request);
			responseObserver.onNext(resourceReference.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ExistsWindowChartsResponse.Builder existsWindowCharts(ExistsWindowChartsRequest request) {
		// validate window
		if (request.getWindowId() <= 0 && Util.isEmpty(request.getWindowUuid(), true)) {
			throw new AdempiereException("@AD_Window_ID@ @NotFound@");
		}
		MWindow window = (MWindow) RecordUtil.getEntity(Env.getCtx(), I_AD_Window.Table_Name, request.getWindowUuid(), request.getWindowId(), null);
		if (window == null || window.getAD_Window_ID() <= 0) {
			throw new AdempiereException("@AD_Window_ID@ @NotFound@");
		}

		// validate tab
		int tabId = request.getTabId();
		if (tabId <= 0 && !Util.isEmpty(request.getTabUuid(), true)) {
			tabId = RecordUtil.getIdFromUuid(I_AD_Tab.Table_Name, request.getTabUuid(), null);
		}

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

		return ExistsWindowChartsResponse.newBuilder()
			.setRecordCount(recordCount)
		;
	}


	@Override
	public void listWindowCharts(ListWindowChartsRequest request, StreamObserver<ListWindowChartsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			
			ListWindowChartsResponse.Builder chartsList = listWindowCharts(request);
			responseObserver.onNext(chartsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	ListWindowChartsResponse.Builder listWindowCharts(ListWindowChartsRequest request) {
		// validate window
		if (request.getWindowId() <= 0 && Util.isEmpty(request.getWindowUuid(), true)) {
			throw new AdempiereException("@AD_Window_ID@ @NotFound@");
		}
		MWindow window = (MWindow) RecordUtil.getEntity(Env.getCtx(), I_AD_Window.Table_Name, request.getWindowUuid(), request.getWindowId(), null);
		if (window == null || window.getAD_Window_ID() <= 0) {
			throw new AdempiereException("@AD_Window_ID@ @NotFound@");
		}

		// validate tab
		int tabId = request.getTabId();
		if (tabId <= 0 && !Util.isEmpty(request.getTabUuid(), true)) {
			tabId = RecordUtil.getIdFromUuid(I_AD_Tab.Table_Name, request.getTabUuid(), null);
		}

		// Get role
		int roleId = Env.getAD_Role_ID(Env.getCtx());

		//	Get from Charts
		Query query = new Query(
			Env.getCtx(),
			"ECA50_WindowChart",
			this.whereClauseWindowChart,
			null
		)
			.setParameters(window.getAD_Window_ID(), tabId, roleId)
			.setOnlyActiveRecords(true)
		;

		int recordCount = query.count();
		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		if (RecordUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListWindowChartsResponse.Builder builderList = ListWindowChartsResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				ValueUtil.validateNull(nexPageToken)
			)
		;

		query
			.<PO>list()
			.forEach(windowChartAllocation -> {
				MChart chartDefinition = new MChart(
					Env.getCtx(),
					windowChartAllocation.get_ValueAsInt(I_AD_Chart.COLUMNNAME_AD_Chart_ID),
					null
				);
				WindowChart.Builder chartBuilder = DashboardingConvertUtil.convertWindowChart(chartDefinition);
				// TODO: Add sequence on ECA50_WindowChart table
				chartBuilder.setSequence(windowChartAllocation.get_ID());

				List<String> contextColumnsList = DashboardingConvertUtil.getContextColumnsByWindowChart(windowChartAllocation.get_ID());
				chartBuilder.addAllContextColumnNames(contextColumnsList);

				int ruleId = windowChartAllocation.get_ValueAsInt(I_AD_Rule.COLUMNNAME_AD_Rule_ID);
				if (ruleId > 0) {
					MRule rule = MRule.get(Env.getCtx(), ruleId);
					chartBuilder.setTransformationScript(
						ValueUtil.validateNull(rule.getScript())
					);
				}

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
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	@SuppressWarnings("unchecked")
	WindowMetrics.Builder getWindowMetrics(GetWindowMetricsRequest request) {
		WindowMetrics.Builder builder = WindowMetrics.newBuilder();
		MChart chart = (MChart) RecordUtil.getEntity(Env.getCtx(), I_AD_Chart.Table_Name, request.getUuid(), request.getId(), null);
		if (chart == null || chart.getAD_Chart_ID() <= 0) {
			throw new AdempiereException("@AD_Chart_ID@ @NotFound@");
		}

		// validate record
		if (Util.isEmpty(request.getTableName(), true)) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		int recordId = request.getRecordId();
		if (recordId <= 0) {
			recordId = RecordUtil.getIdFromUuid(request.getTableName(), request.getRecordUuid(), null);
		}
		if (recordId <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}

		// fill context
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributes(windowNo, Env.getCtx(), request.getContextAttributesList());

		//	Load
		Map<String, List<ChartData>> chartSeries = new HashMap<String, List<ChartData>>();
		CategoryDataset dataSet = chart.getCategoryDataset();

		dataSet.getColumnKeys().forEach(column -> {
			dataSet.getRowKeys().forEach(row -> {
				//	Get from map
				List<ChartData> serie = chartSeries.get(row);
				if (serie == null) {
					serie = new ArrayList<ChartData>();
				}
				//	Add
				Number value = dataSet.getValue((Comparable<?>)row, (Comparable<?>)column);
				BigDecimal numberValue = (value != null? new BigDecimal(value.doubleValue()): Env.ZERO);
				ChartData.Builder chartDataBuilder = ChartData.newBuilder()
					.setName(column.toString())
					.setValue(
						ValueUtil.getDecimalFromBigDecimal(numberValue)
					)
				;
				serie.add(
					chartDataBuilder.build()
				);
				chartSeries.put(row.toString(), serie);
			});
		});

		// builder.setMeasureTarget(
		// 	ValueUtil.getDecimalFromBigDecimal(goal.getMeasureTarget())
		// );

		//	Add measure color
		// MColorSchema colorSchema = goal.getColorSchema();
		// builder.addAllColorSchemas(
		// 	DashboardingConvertUtil.convertColorSchemasList(colorSchema)
		// );

		//	Add all
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
