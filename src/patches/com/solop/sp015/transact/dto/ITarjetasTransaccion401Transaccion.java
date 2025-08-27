
package com.solop.sp015.transact.dto;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para ITarjetasTransaccion_401.Transaccion complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="ITarjetasTransaccion_401.Transaccion">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Comportamiento" type="{http://schemas.datacontract.org/2004/07/TransActV4ConcentradorWS.TransActV4Concentrador}ITarjetasTransaccion_401.Comportamiento" minOccurs="0"/>
 *         &lt;element name="Configuracion" type="{http://schemas.datacontract.org/2004/07/TransActV4ConcentradorWS.TransActV4Concentrador}ITarjetasTransaccion_401.Configuracion" minOccurs="0"/>
 *         &lt;element name="EmisorId" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="EmpCod" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="EmpHASH" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Extendida" type="{http://schemas.datacontract.org/2004/07/TransActV4ConcentradorWS.TransActV4Concentrador}ITarjetasTransaccion_401.TransaccionExtendida" minOccurs="0"/>
 *         &lt;element name="FacturaConsumidorFinal" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="FacturaMonto" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="FacturaMontoGravado" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="FacturaMontoIVA" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="FacturaNro" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="MonedaISO" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Monto" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="MontoCashBack" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="MontoPropina" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="MultiEmp" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="Operacion" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="TarjetaAlimentacion" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="TarjetaId" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="TarjetaTipo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="TermCod" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="TicketOriginal" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ITarjetasTransaccion_401.Transaccion", namespace = "http://schemas.datacontract.org/2004/07/TransActV4ConcentradorWS.TransActV4Concentrador", propOrder = {
    "comportamiento",
    "configuracion",
    "emisorId",
    "empCod",
    "empHASH",
    "extendida",
    "facturaConsumidorFinal",
    "facturaMonto",
    "facturaMontoGravado",
    "facturaMontoIVA",
    "facturaNro",
    "monedaISO",
    "monto",
    "montoCashBack",
    "montoPropina",
    "multiEmp",
    "operacion",
    "tarjetaAlimentacion",
    "tarjetaId",
    "tarjetaTipo",
    "termCod",
    "ticketOriginal"
})
public class ITarjetasTransaccion401Transaccion {

    @XmlElementRef(name = "Comportamiento", namespace = "http://schemas.datacontract.org/2004/07/TransActV4ConcentradorWS.TransActV4Concentrador", type = JAXBElement.class, required = false)
    protected JAXBElement<ITarjetasTransaccion401Comportamiento> comportamiento;
    @XmlElementRef(name = "Configuracion", namespace = "http://schemas.datacontract.org/2004/07/TransActV4ConcentradorWS.TransActV4Concentrador", type = JAXBElement.class, required = false)
    protected JAXBElement<ITarjetasTransaccion401Configuracion> configuracion;
    @XmlElement(name = "EmisorId")
    protected Integer emisorId;
    @XmlElement(name = "EmpCod", required = true, nillable = true)
    protected String empCod;
    @XmlElement(name = "EmpHASH", required = true, nillable = true)
    protected String empHASH;
    @XmlElementRef(name = "Extendida", namespace = "http://schemas.datacontract.org/2004/07/TransActV4ConcentradorWS.TransActV4Concentrador", type = JAXBElement.class, required = false)
    protected JAXBElement<ITarjetasTransaccion401TransaccionExtendida> extendida;
    @XmlElement(name = "FacturaConsumidorFinal")
    protected boolean facturaConsumidorFinal;
    @XmlElement(name = "FacturaMonto")
    protected Double facturaMonto;
    @XmlElement(name = "FacturaMontoGravado")
    protected Double facturaMontoGravado;
    @XmlElement(name = "FacturaMontoIVA")
    protected Double facturaMontoIVA;
    @XmlElement(name = "FacturaNro")
    protected Double facturaNro;
    @XmlElement(name = "MonedaISO")
    protected String monedaISO;
    @XmlElement(name = "Monto")
    protected double monto;
    @XmlElementRef(name = "MontoCashBack", namespace = "http://schemas.datacontract.org/2004/07/TransActV4ConcentradorWS.TransActV4Concentrador", type = JAXBElement.class, required = false)
    protected JAXBElement<Double> montoCashBack;
    @XmlElementRef(name = "MontoPropina", namespace = "http://schemas.datacontract.org/2004/07/TransActV4ConcentradorWS.TransActV4Concentrador", type = JAXBElement.class, required = false)
    protected JAXBElement<Double> montoPropina;
    @XmlElementRef(name = "MultiEmp", namespace = "http://schemas.datacontract.org/2004/07/TransActV4ConcentradorWS.TransActV4Concentrador", type = JAXBElement.class, required = false)
    protected JAXBElement<Integer> multiEmp;
    @XmlElement(name = "Operacion", required = true, nillable = true)
    protected String operacion;
    @XmlElementRef(name = "TarjetaAlimentacion", namespace = "http://schemas.datacontract.org/2004/07/TransActV4ConcentradorWS.TransActV4Concentrador", type = JAXBElement.class, required = false)
    protected JAXBElement<Boolean> tarjetaAlimentacion;
    @XmlElement(name = "TarjetaId")
    protected Integer tarjetaId;
    @XmlElement(name = "TarjetaTipo")
    protected String tarjetaTipo;
    @XmlElement(name = "TermCod", required = true, nillable = true)
    protected String termCod;
    @XmlElement(name = "TicketOriginal")
    protected Integer ticketOriginal;

    /**
     * Obtiene el valor de la propiedad comportamiento.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ITarjetasTransaccion401Comportamiento }{@code >}
     *     
     */
    public JAXBElement<ITarjetasTransaccion401Comportamiento> getComportamiento() {
        return comportamiento;
    }

    /**
     * Define el valor de la propiedad comportamiento.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ITarjetasTransaccion401Comportamiento }{@code >}
     *     
     */
    public void setComportamiento(JAXBElement<ITarjetasTransaccion401Comportamiento> value) {
        this.comportamiento = value;
    }

    /**
     * Obtiene el valor de la propiedad configuracion.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ITarjetasTransaccion401Configuracion }{@code >}
     *     
     */
    public JAXBElement<ITarjetasTransaccion401Configuracion> getConfiguracion() {
        return configuracion;
    }

    /**
     * Define el valor de la propiedad configuracion.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ITarjetasTransaccion401Configuracion }{@code >}
     *     
     */
    public void setConfiguracion(JAXBElement<ITarjetasTransaccion401Configuracion> value) {
        this.configuracion = value;
    }

    /**
     * Obtiene el valor de la propiedad emisorId.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public Integer getEmisorId() {
        return emisorId;
    }

    /**
     * Define el valor de la propiedad emisorId.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setEmisorId(Integer value) {
        this.emisorId = value;
    }

    /**
     * Obtiene el valor de la propiedad empCod.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEmpCod() {
        return empCod;
    }

    /**
     * Define el valor de la propiedad empCod.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEmpCod(String value) {
        this.empCod = value;
    }

    /**
     * Obtiene el valor de la propiedad empHASH.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEmpHASH() {
        return empHASH;
    }

    /**
     * Define el valor de la propiedad empHASH.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEmpHASH(String value) {
        this.empHASH = value;
    }

    /**
     * Obtiene el valor de la propiedad extendida.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ITarjetasTransaccion401TransaccionExtendida }{@code >}
     *     
     */
    public JAXBElement<ITarjetasTransaccion401TransaccionExtendida> getExtendida() {
        return extendida;
    }

    /**
     * Define el valor de la propiedad extendida.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ITarjetasTransaccion401TransaccionExtendida }{@code >}
     *     
     */
    public void setExtendida(JAXBElement<ITarjetasTransaccion401TransaccionExtendida> value) {
        this.extendida = value;
    }

    /**
     * Obtiene el valor de la propiedad facturaConsumidorFinal.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public boolean getFacturaConsumidorFinal() {
        return facturaConsumidorFinal;
    }

    /**
     * Define el valor de la propiedad facturaConsumidorFinal.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setFacturaConsumidorFinal(boolean value) {
        this.facturaConsumidorFinal = value;
    }

    /**
     * Obtiene el valor de la propiedad facturaMonto.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public Double getFacturaMonto() {
        return facturaMonto;
    }

    /**
     * Define el valor de la propiedad facturaMonto.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public void setFacturaMonto(Double value) {
        this.facturaMonto = value;
    }

    /**
     * Obtiene el valor de la propiedad facturaMontoGravado.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public Double getFacturaMontoGravado() {
        return facturaMontoGravado;
    }

    /**
     * Define el valor de la propiedad facturaMontoGravado.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public void setFacturaMontoGravado(Double value) {
        this.facturaMontoGravado = value;
    }

    /**
     * Obtiene el valor de la propiedad facturaMontoIVA.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public Double getFacturaMontoIVA() {
        return facturaMontoIVA;
    }

    /**
     * Define el valor de la propiedad facturaMontoIVA.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public void setFacturaMontoIVA(Double value) {
        this.facturaMontoIVA = value;
    }

    /**
     * Obtiene el valor de la propiedad facturaNro.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public Double getFacturaNro() {
        return facturaNro;
    }

    /**
     * Define el valor de la propiedad facturaNro.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public void setFacturaNro(Double value) {
        this.facturaNro = value;
    }

    /**
     * Obtiene el valor de la propiedad monedaISO.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public String getMonedaISO() {
        return monedaISO;
    }

    /**
     * Define el valor de la propiedad monedaISO.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setMonedaISO(String value) {
        this.monedaISO = value;
    }

    /**
     * Obtiene el valor de la propiedad monto.
     * 
     */
    public double getMonto() {
        return monto;
    }

    /**
     * Define el valor de la propiedad monto.
     * 
     */
    public void setMonto(double value) {
        this.monto = value;
    }

    /**
     * Obtiene el valor de la propiedad montoCashBack.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public JAXBElement<Double> getMontoCashBack() {
        return montoCashBack;
    }

    /**
     * Define el valor de la propiedad montoCashBack.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public void setMontoCashBack(JAXBElement<Double> value) {
        this.montoCashBack = value;
    }

    /**
     * Obtiene el valor de la propiedad montoPropina.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public JAXBElement<Double> getMontoPropina() {
        return montoPropina;
    }

    /**
     * Define el valor de la propiedad montoPropina.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Double }{@code >}
     *     
     */
    public void setMontoPropina(JAXBElement<Double> value) {
        this.montoPropina = value;
    }

    /**
     * Obtiene el valor de la propiedad multiEmp.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public JAXBElement<Integer> getMultiEmp() {
        return multiEmp;
    }

    /**
     * Define el valor de la propiedad multiEmp.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setMultiEmp(JAXBElement<Integer> value) {
        this.multiEmp = value;
    }

    /**
     * Obtiene el valor de la propiedad operacion.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOperacion() {
        return operacion;
    }

    /**
     * Define el valor de la propiedad operacion.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOperacion(String value) {
        this.operacion = value;
    }

    /**
     * Obtiene el valor de la propiedad tarjetaAlimentacion.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public JAXBElement<Boolean> getTarjetaAlimentacion() {
        return tarjetaAlimentacion;
    }

    /**
     * Define el valor de la propiedad tarjetaAlimentacion.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     *     
     */
    public void setTarjetaAlimentacion(JAXBElement<Boolean> value) {
        this.tarjetaAlimentacion = value;
    }

    /**
     * Obtiene el valor de la propiedad tarjetaId.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public Integer getTarjetaId() {
        return tarjetaId;
    }

    /**
     * Define el valor de la propiedad tarjetaId.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setTarjetaId(Integer value) {
        this.tarjetaId = value;
    }

    /**
     * Obtiene el valor de la propiedad tarjetaTipo.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public String getTarjetaTipo() {
        return tarjetaTipo;
    }

    /**
     * Define el valor de la propiedad tarjetaTipo.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setTarjetaTipo(String value) {
        this.tarjetaTipo = value;
    }

    /**
     * Obtiene el valor de la propiedad termCod.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTermCod() {
        return termCod;
    }

    /**
     * Define el valor de la propiedad termCod.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTermCod(String value) {
        this.termCod = value;
    }

    /**
     * Obtiene el valor de la propiedad ticketOriginal.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public Integer getTicketOriginal() {
        return ticketOriginal;
    }

    /**
     * Define el valor de la propiedad ticketOriginal.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Integer }{@code >}
     *     
     */
    public void setTicketOriginal(Integer value) {
        this.ticketOriginal = value;
    }

}
