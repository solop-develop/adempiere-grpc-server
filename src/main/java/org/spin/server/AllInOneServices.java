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

import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.spin.base.setup.SetupLoader;
import org.spin.base.util.Services;
import org.spin.eca52.util.JWTUtil;
import org.spin.grpc.service.BankStatementMatch;
import org.spin.grpc.service.BusinessData;
import org.spin.grpc.service.BusinessPartner;
import org.spin.grpc.service.CoreFunctionality;
import org.spin.grpc.service.Dashboarding;
import org.spin.grpc.service.Dictionary;
import org.spin.grpc.service.Enrollment;
import org.spin.grpc.service.ExpressMovement;
import org.spin.grpc.service.ExpressReceipt;
import org.spin.grpc.service.ExpressShipment;
import org.spin.grpc.service.FileManagement;
import org.spin.grpc.service.GeneralLedger;
import org.spin.grpc.service.ImportFileLoader;
import org.spin.grpc.service.InOutInfo;
import org.spin.grpc.service.InvoiceInfo;
import org.spin.grpc.service.IssueManagement;
import org.spin.grpc.service.LogsInfo;
import org.spin.grpc.service.MatchPOReceiptInvoice;
import org.spin.grpc.service.MaterialManagement;
import org.spin.grpc.service.OrderInfo;
import org.spin.grpc.service.PaymentAllocation;
import org.spin.grpc.service.PaymentPrintExport;
import org.spin.grpc.service.PaymentInfo;
import org.spin.grpc.service.PayrollActionNotice;
import org.spin.grpc.service.PointOfSalesForm;
import org.spin.grpc.service.ProductInfo;
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
import org.spin.grpc.service.field.location_address.LocationAddress;
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
		"data.CoreFunctionality/GetSystemInfo",
		"security.Security/ListLanguages",
		"security.Security/RunLogin",
		"security.Security/ListServices",
		"security.Security/RunLoginOpenID"
	);

	/**	Revoke session	*/
	private List<String> REVOKE_TOKEN_SERVICES = Arrays.asList(
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
			serverBuilder = NettyServerBuilder.forPort(SetupLoader.getInstance().getServer().getPort())
				.sslContext(getSslContextBuilder().build());
		} else {
			serverBuilder = ServerBuilder.forPort(SetupLoader.getInstance().getServer().getPort());
		}

		// Validate JWT on all requests
		AuthorizationServerInterceptor interceptor = getInterceptor();
		serverBuilder.intercept(interceptor);

		//	Bank Statement Match
		if (SetupLoader.getInstance().getServer().isValidService(Services.BANK_STATEMENT_MATCH.getServiceName())) {
			serverBuilder.addService(new BankStatementMatch());
			logger.info("Service " + Services.BANK_STATEMENT_MATCH.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Business Logic
		if(SetupLoader.getInstance().getServer().isValidService(Services.BUSINESS.getServiceName())) {
			serverBuilder.addService(new BusinessData());
			logger.info("Service " + Services.BUSINESS.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Business Partner
		if (SetupLoader.getInstance().getServer().isValidService(Services.BUSINESS_PARTNER.getServiceName())) {
			serverBuilder.addService(new BusinessPartner());
			logger.info("Service " + Services.BUSINESS_PARTNER.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Core Implementation
		if(SetupLoader.getInstance().getServer().isValidService(Services.CORE.getServiceName())) {
			serverBuilder.addService(new CoreFunctionality());
			logger.info("Service " + Services.CORE.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Dictionary
		if(SetupLoader.getInstance().getServer().isValidService(Services.DICTIONARY.getServiceName())) {
			serverBuilder.addService(new Dictionary());
			logger.info("Service " + Services.DICTIONARY.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Enrollment
		if(SetupLoader.getInstance().getServer().isValidService(Services.ENROLLMENT.getServiceName())) {
			serverBuilder.addService(new Enrollment());
			logger.info("Service " + Services.ENROLLMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		// Express Movement
		if (SetupLoader.getInstance().getServer().isValidService(Services.EXPRESS_MOVEMENT.getServiceName())) {
			serverBuilder.addService(new ExpressMovement());
			logger.info("Service " + Services.EXPRESS_MOVEMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		// Express Receipt
		if (SetupLoader.getInstance().getServer().isValidService(Services.EXPRESS_RECEIPT.getServiceName())) {
			serverBuilder.addService(new ExpressReceipt());
			logger.info("Service " + Services.EXPRESS_RECEIPT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		// Express Shipment
		if (SetupLoader.getInstance().getServer().isValidService(Services.EXPRESS_SHIPMENT.getServiceName())) {
			serverBuilder.addService(new ExpressShipment());
			logger.info("Service " + Services.EXPRESS_SHIPMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	File Management
		if(SetupLoader.getInstance().getServer().isValidService(Services.FILE_MANAGEMENT.getServiceName())) {
			serverBuilder.addService(new FileManagement());
			logger.info("Service " + Services.FILE_MANAGEMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Dashboarding
		if(SetupLoader.getInstance().getServer().isValidService(Services.DASHBOARDING.getServiceName())) {
			serverBuilder.addService(new Dashboarding());
			logger.info("Service " + Services.DASHBOARDING.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Workflow
		if(SetupLoader.getInstance().getServer().isValidService(Services.WORKFLOW.getServiceName())) {
			serverBuilder.addService(new Workflow());
			logger.info("Service " + Services.WORKFLOW.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	General Ledger
		if(SetupLoader.getInstance().getServer().isValidService(Services.GENERAL_LEDGER.getServiceName())) {
			serverBuilder.addService(new GeneralLedger());
			logger.info("Service " + Services.GENERAL_LEDGER.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Import File Loader
		if(SetupLoader.getInstance().getServer().isValidService(Services.IMPORT_FILE_LOADER.getServiceName())) {
			serverBuilder.addService(new ImportFileLoader());
			logger.info("Service " + Services.IMPORT_FILE_LOADER.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Location Address
		if(SetupLoader.getInstance().getServer().isValidService(Services.LOCATION_ADDRESS.getServiceName())) {
			serverBuilder.addService(new LocationAddress());
			logger.info("Service " + Services.LOCATION_ADDRESS.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Log
		if(SetupLoader.getInstance().getServer().isValidService(Services.LOG.getServiceName())) {
			serverBuilder.addService(new LogsInfo());
			logger.info("Service " + Services.LOG.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Match PO-Receipt-Invocie
		if (SetupLoader.getInstance().getServer().isValidService(Services.MATCH_PO_RECEIPT_INVOICE.getServiceName())) {
			serverBuilder.addService(new MatchPOReceiptInvoice());
			logger.info("Service " + Services.MATCH_PO_RECEIPT_INVOICE.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Material Management
		if(SetupLoader.getInstance().getServer().isValidService(Services.MATERIAL_MANAGEMENT.getServiceName())) {
			serverBuilder.addService(new MaterialManagement());
			logger.info("Service " + Services.MATERIAL_MANAGEMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	For Security
		if (SetupLoader.getInstance().getServer().isValidService(Services.SECURITY.getServiceName())) {
			serverBuilder.addService(new Security());
			logger.info("Service " + Services.SECURITY.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Store
		if(SetupLoader.getInstance().getServer().isValidService(Services.STORE.getServiceName())) {
			serverBuilder.addService(new WebStore());
			logger.info("Service " + Services.STORE.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	POS
		if(SetupLoader.getInstance().getServer().isValidService(Services.POS.getServiceName())) {
			serverBuilder.addService(new PointOfSalesForm());
			logger.info("Service " + Services.POS.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Updater
		if(SetupLoader.getInstance().getServer().isValidService(Services.UPDATER.getServiceName())) {
			serverBuilder.addService(new UpdateManagement());
			logger.info("Service " + Services.UPDATER.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	In-Out
		if(SetupLoader.getInstance().getServer().isValidService(Services.IN_OUT.getServiceName())) {
			serverBuilder.addService(new InOutInfo());
			logger.info("Service " + Services.IN_OUT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Invoice
		if(SetupLoader.getInstance().getServer().isValidService(Services.INVOICE.getServiceName())) {
			serverBuilder.addService(new InvoiceInfo());
			logger.info("Service " + Services.INVOICE.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Issue Management
		if (SetupLoader.getInstance().getServer().isValidService(Services.ISSUE_MANAGEMENT.getServiceName())) {
			serverBuilder.addService(new IssueManagement());
			logger.info("Service " + Services.ISSUE_MANAGEMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Order
		if(SetupLoader.getInstance().getServer().isValidService(Services.ORDER.getServiceName())) {
			serverBuilder.addService(new OrderInfo());
			logger.info("Service " + Services.ORDER.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Payment
		if (SetupLoader.getInstance().getServer().isValidService(Services.PAYMENT.getServiceName())) {
			serverBuilder.addService(new PaymentInfo());
			logger.info("Service " + Services.PAYMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Payment
		if (SetupLoader.getInstance().getServer().isValidService(Services.PAYMENT_ALLOCATION.getServiceName())) {
			serverBuilder.addService(new PaymentAllocation());
			logger.info("Service " + Services.PAYMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Payment Print/Export
		if (SetupLoader.getInstance().getServer().isValidService(Services.PAYMENT_PTINT_EXPORT.getServiceName())) {
			serverBuilder.addService(new PaymentPrintExport());
			logger.info("Service " + Services.PAYMENT_PTINT_EXPORT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Payroll Action Notice
		if (SetupLoader.getInstance().getServer().isValidService(Services.PAYROLL_ACTION_NOTICE.getServiceName())) {
			serverBuilder.addService(new PayrollActionNotice());
			logger.info("Service " + Services.PAYROLL_ACTION_NOTICE.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Product
		if (SetupLoader.getInstance().getServer().isValidService(Services.PRODUCT.getServiceName())) {
			serverBuilder.addService(new ProductInfo());
			logger.info("Service " + Services.PRODUCT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Record Management
		if (SetupLoader.getInstance().getServer().isValidService(Services.RECORD_MANAGEMENT.getServiceName())) {
			serverBuilder.addService(new RecordManagement());
			logger.info("Service " + Services.RECORD_MANAGEMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Report Management
		if (SetupLoader.getInstance().getServer().isValidService(Services.REPORT_MANAGEMENT.getServiceName())) {
			serverBuilder.addService(new ReportManagement());
			logger.info("Service " + Services.REPORT_MANAGEMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Time Control
		if(SetupLoader.getInstance().getServer().isValidService(Services.TIME_CONTROL.getServiceName())) {
			serverBuilder.addService(new TimeControl());
			logger.info("Service " + Services.TIME_CONTROL.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Time Record
		if (SetupLoader.getInstance().getServer().isValidService(Services.TIME_RECORD.getServiceName())) {
			serverBuilder.addService(new TimeRecord());
			logger.info("Service " + Services.TIME_RECORD.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	User Customization
		if(SetupLoader.getInstance().getServer().isValidService(Services.USER_CUSTOMIZATION.getServiceName())) {
			serverBuilder.addService(new UserCustomization());
			logger.info("Service " + Services.USER_CUSTOMIZATION.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	User Interface
		if(SetupLoader.getInstance().getServer().isValidService(Services.UI.getServiceName())) {
			serverBuilder.addService(new UserInterface());
			logger.info("Service " + Services.UI.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
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
