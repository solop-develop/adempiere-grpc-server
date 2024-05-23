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
package org.spin.server;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.compiere.jr.report.ReportStarter;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.spin.base.setup.SetupLoader;
import org.spin.eca52.util.JWTUtil;
import org.spin.grpc.service.BusinessData;
import org.spin.grpc.service.Dashboarding;
import org.spin.grpc.service.Enrollment;
import org.spin.grpc.service.FileManagement;
import org.spin.grpc.service.GeneralLedger;
import org.spin.grpc.service.ImportFileLoader;
import org.spin.grpc.service.InOutInfo;
import org.spin.grpc.service.LogsInfo;
import org.spin.grpc.service.MaterialManagement;
import org.spin.grpc.service.NoticeManagement;
import org.spin.grpc.service.PaymentPrintExport;
import org.spin.grpc.service.PointOfSalesForm;
import org.spin.grpc.service.RecordManagement;
import org.spin.grpc.service.ReportManagement;
import org.spin.grpc.service.Security;
import org.spin.grpc.service.TimeControl;
import org.spin.grpc.service.TimeRecord;
import org.spin.grpc.service.UpdateManagement;
import org.spin.grpc.service.UserCustomization;
import org.spin.grpc.service.UserInterface;
import org.spin.grpc.service.WebStore;
import org.spin.grpc.service.Workflow;
import org.spin.grpc.service.core_functionality.CoreFunctionality;
import org.spin.grpc.service.dictionary.Dictionary;
import org.spin.grpc.service.field.business_partner.BusinessPartnerInfo;
import org.spin.grpc.service.field.invoice.InvoiceInfoService;
import org.spin.grpc.service.field.location_address.LocationAddress;
import org.spin.grpc.service.field.payment.PaymentInfoService;
import org.spin.grpc.service.field.order.OrderInfoService;
import org.spin.grpc.service.field.product.ProductInfo;
import org.spin.grpc.service.form.ExpressMovement;
import org.spin.grpc.service.form.ExpressReceipt;
import org.spin.grpc.service.form.ExpressShipment;
import org.spin.grpc.service.form.PaymentAllocation;
import org.spin.grpc.service.form.bank_statement_match.BankStatementMatch;
import org.spin.grpc.service.form.issue_management.IssueManagement;
import org.spin.grpc.service.form.match_po_receipt_invoice.MatchPOReceiptInvoice;
import org.spin.grpc.service.form.payroll_action_notice.PayrollActionNotice;
import org.spin.grpc.service.form.task_management.TaskManagement;
import org.spin.grpc.service.form.trial_balance_drillable.TrialBalanceDrillable;
import org.spin.service.grpc.authentication.AuthorizationServerInterceptor;
import org.spin.service.grpc.context.ServiceContextProvider;

import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.grpc.ServerBuilder;

public class AllInOneServices {
	private static final Logger logger = Logger.getLogger(AllInOneServices.class.getName());

	private Server server;

	private static String defaultFileConnection = "resources/standalone.yaml";
	private ServiceContextProvider contextProvider = new ServiceContextProvider();


	/** Services/Methods allow request without Bearer token validation */
	private List<String> ALLOW_REQUESTS_WITHOUT_TOKEN = Arrays.asList(
		// proto package . proto service / proto method
		"core_functionality.CoreFunctionality/GetSystemInfo",
		"core_functionality.CoreFunctionality/ListLanguages",
		"security.Security/RunLogin",
		"security.Security/ListServices",
		"security.Security/RunLoginOpenID"
	);

	/**	Revoke session	*/
	private List<String> REVOKE_TOKEN_SERVICES = Arrays.asList(
		// proto package . proto service / proto method
		"security.Security/RunChangeRole",
		"security.Security/SetSessionAttribute",
		"security.Security/RunLogout"
	);

	private AuthorizationServerInterceptor getInterceptor() {
		AuthorizationServerInterceptor interceptor = new AuthorizationServerInterceptor();
		interceptor.setAllowRequestsWithoutToken(this.ALLOW_REQUESTS_WITHOUT_TOKEN);
		interceptor.setRevokeTokenServices(this.REVOKE_TOKEN_SERVICES);
		return interceptor;
	}


	/**
	  * Get SSL / TLS context
	  * @return
	  */
	  private SslContextBuilder getSslContextBuilder() {
	        SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(new File(SetupLoader.getInstance().getServer().getCertificate_chain_file()),
	                new File(SetupLoader.getInstance().getServer().getPrivate_key_file()));
	        if (SetupLoader.getInstance().getServer().getTrust_certificate_collection_file() != null) {
	            sslClientContextBuilder.trustManager(new File(SetupLoader.getInstance().getServer().getTrust_certificate_collection_file()));
	            sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
	        }
	        return GrpcSslContexts.configure(sslClientContextBuilder);
	  }

	private void start() throws IOException {
		//	Start based on provider
		Env.setContextProvider(this.contextProvider);
		ReportStarter.setReportViewerProvider(
			new ServerReportProvider()
		);
		Ini.setProperty(
			JWTUtil.ECA52_JWT_SECRET_KEY,
			SetupLoader.getInstance().getServer().getJwt_secret_key()
		);
		Ini.setProperty(
			"JWT_EXPIRATION_TIME",
			String.valueOf(
				SetupLoader.getInstance().getServer().getJwt_expiration_time()
			)
		);

		ServerBuilder<?> serverBuilder;
		if(SetupLoader.getInstance().getServer().isTlsEnabled()) {
			serverBuilder = NettyServerBuilder.forPort(
					SetupLoader.getInstance().getServer().getPort()
				)
				.sslContext(
					getSslContextBuilder().build()
				);
		} else {
			serverBuilder = ServerBuilder.forPort(
				SetupLoader.getInstance().getServer().getPort()
			);
		}

		// Validate JWT on all requests
		AuthorizationServerInterceptor interceptor = getInterceptor();
		serverBuilder.intercept(interceptor);

		//	Bank Statement Match
		serverBuilder.addService(new BankStatementMatch());
		logger.info("Service " + BankStatementMatch.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Business Logic
		serverBuilder.addService(new BusinessData());
		logger.info("Service " + BusinessData.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Business Partner Info
		serverBuilder.addService(new BusinessPartnerInfo());
		logger.info("Service " + BusinessPartnerInfo.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Core Implementation
		serverBuilder.addService(new CoreFunctionality());
		logger.info("Service " + CoreFunctionality.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Dashboarding
		serverBuilder.addService(new Dashboarding());
		logger.info("Service " + Dashboarding.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Dictionary
		serverBuilder.addService(new Dictionary());
		logger.info("Service " + Dictionary.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Enrollment
		serverBuilder.addService(new Enrollment());
		logger.info("Service " + Enrollment.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Express Movement
		serverBuilder.addService(new ExpressMovement());
		logger.info("Service " + ExpressMovement.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Express Receipt
		serverBuilder.addService(new ExpressReceipt());
		logger.info("Service " + ExpressReceipt.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Express Shipment
		serverBuilder.addService(new ExpressShipment());
		logger.info("Service " + ExpressShipment.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	File Management
		serverBuilder.addService(new FileManagement());
		logger.info("Service " + FileManagement.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	General Ledger
		serverBuilder.addService(new GeneralLedger());
		logger.info("Service " + GeneralLedger.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Import File Loader
		serverBuilder.addService(new ImportFileLoader());
		logger.info("Service " + ImportFileLoader.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	In-Out
		serverBuilder.addService(new InOutInfo());
		logger.info("Service " + InOutInfo.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Invoice Field
		serverBuilder.addService(new InvoiceInfoService());
		logger.info("Service " + InvoiceInfoService.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Order Field
		serverBuilder.addService(new OrderInfoService());
		logger.info("Service " + OrderInfoService.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Issue Management
		serverBuilder.addService(new IssueManagement());
		logger.info("Service " + IssueManagement.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Location Address
		serverBuilder.addService(new LocationAddress());
		logger.info("Service " + LocationAddress.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Log
		serverBuilder.addService(new LogsInfo());
		logger.info("Service " + LogsInfo.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Match PO-Receipt-Invocie
		serverBuilder.addService(new MatchPOReceiptInvoice());
		logger.info("Service " + MatchPOReceiptInvoice.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Material Management
		serverBuilder.addService(new MaterialManagement());
		logger.info("Service " + MaterialManagement.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Notice Management
		serverBuilder.addService(new NoticeManagement());
		logger.info("Service " + NoticeManagement.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Payment
		serverBuilder.addService(new PaymentInfoService());
		logger.info("Service " + PaymentInfoService.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Payment Allocation
		serverBuilder.addService(new PaymentAllocation());
		logger.info("Service " + PaymentAllocation.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Payment Print/Export
		serverBuilder.addService(new PaymentPrintExport());
		logger.info("Service " + PaymentPrintExport.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Payroll Action Notice
		serverBuilder.addService(new PayrollActionNotice());
		logger.info("Service " + PayrollActionNotice.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Product
		serverBuilder.addService(new ProductInfo());
		logger.info("Service " + ProductInfo.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	POS
		serverBuilder.addService(new PointOfSalesForm());
		logger.info("Service " + PointOfSalesForm.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Record Management
		serverBuilder.addService(new RecordManagement());
		logger.info("Service " + RecordManagement.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Report Management
		serverBuilder.addService(new ReportManagement());
		logger.info("Service " + ReportManagement.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Security
		serverBuilder.addService(new Security());
		logger.info("Service " + Security.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Task Management
		serverBuilder.addService(new TaskManagement());
		logger.info("Service " + TaskManagement.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Time Control
		serverBuilder.addService(new TimeControl());
		logger.info("Service " + TimeControl.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Time Record
		serverBuilder.addService(new TimeRecord());
		logger.info("Service " + TimeRecord.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Trial Balance Drillable Report
		serverBuilder.addService(new TrialBalanceDrillable());
		logger.info("Service " + TrialBalanceDrillable.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Update Center
		serverBuilder.addService(new UpdateManagement());
		logger.info("Service " + UpdateManagement.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	User Customization
		serverBuilder.addService(new UserCustomization());
		logger.info("Service " + UserCustomization.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	User Interface
		serverBuilder.addService(new UserInterface());
		logger.info("Service " + UserInterface.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Web Store
		serverBuilder.addService(new WebStore());
		logger.info("Service " + WebStore.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		//	Workflow
		serverBuilder.addService(new Workflow());
		logger.info("Service " + Workflow.class.getName() + " added on " + SetupLoader.getInstance().getServer().getPort());

			//	Add Server
		this.server = serverBuilder.build().start();
		logger.info("Server started, listening on " + SetupLoader.getInstance().getServer().getPort());
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// Use stderr here since the logger may have been reset by its JVM shutdown hook.
				logger.info("*** shutting down gRPC server since JVM is shutting down");
		    	  AllInOneServices.this.stop();
		    	logger.info("*** server shut down");
			}
		});
	  }

	private void stop() {
		if (this.server != null) {
			this.server.shutdown();
		}
	}

	/**
	 * Await termination on the main thread since the grpc library uses daemon threads.
	 */
	private void blockUntilShutdown() throws InterruptedException {
		if (this.server != null) {
			this.server.awaitTermination();
		}
	}

	/**
	 * Main launches the server from the command line.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args == null) {
			throw new Exception("Arguments Not Found");
		}
		//
		if (args.length == 0) {
			throw new Exception("Arguments Must Be: [property file name]");
		}
		String setupFileName = args[0];
		if (setupFileName == null || setupFileName.trim().length() == 0) {
			setupFileName = defaultFileConnection;
			// throw new Exception("Setup File not found");
		}

		SetupLoader.loadSetup(setupFileName);
		//	Validate load
		SetupLoader.getInstance().validateLoad();
		final AllInOneServices server = new AllInOneServices();
		server.start();
		server.blockUntilShutdown();
	}

}
