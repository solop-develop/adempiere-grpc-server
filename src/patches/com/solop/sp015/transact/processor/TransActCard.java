package com.solop.sp015.transact.processor;

import com.solop.sp015.transact.dto.ArrayOfstring;
import com.solop.sp015.transact.dto.ITarjetasTransaccion401;
import com.solop.sp015.transact.dto.ITarjetasTransaccion401RespuestaCancelarTransaccion;
import com.solop.sp015.transact.dto.ITarjetasTransaccion401RespuestaConsultarTransaccion;
import com.solop.sp015.transact.dto.ITarjetasTransaccion401RespuestaPostearTransaccion;
import com.solop.sp015.transact.dto.ITarjetasTransaccion401TipoEstadoAvance;
import com.solop.sp015.transact.dto.ITarjetasTransaccion401Transaccion;
import com.solop.sp015.transact.dto.TarjetaCierre.ITarjetasCierre400;
import com.solop.sp015.transact.dto.TarjetaCierre.ITarjetasCierre400Cierre;
import com.solop.sp015.transact.dto.TarjetaCierre.ITarjetasCierre400RespuestaConsultarCierre;
import com.solop.sp015.transact.dto.TarjetaCierre.ITarjetasCierre400RespuestaPostearCierre;
import com.solop.sp015.transact.dto.TarjetaCierre.ITarjetasCierre400TipoEstadoAvance;
import com.solop.sp015.transact.dto.TarjetaCierre.TarjetasCierre400;
import com.solop.sp015.transact.dto.TarjetasTransaccion401;
import com.solop.sp015.util.PaymentProcessUtil;
import org.adempiere.core.domains.models.I_C_TaxGroup;
import org.adempiere.core.domains.models.X_C_CardProvider;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.interfaces.PaymentProcessorClosing;
import org.compiere.interfaces.PaymentProcessorReverse;
import org.compiere.interfaces.PaymentProcessorStatus;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankStatement;
import org.compiere.model.MCard;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceTax;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderTax;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.PaymentProcessor;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.store.model.MCPaymentMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Optional;

/**
 * Trans Act Processor
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class TransActCard extends PaymentProcessor implements PaymentProcessorStatus, PaymentProcessorReverse, PaymentProcessorClosing {

    static String RESPONSE_STATUS = "ResponseStatus";
    static String RESPONSE_CODE = "ResponseCode";
    static String NEXT_REQUEST_TIME = "NextRequestTime";
    static String RESPONSE_MESSAGE = "ResponseMessage";

    // Card Provider Codes (Database, not Service)
    static String ALL = "000"; // 0
    static String BROU = "001"; // 1
    static String CABAL = "002"; // 2
    static String LIDER = "003"; // 3
    static String EDENRED = "004"; // 4
    static String SODEXO = "005"; // 5
    static String MIDES = "006"; // 6
    static String BANDES = "007"; // 7
    static String SCOTIABANK = "008"; // 8
    static String CITIBANK = "009"; // 9
    static String BBVA = "010"; // 10
    static String HSBC = "011"; // 11
    static String ITAU = "012"; // 12
    static String SANTANDER = "013"; // 13
    static String OCA = "014"; // 14
    static String FUCAC = "015"; // 15
    static String CREDITOS_DIRECTOS = "016"; // 16
    static String CREDITEL = "017"; // 17
    static String ANDA = "018"; // 18
    static String DISCOVER = "019"; // 19
    static String CLUB_DEL_ESTE = "020"; // 20
    static String PASSCARD = "021"; // 21
    static String PRONTO = "022"; // 22
    static String MI_DINERO = "023"; // 23
    static String ABITAB = "024"; // 24
    static String PREX = "025"; // 25
    static String MASTERCARD = "026"; // 26
    static String HERITAGE = "027"; // 27
    static String ITAU_INFINITE = "028"; // 1201
    static String SANTANDER_SELECT = "029"; // 1302

    //Card Type Codes (Service, not Database)
    static int C_ALL = 0;
    static int C_MASTERCARD = 1;
    static int C_VISA = 2;
    static int C_DISCOVER = 3;
    static int C_AMERICAN_EXPRESS = 4;
    static int C_TARJETA_D = 5;
    static int C_OCA_MASTER = 6;
    static int C_CABAL = 8;
    static int C_ANDA = 9;
    static int C_CREDITEL = 12;
    static int C_PASSCARD = 14;
    static int C_LIDER = 15;
    static int C_CLUB_DEL_ESTE = 16;
    static int C_MAESTRO = 17;
    static int C_EDENRED_ALIM = 19;
    static int C_SODEXO_ALIM = 20;
    static int C_MI_DINERO_ALIM = 21;
    static int C_MIDES = 22;
    static int C_DUCSA = 27;
    static int C_MERCADO_PAGO_QR = 29;
    static int C_OCA_PRODUCTO = 32;
    static int C_AXION = 33;
    static int C_SOY_SANTANDER_QR = 34;
    static int C_CREDITO_DE_LA_CASA_QR = 35;
    static int C_PIX_QR = 36;
    static int C_TOKE_QR = 37;

    @Override
    public boolean processCC() throws IllegalArgumentException {
        //	Validate Online Payment
        MDocType documentType = MDocType.get(getPayment().getCtx(), getPayment().getC_DocType_ID());
        if(!documentType.get_ValueAsBoolean(PaymentProcessUtil.SP015_AllowsOnlinePayment)) {
            return true;
        }
        requestPayment();
        return true;
    }

    @Override
    public boolean isProcessedOK() {
        return true;
    }

    private String getValidCurrencyISOCode(String isoCode) {
        if(isoCode == null) {
            return null;
        }
        if(isoCode.equals("UYU")) {
            return "0858";
        } else if(isoCode.equals("USD")) {
            return "0840";
        }
        return null;
    }

    private String getTerminalCode() {
        if(getPayment().getC_PaymentMethod_ID() <= 0) {
            throw new AdempiereException("@C_PaymentMethod_ID@ @NotFound@");
        }
        MCPaymentMethod paymentMethod = new MCPaymentMethod(getPayment().getCtx(), getPayment().getC_PaymentMethod_ID(), null);
        String terminalCode = paymentMethod.getValue();
        if(Util.isEmpty(terminalCode, true)) {
            throw new AdempiereException("@C_PaymentMethod_ID@ / @Value@ @IsMandatory@");
        }
        return terminalCode;
    }
    private String getTerminalCode(int paymentMethodId) {
        if(paymentMethodId <= 0) {
            throw new AdempiereException("@C_PaymentMethod_ID@ @NotFound@");
        }
        MCPaymentMethod paymentMethod = new MCPaymentMethod(getBankStatement().getCtx(), paymentMethodId, null);
        String terminalCode = paymentMethod.getValue();
        if(Util.isEmpty(terminalCode, true)) {
            throw new AdempiereException("@C_PaymentMethod_ID@ / @Value@ @IsMandatory@");
        }
        return terminalCode;
    }

    private void requestPayment() {
        if(getPayment().getC_Invoice_ID() <= 0 && getPayment().getC_Order_ID() <= 0) {
            throw new AdempiereException("@C_Order_ID@ / @C_Invoice_ID@ @NotFound@");
        }
        if (!getPayment().isReceipt() && Util.isEmpty(getPayment().getR_PnRef_DC())) {
            return;
        }
        //  Only Valid payment
        if(getPayment().isReversal()) {
            return;
        }
        try {
            if(Util.isEmpty(getPayment().get_ValueAsString(RESPONSE_STATUS), true)
                    || "E".equals(getPayment().get_ValueAsString(RESPONSE_STATUS))
                    || "R".equals(getPayment().get_ValueAsString(RESPONSE_STATUS))) {
                MCurrency currency = MCurrency.get(getPayment().getCtx(), getPayment().getC_Currency_ID());
                ITarjetasTransaccion401Transaccion transaction = new ITarjetasTransaccion401Transaccion();
                transaction.setEmpCod(getPaymentProcessor().getUserID());
                transaction.setEmpHASH(getPaymentProcessor().getPassword());
                transaction.setFacturaMonto(getInvoiceAmount());
                transaction.setFacturaMontoGravado(getTaxBaseAmount());
                transaction.setFacturaMontoIVA(getTaxAmount());
                transaction.setFacturaNro(getDocumentNo());
                transaction.setFacturaConsumidorFinal(isFinalConsumer());
                transaction.setMonedaISO(getValidCurrencyISOCode(currency.getISO_Code()));
                transaction.setMonto(getPayment().getPayAmt().multiply(Env.ONEHUNDRED).doubleValue());
                transaction.setOperacion(getPayment().isReceipt() ? "VTA": "DEV");
                if (getPayment().getC_CardProvider_ID() > 0) {
                    X_C_CardProvider cardProvider = new X_C_CardProvider(getPayment().getCtx(), getPayment().getC_CardProvider_ID(), getPayment().get_TrxName());
                    Integer providerCode = getProviderCode(cardProvider.getValue());
                    transaction.setEmisorId(providerCode);

                }
                if (!getPayment().isReceipt()) {
                    Integer originalTicket = new BigDecimal(getPayment().getR_PnRef_DC()).intValue();
                    transaction.setTicketOriginal(originalTicket);
                }
                transaction.setTermCod(getTerminalCode());
                ITarjetasTransaccion401 service = getServiceConnection();
                ITarjetasTransaccion401RespuestaPostearTransaccion response = service.postearTransaccion(transaction);
                if (response.getRespCodigoRespuesta() == 0) {
                    //  Save response
                    getPayment().setR_AuthCode_DC(response.getTokenNro());
                    getPayment().setR_Result(String.valueOf(response.getRespCodigoRespuesta()));
                    getPayment().set_ValueOfColumn(RESPONSE_STATUS, "W");
                } else {
                    getPayment().set_ValueOfColumn(RESPONSE_STATUS, "E");
                }
                getPayment().set_ValueOfColumn(NEXT_REQUEST_TIME, response.getTokenSegundosConsultar() * 1000);
                setResponseMessage(null, response.getRespMensajeError(), response.getRespCodigoRespuesta());
                getPayment().saveEx();
            }
        } catch (Exception e) {
            getPayment().set_ValueOfColumn(RESPONSE_STATUS, "E");
            getPayment().set_ValueOfColumn(RESPONSE_MESSAGE, e.getLocalizedMessage());
            getPayment().saveEx();
        }

    }
    @Override
    public boolean closeBatch(int paymentMethodId, Timestamp date) {
        if(paymentMethodId <= 0) {
            throw new AdempiereException("@C_PaymentMethod_ID@ @NotFound@");
        }
		/*
		String lastStatus = null;
		String whereClause = "C_BankStatement_ID = ? AND C_PaymentMethod_ID = ?";
		PO paymentProcessorRun = new Query(getBankStatement().getCtx(), "C_PaymentProcessorRun", whereClause, getBankStatement().get_TrxName())
            .setParameters(getBankStatement().get_ID(), paymentMethodId)
            .setOrderBy("Created DESC")
            .first();
        */
        MTable processorRunTable = MTable.get(getBankStatement().getCtx(), "C_PaymentProcessorRun");
        if (processorRunTable == null || processorRunTable.get_ID() <= 0){
            throw new AdempiereException("@M_Table_ID@: C_PaymentProcessorRun @NotFound@");
        }
        PO paymentProcessorRun = processorRunTable.getPO(0, getBankStatement().get_TrxName());
        paymentProcessorRun.set_ValueOfColumn(MBankStatement.COLUMNNAME_C_BankStatement_ID, getBankStatement().get_ID());
        paymentProcessorRun.set_ValueOfColumn(MCPaymentMethod.COLUMNNAME_C_PaymentMethod_ID, paymentMethodId);
        paymentProcessorRun.saveEx();
        setPaymentProcessorRun(paymentProcessorRun);

        try {
            ITarjetasCierre400Cierre transaction = new ITarjetasCierre400Cierre();
            transaction.setEmpCod(getPaymentProcessor().getUserID());
            transaction.setEmpHASH(getPaymentProcessor().getPassword());
            transaction.setTermCod(getTerminalCode(paymentMethodId));
            ITarjetasCierre400 service = getClosingServiceConnection();
            ITarjetasCierre400RespuestaPostearCierre response = service.postearCierre(transaction);
            if (response.getRespCodigoRespuesta() == 0) {
                paymentProcessorRun.set_ValueOfColumn("R_AuthCode_DC", response.getTokenNro());
                paymentProcessorRun.set_ValueOfColumn(RESPONSE_STATUS, "W");
                paymentProcessorRun.set_ValueOfColumn(RESPONSE_CODE, String.valueOf(response.getRespCodigoRespuesta()));
                //  Save response
            } else {
                paymentProcessorRun.set_ValueOfColumn(RESPONSE_STATUS, "E");
            }

            paymentProcessorRun.set_ValueOfColumn(NEXT_REQUEST_TIME, new BigDecimal(response.getTokenSegundosConsultar() * 1000));
            setResponseMessage(null, response.getRespMensajeError(), response.getRespCodigoRespuesta());
            paymentProcessorRun.saveEx();

        } catch (Exception e) {
            paymentProcessorRun.set_ValueOfColumn(RESPONSE_STATUS, "E");
            paymentProcessorRun.set_ValueOfColumn(RESPONSE_MESSAGE,  e.getLocalizedMessage());
            paymentProcessorRun.saveEx();

        }
        return false;
    }

    private double getDocumentNo() {
        String documentNoAsString = "";
        if(getPayment().getC_Invoice_ID() > 0) {
            MInvoice invoice = new MInvoice(getPayment().getCtx(), getPayment().getC_Invoice_ID(), getPayment().get_TrxName());
            documentNoAsString = invoice.getDocumentNo();
        } else if(getPayment().getC_Order_ID() > 0) {
            MOrder order = new MOrder(getPayment().getCtx(), getPayment().getC_Order_ID(), getPayment().get_TrxName());
            documentNoAsString = order.getDocumentNo();
        } else {
            documentNoAsString = getPayment().getDocumentNo();
        }
        return Double.parseDouble(documentNoAsString.replaceAll("\\D+", ""));
    }

    private boolean isFinalConsumer() {
        MBPartner businessPartner = MBPartner.get(getPayment().getCtx(), getPayment().getC_BPartner_ID());
        if(businessPartner.getC_TaxGroup_ID() <= 0) {
            return true;
        }
        PO taxGroup = new Query(getPayment().getCtx(), I_C_TaxGroup.Table_Name, "C_TaxGroup_ID = ?", null).setParameters(businessPartner.getC_TaxGroup_ID()).first();
        if(taxGroup == null) {
            return true;
        }
        return Optional.ofNullable(taxGroup.get_ValueAsString("Value")).orElse("").equals("CI");
    }

    private double getInvoiceAmount() {
        BigDecimal amount = Env.ZERO;
        if(getPayment().getC_Invoice_ID() > 0) {
            MInvoice invoice = new MInvoice(getPayment().getCtx(), getPayment().getC_Invoice_ID(), getPayment().get_TrxName());
            amount = invoice.getGrandTotal();
            amount = getConvertedAmount(amount, invoice.getC_Currency_ID());
        } else if(getPayment().getC_Order_ID() > 0) {
            MOrder order = new MOrder(getPayment().getCtx(), getPayment().getC_Order_ID(), getPayment().get_TrxName());
            amount = order.getGrandTotal();
            amount = getConvertedAmount(amount, order.getC_Currency_ID());
        } else {
            amount = getPayment().getPayAmt(true);
        }
        return amount.multiply(Env.ONEHUNDRED).doubleValue();
    }

    private double getTaxAmount() {
        BigDecimal amount = Env.ZERO;
        if(getPayment().getC_Invoice_ID() > 0) {
            MInvoice invoice = new MInvoice(getPayment().getCtx(), getPayment().getC_Invoice_ID(), getPayment().get_TrxName());
            amount = Arrays.stream(invoice.getTaxes(true))
                    .map(MInvoiceTax::getTaxAmt)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            amount = getConvertedAmount(amount, invoice.getC_Currency_ID());
        } else if(getPayment().getC_Order_ID() > 0) {
            MOrder order = new MOrder(getPayment().getCtx(), getPayment().getC_Order_ID(), getPayment().get_TrxName());
            amount = Arrays.stream(order.getTaxes(true))
                    .map(MOrderTax::getTaxAmt)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            amount = getConvertedAmount(amount, order.getC_Currency_ID());
        }
        return amount.multiply(Env.ONEHUNDRED).doubleValue();
    }

    private double getTaxBaseAmount() {
        BigDecimal amount = Env.ZERO;
        if(getPayment().getC_Invoice_ID() > 0) {
            MInvoice invoice = new MInvoice(getPayment().getCtx(), getPayment().getC_Invoice_ID(), getPayment().get_TrxName());
            amount = Arrays.stream(invoice.getTaxes(true))
                    .filter(tax -> tax.getTaxAmt().signum() != 0)
                    .map(MInvoiceTax::getTaxBaseAmt)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            amount = getConvertedAmount(amount, invoice.getC_Currency_ID());
        } else if(getPayment().getC_Order_ID() > 0) {
            MOrder order = new MOrder(getPayment().getCtx(), getPayment().getC_Order_ID(), getPayment().get_TrxName());
            amount = Arrays.stream(order.getTaxes(true))
                    .filter(tax -> tax.getTaxAmt().signum() != 0)
                    .map(MOrderTax::getTaxBaseAmt)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            amount = getConvertedAmount(amount, order.getC_Currency_ID());
        }
        return amount.multiply(Env.ONEHUNDRED).doubleValue();
    }

    public BigDecimal getConvertedAmount(BigDecimal amount, int currencyFromId) {
        MCurrency currency = MCurrency.get(getPayment().getCtx(), getPayment().getC_Currency_ID());
        if(currencyFromId == currency.getC_Currency_ID()
                || amount == null
                || amount.compareTo(Env.ZERO) == 0) {
            return amount;
        }
        return MConversionRate.convert(getPayment().getCtx(), amount, currencyFromId, currency.getC_Currency_ID(), getPayment().getDateAcct(), getPayment().getC_ConversionType_ID(), getPayment().getAD_Client_ID(), getPayment().getAD_Org_ID());
    }

    public ITarjetasTransaccion401 getServiceConnection() {
        ITarjetasTransaccion401 port;
        try {
            String host = getPaymentProcessor().getHostAddress();
            if(getPaymentProcessor().getHostPort() > 0) {
                host = host + ":" + getPaymentProcessor().getHostPort();
            }
            TarjetasTransaccion401 service = new TarjetasTransaccion401(new URL(host + "/Concentrador/TarjetasTransaccion_401.svc?wsdl"));
            port = service.getBasicHttpBindingITarjetasTransaccion401();
        } catch (MalformedURLException e) {
            throw new AdempiereException(e);
        }
        return port;
    }
    public ITarjetasCierre400 getClosingServiceConnection() {
        ITarjetasCierre400 port;
        try {
            String host = getPaymentProcessor().getHostAddress();
            if(getPaymentProcessor().getHostPort() > 0) {
                host = host + ":" + getPaymentProcessor().getHostPort();
            }
            TarjetasCierre400 service = new TarjetasCierre400(new URL(host + "/Concentrador/TarjetasCierre_400.svc?wsdl"));
            port = service.getBasicHttpBindingITarjetasCierre400();
        } catch (MalformedURLException e) {
            throw new AdempiereException(e);
        }
        return port;
    }
    private void setResponseMessage(String responseMsg, String responseErrorMsg, int responseCode) {
        StringBuilder responseStr = new StringBuilder();
        if (!Util.isEmpty(responseMsg, true)) {
            responseStr.append(responseMsg);
        }
        if (!Util.isEmpty(responseErrorMsg, true)) {
            if (responseStr.length() > 0) {
                responseStr.append(" - ");
            }
            responseStr.append(responseErrorMsg);
        }
        if (isPayment()) {
            getPayment().set_ValueOfColumn(RESPONSE_MESSAGE, responseStr.toString());
            getPayment().set_ValueOfColumn(RESPONSE_CODE, String.valueOf(responseCode));
        } else {
            getPaymentProcessorRun().set_ValueOfColumn(RESPONSE_MESSAGE, responseStr.toString());
            getPaymentProcessorRun().set_ValueOfColumn(RESPONSE_CODE, String.valueOf(responseCode));
        }


    }
    @Override
    public boolean transactionReverse() {
        try {
            if ("A".equals(getPayment().get_ValueAsString(RESPONSE_STATUS))) {
                return false;
            }
            if ("R".equals(getPayment().get_ValueAsString(RESPONSE_STATUS))) {
                return true;
            }
            if (Util.isEmpty(getPayment().getR_AuthCode_DC(), true)) {
                return true;
            }
            ITarjetasTransaccion401 service = getServiceConnection();
            ITarjetasTransaccion401RespuestaConsultarTransaccion responseRequest = service.consultarTransaccion(getPayment().getR_AuthCode_DC());

            if(!responseRequest.isAprobada()) {
                ITarjetasTransaccion401RespuestaCancelarTransaccion responseCancel = service.cancelarTransaccion(getPayment().getR_AuthCode_DC());
                if (responseCancel.getRespCodigoRespuesta() == 0) {
                    getPayment().set_ValueOfColumn(RESPONSE_STATUS, null);
                    getPayment().setR_AuthCode_DC("");
                    getPayment().setR_Info(String.valueOf(responseCancel.getRespCodigoRespuesta()));
                    return true;
                }
                setResponseMessage(null, responseCancel.getRespMensajeError(), responseCancel.getRespCodigoRespuesta());
            }
        } catch (Exception e) {
            getPayment().set_ValueOfColumn(RESPONSE_MESSAGE, e.getLocalizedMessage());
        }
        return false;
    }

    @Override
    public boolean transactionStatus(int paymentProcessorRunId) {
        if (isPayment()) {
            // Search response
            if(Util.isEmpty(getPayment().get_ValueAsString(RESPONSE_STATUS), true)) {
                return true;
            }
            try {
                ITarjetasTransaccion401 service = getServiceConnection();
                ITarjetasTransaccion401RespuestaConsultarTransaccion response = service.consultarTransaccion(getPayment().getR_AuthCode_DC());
                if(response.isAprobada()) {
                    getPayment().setR_RespMsg(String.valueOf(response.getMsgRespuesta()));
                    getPayment().setR_Info(String.valueOf(response.getRespCodigoRespuesta()));
                    getPayment().setR_PnRef(response.getNroAutorizacion());
                    getPayment().setDocumentNo(response.getNroAutorizacion());
                    getPayment().setR_AuthCode(response.getNroAutorizacion());
                    getPayment().setR_PnRef_DC(String.valueOf(response.getTicket()));
                    MCard card = getCard(response.getTarjetaId());
                    if (card != null && card.getC_Card_ID() >0) {
                        getPayment().setC_Card_ID(card.get_ID());
                        String cardTypeCode = card.getCreditCardType();
                        if (!Util.isEmpty(cardTypeCode, true)) {
                            getPayment().setCreditCardType(cardTypeCode);
                        }
                    }
                    getPayment().setIsApproved(true);
                }
                getPayment().set_ValueOfColumn(NEXT_REQUEST_TIME, response.getRespTokenSegundosReConsultar() * 1000);
                setResponseMessage(response.getMsgRespuesta(), response.getRespMensajeError(), response.getRespCodigoRespuesta());
                validateProgressStatus(response.getRespEstadoAvance());
                getPayment().saveEx();
                processResponse(response);
            } catch (Exception e) {
                getPayment().set_ValueOfColumn(RESPONSE_STATUS, "E");
                getPayment().set_ValueOfColumn(RESPONSE_MESSAGE, e.getLocalizedMessage());
                getPayment().saveEx();
            }
        } else if (getBankStatement() != null) {

            String lastStatus = null;
            PO paymentProcessorRun = getPaymentProcessorRun();
            if (paymentProcessorRun == null) {
                //String whereClause = "C_BankStatement_ID = ? AND C_PaymentMethod_ID = ?";
                MTable processorRunTable = MTable.get(Env.getCtx(),"C_PaymentProcessorRun");
                paymentProcessorRun = processorRunTable.getPO(paymentProcessorRunId, getBankStatement().get_TrxName());
                if (paymentProcessorRun != null) {
                    lastStatus = paymentProcessorRun.get_ValueAsString(RESPONSE_STATUS);
                    setPaymentProcessorRun(paymentProcessorRun);
                }
            }
            if(Util.isEmpty(lastStatus, true) || "A".equals(lastStatus)) {
                return true;
            }
            try {
                String tokenNo = paymentProcessorRun.get_ValueAsString("R_AuthCode_DC");
                ITarjetasCierre400 service = getClosingServiceConnection();
                ITarjetasCierre400RespuestaConsultarCierre response = service.consultarCierre(tokenNo);
                if(response.isRespCierreFinalizado()) {
                    if ("APROBADO".equals(response.getEstado())){
                        paymentProcessorRun.set_ValueOfColumn(RESPONSE_STATUS, "A");
                    } else {
                        paymentProcessorRun.set_ValueOfColumn(RESPONSE_STATUS, "E");
                    }
                    setResponseMessage(response.getEstado(), response.getRespMensajeError(), response.getRespCodigoRespuesta());
                    //validateProgressStatus(response.getRespEstadoAvance());
                    processClosingResponse(response);
                } else if (response.getRespEstadoAvance() != null) {
                    setResponseMessage(response.getEstado(), response.getRespMensajeError(), response.getRespCodigoRespuesta());
                    validateProgressStatus(response.getRespEstadoAvance());
                    processClosingResponse(response);
                } else if ("W".equals(paymentProcessorRun.get_ValueAsString(RESPONSE_STATUS))) {
                    setResponseMessage("","", response.getRespCodigoRespuesta());
                }
                paymentProcessorRun.set_ValueOfColumn(NEXT_REQUEST_TIME, new BigDecimal(response.getRespTokenSegundosReConsultar() * 1000));

                paymentProcessorRun.saveEx();
            } catch (Exception e) {
                paymentProcessorRun.set_ValueOfColumn(RESPONSE_STATUS, "E");
                paymentProcessorRun.set_ValueOfColumn(RESPONSE_MESSAGE, e.getLocalizedMessage());
                paymentProcessorRun.saveEx();
            }
        }
        return false;
    }

    private void processResponse(ITarjetasTransaccion401RespuestaConsultarTransaccion response) {
        addNumericValue("Lote", BigDecimal.valueOf(response.getLote()));
        addNumericValue("RespCodigoRespuesta", BigDecimal.valueOf(response.getRespCodigoRespuesta()));
        addTextValue("NroAutorizacion", response.getNroAutorizacion());
        addTextValue("MsgRespuesta", response.getMsgRespuesta());
        addTextValue("CodRespAdq", response.getCodRespAdq());
        addTextValue("RespMensajeError", response.getRespMensajeError());
        addNumericValue("RespTokenSegundosReConsultar", BigDecimal.valueOf(response.getRespTokenSegundosReConsultar()));
        addNumericValue("TarjetaId", BigDecimal.valueOf(response.getTarjetaId()));
        addTextValue("TarjetaTipo", response.getTarjetaTipo());
        addTextValue("Ticket", String.valueOf(response.getTicket()));
        addTextValue("TokenNro", response.getTokenNro());
        addNumericValue("TransaccionId", BigDecimal.valueOf(response.getTransaccionId()).setScale(0, RoundingMode.HALF_UP));
        addBooleanValue("Aprobada", response.isAprobada());
        addBooleanValue("EsOffline", response.isEsOffline());
        addBooleanValue("RespTransaccionFinalizada", response.isRespTransaccionFinalizada());
        ArrayOfstring voucher = response.getVoucher();
        if(voucher != null && !voucher.getString().isEmpty()) {
            StringBuilder voucherValue = new StringBuilder();
            voucher.getString().forEach(value -> voucherValue.append(value).append(Env.NL));
            String voucherAsString = voucherValue.toString()
                    .replaceAll("#LOGO#", "")
                    .replaceAll("#CF#", "")
                    .replaceAll("/I", "\n")
                    .replaceAll("/H", "\n")
                    .replaceAll("/N", "\n")
                    .replaceAll("\n\n", "\n")
                    .replaceAll("\n\n\n", "\n");
            String [] vouchers = voucherAsString.split("#BR#\n");
            if(vouchers.length > 0) {
                Arrays.asList(vouchers).forEach(value -> addTextValue("Voucher", value));
            }
        }
    }
    private void processClosingResponse(ITarjetasCierre400RespuestaConsultarCierre response) {
        addTextValue("Estado", response.getEstado());
        addNumericValue("RespCodigoRespuesta", BigDecimal.valueOf(response.getRespCodigoRespuesta()));
        addTextValue("TokenNro", response.getTokenNro());
        addTextValue("RespMensajeError", response.getRespMensajeError());
        addNumericValue("RespTokenSegundosReConsultar", BigDecimal.valueOf(response.getRespTokenSegundosReConsultar()));
        addBooleanValue("RespCierreFinalizado", response.isRespCierreFinalizado());
        com.solop.sp015.transact.dto.TarjetaCierre.ArrayOfstring voucher = response.getVoucher();
        if(voucher != null && !voucher.getString().isEmpty()) {
            StringBuilder voucherValue = new StringBuilder();
            voucher.getString().forEach(value -> voucherValue.append(value).append(Env.NL));
            String voucherAsString = voucherValue.toString()
                    .replaceAll("#LOGO#", "")
                    .replaceAll("#CF#", "")
                    .replaceAll("/I", "\n")
                    .replaceAll("/H", "\n")
                    .replaceAll("/N", "\n")
                    .replaceAll("\n\n", "\n")
                    .replaceAll("\n\n\n", "\n")
                    .replaceAll("#BR#", "");
            addTextValue("Voucher", voucherAsString);
        }
    }

    private void validateProgressStatus(ITarjetasTransaccion401TipoEstadoAvance status) {
        if (status.equals(ITarjetasTransaccion401TipoEstadoAvance.ESTADOAVANCE_FINALIZADA_CORRECTAMENTE)) {
            getPayment().set_ValueOfColumn(RESPONSE_STATUS, "A");
        } else if (status.equals(ITarjetasTransaccion401TipoEstadoAvance.ESTADOAVANCE_CANCELADA)) {
            getPayment().set_ValueOfColumn(RESPONSE_STATUS, "E");
        } else if (status.equals(ITarjetasTransaccion401TipoEstadoAvance.ESTADOAVANCE_FINALIZADA_ERROR)) {
            getPayment().set_ValueOfColumn(RESPONSE_STATUS, "R");
        } else if (status.equals(ITarjetasTransaccion401TipoEstadoAvance.ESTADOAVANCE_ENPROCESO)) {
            getPayment().set_ValueOfColumn(RESPONSE_STATUS, "W");
        } else if (status.equals(ITarjetasTransaccion401TipoEstadoAvance.ESTADOAVANCE_PENDIENTE_PROCESO)) {
            getPayment().set_ValueOfColumn(RESPONSE_STATUS, "W");
        } else if (status.equals(ITarjetasTransaccion401TipoEstadoAvance.ESTADOAVANCE_PROCESADA_SIN_CONFIRMAR)) {
            getPayment().set_ValueOfColumn(RESPONSE_STATUS, "W");
        } else if (status.equals(ITarjetasTransaccion401TipoEstadoAvance.ESTADOAVANCE_SINDEFINIR)) {
            getPayment().set_ValueOfColumn(RESPONSE_STATUS, "W");
        } else {
            getPayment().set_ValueOfColumn(RESPONSE_STATUS, "E");
        }
    }
    private void validateProgressStatus(ITarjetasCierre400TipoEstadoAvance status) {
        String newStatus = "E";
        if (status == null) {
            newStatus = "E";
        } else if (status.equals(ITarjetasCierre400TipoEstadoAvance.FINALIZADO)) {
            newStatus = "A";
        } else if (status.equals(ITarjetasCierre400TipoEstadoAvance.TOKEN_INEXISTENTE)) {
            newStatus = "E";
        } else if (status.equals(ITarjetasCierre400TipoEstadoAvance.ERROR)) {
            newStatus = "R";
        } else if (status.equals(ITarjetasCierre400TipoEstadoAvance.EN_PROCESO)) {
            newStatus = "W";
        } else if (status.equals(ITarjetasCierre400TipoEstadoAvance.ESPERANDO)) {
            newStatus = "W";
        } else {
            newStatus = "E";
        }
        getPaymentProcessorRun().set_ValueOfColumn(RESPONSE_STATUS, newStatus);
    }
    private Integer getProviderCode(String code){
        Integer result = null;
        if (ALL.equals(code)){
            result = 0;
        } else if (BROU.equals(code)) {
            result = 1;
        } else if(CABAL.equals(code)) {
            result = 2;
        } else if(LIDER.equals(code)) {
            result = 3;
        } else if(EDENRED.equals(code)) {
            result = 4;
        } else if(SODEXO.equals(code)) {
            result = 5;
        } else if(MIDES.equals(code)) {
            result = 6;
        } else if(BANDES.equals(code)) {
            result = 7;
        } else if(SCOTIABANK.equals(code)) {
            result = 8;
        } else if(CITIBANK.equals(code)) {
            result = 9;
        } else if(BBVA.equals(code)) {
            result = 10;
        } else if(HSBC.equals(code)) {
            result = 11;
        } else if(ITAU.equals(code)) {
            result = 12;
        } else if(SANTANDER.equals(code)) {
            result = 13;
        } else if(OCA.equals(code)) {
            result = 14;
        } else if(FUCAC.equals(code)) {
            result = 15;
        } else if(CREDITOS_DIRECTOS.equals(code)) {
            result = 16;
        } else if(CREDITEL.equals(code)) {
            result = 17;
        } else if(ANDA.equals(code)) {
            result = 18;
        } else if(DISCOVER.equals(code)) {
            result = 19;
        } else if(CLUB_DEL_ESTE.equals(code)) {
            result = 20;
        } else if(PASSCARD.equals(code)) {
            result = 21;
        } else if(PRONTO.equals(code)) {
            result = 22;
        } else if(MI_DINERO.equals(code)) {
            result = 23;
        } else if(ABITAB.equals(code)) {
            result = 24;
        } else if(PREX.equals(code)) {
            result = 25;
        } else if(MASTERCARD.equals(code)) {
            result = 26;
        } else if(HERITAGE.equals(code)) {
            result = 27;
        } else if(ITAU_INFINITE.equals(code)) {
            result = 1201;
        } else if(SANTANDER_SELECT.equals(code)) {
            result = 1302;
        } else {
            throw new AdempiereException("@C_CardProvider_ID@: " + code + " @NotValid@");
        }
        return result;
    }

    private MCard getCard(int cardId) {
        //Do better mapping for other providers or if provider changes cardId Codes
        return MCard.getFromValue(String.valueOf(cardId));
    }

}
