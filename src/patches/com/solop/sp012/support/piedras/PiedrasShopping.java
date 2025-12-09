package com.solop.sp012.support.piedras;

import com.solop.sp012.support.IShopping;
import com.solop.sp012.support.IShoppingDocument;
import com.solop.sp012.support.IShoppingResponse;
import com.solop.sp012.support.piedras.dto.Entrada;
import com.solop.sp012.support.piedras.dto.Salida;
import com.solop.sp012.support.piedras.dto.WsDeclaVtas2PortType;
import com.solop.sp012.support.piedras.dto.WsDeclaVtas2Service;
import com.solop.sp012.support.piedras.dto.constxcont.WsConsxContPortType;
import com.solop.sp012.support.piedras.dto.constxcont.WsConsxContService;
import com.solop.sp012.support.piedras.dto.constxrut.WsConsxRUTPortType;
import com.solop.sp012.support.piedras.dto.constxrut.WsConsxRUTService;
import com.solop.sp012.support.utils.Utils;
import com.solop.sp012.support.utils.contract.IShoppingContract;
import com.solop.sp012.support.utils.contract.IShoppingQueryContracts;
import com.solop.sp012.support.utils.contract.ShoppingContractList;
import com.solop.sp012.support.utils.sector.IShoppingQuerySector;
import com.solop.sp012.support.utils.sector.IShoppingSector;
import com.solop.sp012.support.utils.sector.ShoppingSectorList;
import com.solop.sp012.util.ShoppingMetadata;
import com.solop.sp012.util.ShoppingUtil;
import com.solop.sp013.core.util.ElectronicInvoicingChanges;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.*;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.spin.model.MADAppRegistration;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class PiedrasShopping implements IShopping, IShoppingQueryContracts, IShoppingQuerySector {
    /**	Registration Id	*/
    private int registrationId = 0;
    private static final CLogger logger = CLogger.getCLogger(PiedrasShopping.class);
    private static final String USERNAME_KEY = "user";
    private static final String PASSWORD_KEY = "password";
    private String username;
    private String password;
    private String host;

    private String shoppingNumber;
    private int contractNumber;
    private String chanelCode;

    @Override
    public IShoppingResponse sendDocument(IShoppingDocument document) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        MInvoice invoice = new MInvoice(Env.getCtx(), document.getDocumentId(), null);
        if(!invoice.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsSent)) {
            throw new AdempiereException("@SP012.InvalidDocument@");
        }
        MOrg org = MOrg.get(Env.getCtx(), invoice.getAD_Org_ID());
        MOrgInfo orgInfo = org.getInfo();
        MDocType documentType = MDocType.get(Env.getCtx(), invoice.getC_DocTypeTarget_ID());
        int transactionTypeId = documentType.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TransactionType_ID);
        PO transactionType = new Query(Env.getCtx(), ElectronicInvoicingChanges.SP013_TransactionType, "SP013_TransactionType_ID = ?", null).setParameters(transactionTypeId).first();
        if(transactionType == null) {
            throw new AdempiereException("@SP012.InvalidTransactionType@");
        }
        String serial = getPrefix(invoice.getDocumentNo());
        int validDocumentNo = getDocumentNo(invoice.getDocumentNo());
        // DTO Area
        Entrada entrada = new Entrada();
        Entrada.WsDeclaVtas2 wsDeclaVtas2 = new Entrada.WsDeclaVtas2();
        entrada.getWsDeclaVtas2().add(wsDeclaVtas2);
        Entrada.WsDeclaVtas2.General general = new Entrada.WsDeclaVtas2.General();
        wsDeclaVtas2.setGeneral(general);
        Entrada.WsDeclaVtas2.General.Cab header = new Entrada.WsDeclaVtas2.General.Cab();
        general.setCab(header);
        List<Entrada.WsDeclaVtas2.General.Det> details = wsDeclaVtas2.getGeneral().getDet();
        // Client Identification
        header.setNumeroRUT(orgInfo.getTaxID());
        header.setCodigoShopping(shoppingNumber);
        header.setNumeroContrato(contractNumber);
        header.setCodigoCanal(chanelCode);
        header.setSecuencial(invoice.get_ID());
        // Invoice Data
        if (invoice.getC_POS_ID() > 0) {
            MPOS pos = MPOS.get(invoice.getCtx(), invoice.getC_POS_ID());
            header.setCaja(ShoppingUtil.cutString(pos.getName(), 20));
        } else {
            header.setCaja("Default");
        }
        header.setCodigoCFE(transactionType.get_ValueAsInt("Value"));
        header.setSerieCFE(serial);
        header.setNumeroCFE(validDocumentNo);
        header.setMonedaCFE(invoice.getCurrencyISO());
        header.setFechaEmisionCFE(sdf.format(invoice.getCreated()));
        MCurrency mCurrUYU = MCurrency.get(Env.getCtx(), orgInfo.getFiscalCurrency_ID());
        AtomicReference<BigDecimal> rate = new AtomicReference<>(Env.ONE);
        if (mCurrUYU.get_ID() != invoice.getC_Currency_ID()) {
            if (rate.get().compareTo(Env.ZERO) < 0) {
                throw new AdempiereException("@C_CurrencyRate_ID@ @not.found@");
            }
            rate.set(MConversionRate.getRate(invoice.getC_Currency_ID(), mCurrUYU.getC_Currency_ID(), invoice.getDateInvoiced(), MConversionType.TYPE_SPOT, invoice.getAD_Client_ID(), 0));
            rate.set(rate.get().setScale(2, RoundingMode.HALF_UP));
        }
        //  Unsupported
//        header.TipodeCambio(rate.get().doubleValue());

        header.setTotalMOCIVA(invoice.getGrandTotal(false).doubleValue());
        header.setTotalMNSIVA(invoice.getTotalLines().doubleValue() * rate.get().doubleValue());
        BigDecimal lineTotalNoTax = BigDecimal.ZERO;
        if (invoice.isTaxIncluded()) {
            for (MInvoiceLine invoiceLine : invoice.getLines(true)) {
                lineTotalNoTax = lineTotalNoTax.add(invoiceLine.getLineNetAmt().subtract(invoiceLine.getTaxAmt()));
            }
            header.setTotalMNSIVA(lineTotalNoTax.doubleValue() * rate.get().doubleValue());
        }
        //  TODO: Replace this by real payment method
        header.setCodigoFormaPago("00");
        try {
            GregorianCalendar gregC = new GregorianCalendar();
            header.setFechaTransferencia(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregC));
            header.setHoratransferencia(new SimpleDateFormat("HH:mm").format(new Timestamp(System.currentTimeMillis())));
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        header.setTotal1(invoice.getGrandTotal(false).doubleValue());

        List<MInvoiceLine> invoiceLines = List.of(invoice.getLines(true));
        Map<Integer, List<MInvoiceLine>> mapLines = invoiceLines
                .stream()
                .collect(Collectors.groupingBy(invoiceLine -> ((MProduct) invoiceLine.getM_Product()).get_ValueAsInt("M_Material_Group_ID")));

        mapLines.forEach((materialGroupId, mInvoiceLines) -> {
            PO materialGroup = ShoppingUtil.getProductShoppingCategoryAllocated(materialGroupId, invoice.getAD_Org_ID());
            String materialGroupValue = materialGroup.get_ValueAsString("Value");
            double totalAmt =  mInvoiceLines.stream().mapToDouble(value -> value.getLineNetAmt().doubleValue()).sum();
            if (invoice.isTaxIncluded()) {
                double taxAmt = mInvoiceLines.stream().mapToDouble(value -> value.getTaxAmt().doubleValue()).sum();
                totalAmt -= taxAmt;
            }
            Entrada.WsDeclaVtas2.General.Det detail = new Entrada.WsDeclaVtas2.General.Det();
            details.add(detail);
            detail.setCodRubro(materialGroupValue);
            detail.setContadoMNSIVA(totalAmt * rate.get().doubleValue());
            //  TODO: To be Defined
            detail.setIncluirenPromo("N");
        });

        AtomicReference<List<Salida.Resultado>> responses = new AtomicReference<>();

        Salida.Resultado response;

        try {

            Utils.setDefaultBasicAuthentication(username, password);

            URL url = new URL(host + "/soap/NodumLocales/services/forms/v1.3/wsDeclaVtas2?wsdl");
            WsDeclaVtas2Service service = new WsDeclaVtas2Service(url);
            WsDeclaVtas2PortType port = service.getWsDeclaVtas2Port();

            responses.set(port.procesarAlta(Collections.singletonList(wsDeclaVtas2)));

            response = responses.get().get(0);
            logger.info("status = " + response.getEstado());
            logger.info("message = " + response.getMensaje());
            logger.info("id = " + response.getIdentificador());

        } catch (Exception e) {
            throw new AdempiereException(e);
        }

        AtomicBoolean isError = new AtomicBoolean(0 != (response.getEstado()));

        return new IShoppingResponse() {
            @Override
            public boolean isError() {
                return isError.get();
            }

            @Override
            public String getErrorMessage() {
                return response.getMensaje();
            }

            @Override
            public String getReferenceNo() {
                return String.valueOf(response.getIdentificador());
            }
        };
    }

    private String getPrefix(String documentNo) {
        return documentNo.replaceAll("[^A-Za-z]", "");
    }

    private int getDocumentNo(String documentNo) {
        String newDocumentNo = documentNo.replaceAll("[^0-9]", "");
        newDocumentNo = String.format("%1$" + 7 + "s", newDocumentNo).replace(" ", "0");
        return Integer.parseInt(newDocumentNo);
    }

    @Override
    public String testConnection() {
        return "Ok";
    }

    @Override
    public void setAppRegistrationId(int registrationId) {
        this.registrationId = registrationId;
        MADAppRegistration registration = MADAppRegistration.getById(Env.getCtx(), getAppRegistrationId(), null);
        username = registration.getParameterValue(USERNAME_KEY);
        password = registration.getParameterValue(PASSWORD_KEY);
        if(username == null || password == null) {
            throw new AdempiereException("@SP012.NoUserPassword@");
        }
        host = registration.getHost();
        MOrgInfo info = MOrgInfo.get(Env.getCtx(), registration.getAD_Org_ID(), null);
        contractNumber = info.get_ValueAsInt(ShoppingMetadata.SP012_ContractNumber);
        shoppingNumber = info.get_ValueAsString(ShoppingMetadata.SP012_ShoppingCode);
        chanelCode = info.get_ValueAsString(ShoppingMetadata.SP012_ChannelCode);
    }

    @Override
    public int getAppRegistrationId() {
        return registrationId;
    }

    @Override
    public ShoppingContractList getContracts(String taxId) {

        com.solop.sp012.support.piedras.dto.constxrut.Entrada.WsConsxRUT request = new com.solop.sp012.support.piedras.dto.constxrut.Entrada.WsConsxRUT();

        com.solop.sp012.support.piedras.dto.constxrut.Entrada.WsConsxRUT.General general = new com.solop.sp012.support.piedras.dto.constxrut.Entrada.WsConsxRUT.General();
        request.setGeneral(general);
        com.solop.sp012.support.piedras.dto.constxrut.Entrada.WsConsxRUT.General.Cab cab = new com.solop.sp012.support.piedras.dto.constxrut.Entrada.WsConsxRUT.General.Cab();
        general.setCab(cab);
        cab.setNumeroRUT(taxId);

        Utils.setDefaultBasicAuthentication(username, password);

        WsConsxRUTService service;
        try {
            service = new WsConsxRUTService(new URL(host + "/soap/NodumLocales/services/sim/v1.3/wsConsxRUT?wsdl"));
        } catch (MalformedURLException e) {
            throw new AdempiereException(e);
        }
        WsConsxRUTPortType port = service.getWsConsxRUTPort();

        AtomicReference<ShoppingContractList> contractList = new AtomicReference<>();
        try {
            List<com.solop.sp012.support.piedras.dto.constxrut.Salida.RWsConsxRUT> response = port.simular(List.of(request));
            List<IShoppingContract> shoppingContractResponses = new ArrayList<>();
            response.forEach(rWsConsxRUT -> {
                String currentRut = rWsConsxRUT.getRGeneral().getRCab().getRNumeroRUT();
                rWsConsxRUT.getRGeneral().getRDet().forEach(rDet -> {
                    IShoppingContract contractResponse = new IShoppingContract() {
                        @Override
                        public String getTaxId() {
                            return currentRut;
                        }

                        @Override
                        public String getShoppingCode() {
                            return rDet.getRCodigoShopping();
                        }

                        @Override
                        public String getShoppingName() {
                            return rDet.getRNombreShopping();
                        }

                        @Override
                        public int getContractNumber() {
                            return rDet.getRNumerodeContrato();
                        }

                        @Override
                        public String getContractDescription() {
                            return rDet.getRDescripcioncontrato();
                        }

                        @Override
                        public String getStore() {
                            return rDet.getRLocales();
                        }

                        @Override
                        public String getPublicationStage() {
                            return rDet.getREtapaPublicacion();
                        }
                    };

                    shoppingContractResponses.add(contractResponse);

                    logger.info("contractResponse.getRut() - " + contractResponse.getTaxId());
                    logger.info("contractResponse.getShoppingCode() - " + contractResponse.getShoppingCode());
                    logger.info("contractResponse.getShoppingName() - " + contractResponse.getShoppingName());
                    logger.info("contractResponse.getContractNumber() - " + contractResponse.getContractNumber());
                    logger.info("contractResponse.getContractDescription() - " + contractResponse.getContractDescription());
                    logger.info("contractResponse.getStore() - " + contractResponse.getStore());
                    logger.info("contractResponse.getPublicationStage() - " + contractResponse.getPublicationStage());
                });
            });
            contractList.set(new ShoppingContractList(false, null, shoppingContractResponses));
        } catch (Exception e) {
            contractList.set(new ShoppingContractList(true, e.getMessage(), null));
        }

        return contractList.get();
    }

    @Override
    public ShoppingSectorList getSectors(String taxId, String shoppingCode, int contractNumber) {
        com.solop.sp012.support.piedras.dto.constxcont.Entrada.WsConsxCont request = new com.solop.sp012.support.piedras.dto.constxcont.Entrada.WsConsxCont();
        com.solop.sp012.support.piedras.dto.constxcont.Entrada.WsConsxCont.General general = new com.solop.sp012.support.piedras.dto.constxcont.Entrada.WsConsxCont.General();
        request.setGeneral(general);
        com.solop.sp012.support.piedras.dto.constxcont.Entrada.WsConsxCont.General.Cab cab = new com.solop.sp012.support.piedras.dto.constxcont.Entrada.WsConsxCont.General.Cab();
        general.setCab(cab);
        cab.setNumeroRUT(taxId);
        cab.setCodigoShopping(shoppingCode);
        cab.setNumeroContrato(contractNumber);

        Utils.setDefaultBasicAuthentication(username, password);

        WsConsxContService service;
        try {
            service = new WsConsxContService(new URL(host + "/soap/NodumLocales/services/sim/v1.3/wsConsxCont?wsdl"));
        } catch (MalformedURLException e) {
            throw new AdempiereException(e);
        }
        WsConsxContPortType port = service.getWsConsxContPort();

        AtomicReference<ShoppingSectorList> sectorList = new AtomicReference<>();
        try {
            List<com.solop.sp012.support.piedras.dto.constxcont.Salida.RWsConsxCont> response = port.simular(List.of(request));
            List<IShoppingSector> shoppingSectorsResponse = new ArrayList<>();
            response.forEach(rWsConsxCont -> {
                String currentRut = rWsConsxCont.getRGeneral().getRCab().getRNumeroRUT();
                String currentShoppingCode = rWsConsxCont.getRGeneral().getRCab().getRCodigoShopping();
                int currentContractNumber = rWsConsxCont.getRGeneral().getRCab().getRNumeroContrato();
                rWsConsxCont.getRGeneral().getRDetR().forEach(rDetR -> {
                    IShoppingSector sectorResponse = new IShoppingSector() {
                        @Override
                        public String getRut() {
                            return currentRut;
                        }

                        @Override
                        public String getShoppingCode() {
                            return currentShoppingCode;
                        }

                        @Override
                        public int getContractNumber() {
                            return currentContractNumber;
                        }

                        @Override
                        public String getChannelCode() {
                            return rDetR.getRCodigoCanal();
                        }

                        @Override
                        public String getChannelName() {
                            return rDetR.getRNombreCanal();
                        }

                        @Override
                        public String getSectorCode() {
                            return rDetR.getRCodigoRubro();
                        }

                        @Override
                        public String getSectorName() {
                            return rDetR.getRNombreRubro();
                        }
                    };

                    shoppingSectorsResponse.add(sectorResponse);

                    logger.info("sectorResponse.getRut() - " + sectorResponse.getRut());
                    logger.info(" sectorResponse.getShoppingCode()- " + sectorResponse.getShoppingCode());
                    logger.info("sectorResponse.getContractNumber() - " + sectorResponse.getContractNumber());
                    logger.info("sectorResponse.getChannelCode() - " + sectorResponse.getChannelCode());
                    logger.info("sectorResponse.getChannelName() - " + sectorResponse.getChannelName());
                    logger.info("sectorResponse.getSectorCode() - " + sectorResponse.getSectorCode());
                    logger.info("sectorResponse.getSectorName() - " + sectorResponse.getSectorName());
                });
            });
            sectorList.set(new ShoppingSectorList(false, null, shoppingSectorsResponse));
        } catch (Exception e) {
            sectorList.set(new ShoppingSectorList(true, e.getMessage(), null));
        }

        return sectorList.get();
    }
}
