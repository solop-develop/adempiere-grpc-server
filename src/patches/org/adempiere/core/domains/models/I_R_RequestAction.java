/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/
package org.adempiere.core.domains.models;

import org.compiere.model.MTable;
import org.compiere.util.KeyNamePair;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Generated Interface for R_RequestAction
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_R_RequestAction 
{

    /** TableName=R_RequestAction */
    public static final String Table_Name = "R_RequestAction";

    /** AD_Table_ID=418 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 7 - System - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(7);

    /** Load Meta Data */

    /** Column name A_Asset_ID */
    public static final String COLUMNNAME_A_Asset_ID = "A_Asset_ID";

	/** Set Fixed Asset.
	  * Fixed Asset used internally or by customers
	  */
	public void setA_Asset_ID (int A_Asset_ID);

	/** Get Fixed Asset.
	  * Fixed Asset used internally or by customers
	  */
	public int getA_Asset_ID();

	public I_A_Asset getA_Asset() throws RuntimeException;

    /** Column name A_AssetTo_ID */
    public static final String COLUMNNAME_A_AssetTo_ID = "A_AssetTo_ID";

	/** Set Fixed Asset To.
	  * Fixed Asset used internally or by customers
	  */
	public void setA_AssetTo_ID (int A_AssetTo_ID);

	/** Get Fixed Asset To.
	  * Fixed Asset used internally or by customers
	  */
	public int getA_AssetTo_ID();

	public I_A_Asset getA_AssetTo() throws RuntimeException;

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();

    /** Column name AD_Role_ID */
    public static final String COLUMNNAME_AD_Role_ID = "AD_Role_ID";

	/** Set Role.
	  * Responsibility Role
	  */
	public void setAD_Role_ID (int AD_Role_ID);

	/** Get Role.
	  * Responsibility Role
	  */
	public int getAD_Role_ID();

	public I_AD_Role getAD_Role() throws RuntimeException;

    /** Column name AD_RoleTo_ID */
    public static final String COLUMNNAME_AD_RoleTo_ID = "AD_RoleTo_ID";

	/** Set Role To.
	  * Responsibility Role To
	  */
	public void setAD_RoleTo_ID (int AD_RoleTo_ID);

	/** Get Role To.
	  * Responsibility Role To
	  */
	public int getAD_RoleTo_ID();

	public I_AD_Role getAD_RoleTo() throws RuntimeException;

    /** Column name AD_User_ID */
    public static final String COLUMNNAME_AD_User_ID = "AD_User_ID";

	/** Set User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User_ID (int AD_User_ID);

	/** Get User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID();

	public I_AD_User getAD_User() throws RuntimeException;

    /** Column name AD_UserTo_ID */
    public static final String COLUMNNAME_AD_UserTo_ID = "AD_UserTo_ID";

	/** Set User/Contact To.
	  * User within the system - Internal or Business Partner Contact
	  */
	public void setAD_UserTo_ID (int AD_UserTo_ID);

	/** Get User/Contact To.
	  * User within the system - Internal or Business Partner Contact
	  */
	public int getAD_UserTo_ID();

	public I_AD_User getAD_UserTo() throws RuntimeException;

    /** Column name C_Activity_ID */
    public static final String COLUMNNAME_C_Activity_ID = "C_Activity_ID";

	/** Set Activity.
	  * Business Activity
	  */
	public void setC_Activity_ID (int C_Activity_ID);

	/** Get Activity.
	  * Business Activity
	  */
	public int getC_Activity_ID();

	public I_C_Activity getC_Activity() throws RuntimeException;

    /** Column name C_ActivityTo_ID */
    public static final String COLUMNNAME_C_ActivityTo_ID = "C_ActivityTo_ID";

	/** Set Activity To.
	  * Business Activity To
	  */
	public void setC_ActivityTo_ID (int C_ActivityTo_ID);

	/** Get Activity To.
	  * Business Activity To
	  */
	public int getC_ActivityTo_ID();

	public I_C_Activity getC_ActivityTo() throws RuntimeException;

    /** Column name C_BPartner_ID */
    public static final String COLUMNNAME_C_BPartner_ID = "C_BPartner_ID";

	/** Set Business Partner .
	  * Identifies a Business Partner
	  */
	public void setC_BPartner_ID (int C_BPartner_ID);

	/** Get Business Partner .
	  * Identifies a Business Partner
	  */
	public int getC_BPartner_ID();

	public I_C_BPartner getC_BPartner() throws RuntimeException;

    /** Column name C_BPartnerTo_ID */
    public static final String COLUMNNAME_C_BPartnerTo_ID = "C_BPartnerTo_ID";

	/** Set Business Partner To.
	  * Identifies a Business Partner
	  */
	public void setC_BPartnerTo_ID (int C_BPartnerTo_ID);

	/** Get Business Partner To.
	  * Identifies a Business Partner
	  */
	public int getC_BPartnerTo_ID();

	public I_C_BPartner getC_BPartnerTo() throws RuntimeException;

    /** Column name C_Invoice_ID */
    public static final String COLUMNNAME_C_Invoice_ID = "C_Invoice_ID";

	/** Set Invoice.
	  * Invoice Identifier
	  */
	public void setC_Invoice_ID (int C_Invoice_ID);

	/** Get Invoice.
	  * Invoice Identifier
	  */
	public int getC_Invoice_ID();

	public I_C_Invoice getC_Invoice() throws RuntimeException;

    /** Column name C_InvoiceTo_ID */
    public static final String COLUMNNAME_C_InvoiceTo_ID = "C_InvoiceTo_ID";

	/** Set Invoice To.
	  * Invoice Identifier
	  */
	public void setC_InvoiceTo_ID (int C_InvoiceTo_ID);

	/** Get Invoice To.
	  * Invoice Identifier
	  */
	public int getC_InvoiceTo_ID();

	public I_C_Invoice getC_InvoiceTo() throws RuntimeException;

    /** Column name ConfidentialType */
    public static final String COLUMNNAME_ConfidentialType = "ConfidentialType";

	/** Set Confidentiality.
	  * Type of Confidentiality
	  */
	public void setConfidentialType (String ConfidentialType);

	/** Get Confidentiality.
	  * Type of Confidentiality
	  */
	public String getConfidentialType();

    /** Column name ConfidentialTypeTo */
    public static final String COLUMNNAME_ConfidentialTypeTo = "ConfidentialTypeTo";

	/** Set Confidentiality To.
	  * Type of Confidentiality
	  */
	public void setConfidentialTypeTo (String ConfidentialTypeTo);

	/** Get Confidentiality To.
	  * Type of Confidentiality
	  */
	public String getConfidentialTypeTo();

    /** Column name C_Order_ID */
    public static final String COLUMNNAME_C_Order_ID = "C_Order_ID";

	/** Set Order.
	  * Order
	  */
	public void setC_Order_ID (int C_Order_ID);

	/** Get Order.
	  * Order
	  */
	public int getC_Order_ID();

	public I_C_Order getC_Order() throws RuntimeException;

    /** Column name C_OrderTo_ID */
    public static final String COLUMNNAME_C_OrderTo_ID = "C_OrderTo_ID";

	/** Set Order To.
	  * Order To
	  */
	public void setC_OrderTo_ID (int C_OrderTo_ID);

	/** Get Order To.
	  * Order To
	  */
	public int getC_OrderTo_ID();

	public I_C_Order getC_OrderTo() throws RuntimeException;

    /** Column name C_Payment_ID */
    public static final String COLUMNNAME_C_Payment_ID = "C_Payment_ID";

	/** Set Payment.
	  * Payment identifier
	  */
	public void setC_Payment_ID (int C_Payment_ID);

	/** Get Payment.
	  * Payment identifier
	  */
	public int getC_Payment_ID();

	public I_C_Payment getC_Payment() throws RuntimeException;

    /** Column name C_PaymentTo_ID */
    public static final String COLUMNNAME_C_PaymentTo_ID = "C_PaymentTo_ID";

	/** Set Payment To.
	  * Payment identifier
	  */
	public void setC_PaymentTo_ID (int C_PaymentTo_ID);

	/** Get Payment To.
	  * Payment identifier
	  */
	public int getC_PaymentTo_ID();

	public I_C_Payment getC_PaymentTo() throws RuntimeException;

    /** Column name C_Project_ID */
    public static final String COLUMNNAME_C_Project_ID = "C_Project_ID";

	/** Set Project.
	  * Financial Project
	  */
	public void setC_Project_ID (int C_Project_ID);

	/** Get Project.
	  * Financial Project
	  */
	public int getC_Project_ID();

	public I_C_Project getC_Project() throws RuntimeException;

    /** Column name C_ProjectTo_ID */
    public static final String COLUMNNAME_C_ProjectTo_ID = "C_ProjectTo_ID";

	/** Set Project To.
	  * Financial Project
	  */
	public void setC_ProjectTo_ID (int C_ProjectTo_ID);

	/** Get Project To.
	  * Financial Project
	  */
	public int getC_ProjectTo_ID();

	public I_C_Project getC_ProjectTo() throws RuntimeException;

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name DateCompletePlan */
    public static final String COLUMNNAME_DateCompletePlan = "DateCompletePlan";

	/** Set Complete Plan.
	  * Planned Completion Date
	  */
	public void setDateCompletePlan (Timestamp DateCompletePlan);

	/** Get Complete Plan.
	  * Planned Completion Date
	  */
	public Timestamp getDateCompletePlan();

    /** Column name DateCompletePlanTo */
    public static final String COLUMNNAME_DateCompletePlanTo = "DateCompletePlanTo";

	/** Set Complete Plan To.
	  * Planned Completion Date
	  */
	public void setDateCompletePlanTo (Timestamp DateCompletePlanTo);

	/** Get Complete Plan To.
	  * Planned Completion Date
	  */
	public Timestamp getDateCompletePlanTo();

    /** Column name DateNextAction */
    public static final String COLUMNNAME_DateNextAction = "DateNextAction";

	/** Set Date next action.
	  * Date that this request should be acted on
	  */
	public void setDateNextAction (Timestamp DateNextAction);

	/** Get Date next action.
	  * Date that this request should be acted on
	  */
	public Timestamp getDateNextAction();

    /** Column name DateNextActionTo */
    public static final String COLUMNNAME_DateNextActionTo = "DateNextActionTo";

	/** Set Date next action To.
	  * Date that this request should be acted on
	  */
	public void setDateNextActionTo (Timestamp DateNextActionTo);

	/** Get Date next action To.
	  * Date that this request should be acted on
	  */
	public Timestamp getDateNextActionTo();

    /** Column name DateStartPlan */
    public static final String COLUMNNAME_DateStartPlan = "DateStartPlan";

	/** Set Start Plan.
	  * Planned Start Date
	  */
	public void setDateStartPlan (Timestamp DateStartPlan);

	/** Get Start Plan.
	  * Planned Start Date
	  */
	public Timestamp getDateStartPlan();

    /** Column name DateStartPlanTo */
    public static final String COLUMNNAME_DateStartPlanTo = "DateStartPlanTo";

	/** Set Start Plan To.
	  * Planned Start Date
	  */
	public void setDateStartPlanTo (Timestamp DateStartPlanTo);

	/** Get Start Plan To.
	  * Planned Start Date
	  */
	public Timestamp getDateStartPlanTo();

    /** Column name DurationInMillis */
    public static final String COLUMNNAME_DurationInMillis = "DurationInMillis";

	/** Set Duration in Milliseconds	  */
	public void setDurationInMillis (BigDecimal DurationInMillis);

	/** Get Duration in Milliseconds	  */
	public BigDecimal getDurationInMillis();

    /** Column name EndDate */
    public static final String COLUMNNAME_EndDate = "EndDate";

	/** Set End Date.
	  * Last effective date (inclusive)
	  */
	public void setEndDate (Timestamp EndDate);

	/** Get End Date.
	  * Last effective date (inclusive)
	  */
	public Timestamp getEndDate();

    /** Column name EndDateTo */
    public static final String COLUMNNAME_EndDateTo = "EndDateTo";

	/** Set End Date To.
	  * Last effective date (inclusive)
	  */
	public void setEndDateTo (Timestamp EndDateTo);

	/** Get End Date To.
	  * Last effective date (inclusive)
	  */
	public Timestamp getEndDateTo();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name IsEscalated */
    public static final String COLUMNNAME_IsEscalated = "IsEscalated";

	/** Set Escalated.
	  * This request has been escalated
	  */
	public void setIsEscalated (String IsEscalated);

	/** Get Escalated.
	  * This request has been escalated
	  */
	public String getIsEscalated();

    /** Column name IsEscalatedTo */
    public static final String COLUMNNAME_IsEscalatedTo = "IsEscalatedTo";

	/** Set Escalated To.
	  * This request has been escalated
	  */
	public void setIsEscalatedTo (boolean IsEscalatedTo);

	/** Get Escalated To.
	  * This request has been escalated
	  */
	public boolean isEscalatedTo();

    /** Column name IsInvoiced */
    public static final String COLUMNNAME_IsInvoiced = "IsInvoiced";

	/** Set Invoiced.
	  * Is this invoiced?
	  */
	public void setIsInvoiced (String IsInvoiced);

	/** Get Invoiced.
	  * Is this invoiced?
	  */
	public String getIsInvoiced();

    /** Column name IsInvoicedTo */
    public static final String COLUMNNAME_IsInvoicedTo = "IsInvoicedTo";

	/** Set Invoiced To.
	  * Is this invoiced?
	  */
	public void setIsInvoicedTo (boolean IsInvoicedTo);

	/** Get Invoiced To.
	  * Is this invoiced?
	  */
	public boolean isInvoicedTo();

    /** Column name IsSelfService */
    public static final String COLUMNNAME_IsSelfService = "IsSelfService";

	/** Set Self-Service.
	  * This is a Self-Service entry or this entry can be changed via Self-Service
	  */
	public void setIsSelfService (String IsSelfService);

	/** Get Self-Service.
	  * This is a Self-Service entry or this entry can be changed via Self-Service
	  */
	public String getIsSelfService();

    /** Column name IsSelfServiceTo */
    public static final String COLUMNNAME_IsSelfServiceTo = "IsSelfServiceTo";

	/** Set Self-Service To.
	  * This is a Self-Service entry or this entry can be changed via Self-Service
	  */
	public void setIsSelfServiceTo (boolean IsSelfServiceTo);

	/** Get Self-Service To.
	  * This is a Self-Service entry or this entry can be changed via Self-Service
	  */
	public boolean isSelfServiceTo();

    /** Column name M_InOut_ID */
    public static final String COLUMNNAME_M_InOut_ID = "M_InOut_ID";

	/** Set Shipment/Receipt.
	  * Material Shipment Document
	  */
	public void setM_InOut_ID (int M_InOut_ID);

	/** Get Shipment/Receipt.
	  * Material Shipment Document
	  */
	public int getM_InOut_ID();

	public I_M_InOut getM_InOut() throws RuntimeException;

    /** Column name M_InOutTo_ID */
    public static final String COLUMNNAME_M_InOutTo_ID = "M_InOutTo_ID";

	/** Set Shipment/Receipt To.
	  * Material Shipment Document
	  */
	public void setM_InOutTo_ID (int M_InOutTo_ID);

	/** Get Shipment/Receipt To.
	  * Material Shipment Document
	  */
	public int getM_InOutTo_ID();

	public I_M_InOut getM_InOutTo() throws RuntimeException;

    /** Column name M_Product_ID */
    public static final String COLUMNNAME_M_Product_ID = "M_Product_ID";

	/** Set Product.
	  * Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID);

	/** Get Product.
	  * Product, Service, Item
	  */
	public int getM_Product_ID();

	public I_M_Product getM_Product() throws RuntimeException;

    /** Column name M_ProductSpent_ID */
    public static final String COLUMNNAME_M_ProductSpent_ID = "M_ProductSpent_ID";

	/** Set Product Used.
	  * Product/Resource/Service used in Request
	  */
	public void setM_ProductSpent_ID (int M_ProductSpent_ID);

	/** Get Product Used.
	  * Product/Resource/Service used in Request
	  */
	public int getM_ProductSpent_ID();

	public I_M_Product getM_ProductSpent() throws RuntimeException;

    /** Column name M_ProductTo_ID */
    public static final String COLUMNNAME_M_ProductTo_ID = "M_ProductTo_ID";

	/** Set Product To.
	  * Product, Service, Item
	  */
	public void setM_ProductTo_ID (int M_ProductTo_ID);

	/** Get Product To.
	  * Product, Service, Item
	  */
	public int getM_ProductTo_ID();

	public I_M_Product getM_ProductTo() throws RuntimeException;

    /** Column name M_RMA_ID */
    public static final String COLUMNNAME_M_RMA_ID = "M_RMA_ID";

	/** Set RMA.
	  * Return Material Authorization
	  */
	public void setM_RMA_ID (int M_RMA_ID);

	/** Get RMA.
	  * Return Material Authorization
	  */
	public int getM_RMA_ID();

	public I_M_RMA getM_RMA() throws RuntimeException;

    /** Column name M_RMATo_ID */
    public static final String COLUMNNAME_M_RMATo_ID = "M_RMATo_ID";

	/** Set RMA To.
	  * Return Material Authorization
	  */
	public void setM_RMATo_ID (int M_RMATo_ID);

	/** Get RMA To.
	  * Return Material Authorization
	  */
	public int getM_RMATo_ID();

	public I_M_RMA getM_RMATo() throws RuntimeException;

    /** Column name NullColumns */
    public static final String COLUMNNAME_NullColumns = "NullColumns";

	/** Set Null Columns.
	  * Columns with NULL value
	  */
	public void setNullColumns (String NullColumns);

	/** Get Null Columns.
	  * Columns with NULL value
	  */
	public String getNullColumns();

    /** Column name Priority */
    public static final String COLUMNNAME_Priority = "Priority";

	/** Set Priority.
	  * Indicates if this request is of a high, medium or low priority.
	  */
	public void setPriority (String Priority);

	/** Get Priority.
	  * Indicates if this request is of a high, medium or low priority.
	  */
	public String getPriority();

    /** Column name PriorityTo */
    public static final String COLUMNNAME_PriorityTo = "PriorityTo";

	/** Set Priority To.
	  * Indicates if this request is of a high, medium or low priority.
	  */
	public void setPriorityTo (String PriorityTo);

	/** Get Priority To.
	  * Indicates if this request is of a high, medium or low priority.
	  */
	public String getPriorityTo();

    /** Column name PriorityUser */
    public static final String COLUMNNAME_PriorityUser = "PriorityUser";

	/** Set User Importance.
	  * Priority of the issue for the User
	  */
	public void setPriorityUser (String PriorityUser);

	/** Get User Importance.
	  * Priority of the issue for the User
	  */
	public String getPriorityUser();

    /** Column name PriorityUserTo */
    public static final String COLUMNNAME_PriorityUserTo = "PriorityUserTo";

	/** Set User Importance To.
	  * Priority of the issue for the User
	  */
	public void setPriorityUserTo (String PriorityUserTo);

	/** Get User Importance To.
	  * Priority of the issue for the User
	  */
	public String getPriorityUserTo();

    /** Column name QtyInvoiced */
    public static final String COLUMNNAME_QtyInvoiced = "QtyInvoiced";

	/** Set Quantity Invoiced.
	  * Invoiced Quantity
	  */
	public void setQtyInvoiced (BigDecimal QtyInvoiced);

	/** Get Quantity Invoiced.
	  * Invoiced Quantity
	  */
	public BigDecimal getQtyInvoiced();

    /** Column name QtyInvoicedTo */
    public static final String COLUMNNAME_QtyInvoicedTo = "QtyInvoicedTo";

	/** Set Quantity Invoiced To.
	  * Invoiced Quantity
	  */
	public void setQtyInvoicedTo (BigDecimal QtyInvoicedTo);

	/** Get Quantity Invoiced To.
	  * Invoiced Quantity
	  */
	public BigDecimal getQtyInvoicedTo();

    /** Column name QtyPlan */
    public static final String COLUMNNAME_QtyPlan = "QtyPlan";

	/** Set Quantity Plan.
	  * Planned Quantity
	  */
	public void setQtyPlan (BigDecimal QtyPlan);

	/** Get Quantity Plan.
	  * Planned Quantity
	  */
	public BigDecimal getQtyPlan();

    /** Column name QtyPlanTo */
    public static final String COLUMNNAME_QtyPlanTo = "QtyPlanTo";

	/** Set Quantity Plan To.
	  * Planned Quantity
	  */
	public void setQtyPlanTo (BigDecimal QtyPlanTo);

	/** Get Quantity Plan To.
	  * Planned Quantity
	  */
	public BigDecimal getQtyPlanTo();

    /** Column name QtySpent */
    public static final String COLUMNNAME_QtySpent = "QtySpent";

	/** Set Quantity Used.
	  * Quantity used for this event
	  */
	public void setQtySpent (BigDecimal QtySpent);

	/** Get Quantity Used.
	  * Quantity used for this event
	  */
	public BigDecimal getQtySpent();

    /** Column name QtySpentTo */
    public static final String COLUMNNAME_QtySpentTo = "QtySpentTo";

	/** Set Quantity Used To.
	  * Quantity used for this event
	  */
	public void setQtySpentTo (BigDecimal QtySpentTo);

	/** Get Quantity Used To.
	  * Quantity used for this event
	  */
	public BigDecimal getQtySpentTo();

    /** Column name R_Category_ID */
    public static final String COLUMNNAME_R_Category_ID = "R_Category_ID";

	/** Set Category.
	  * Request Category
	  */
	public void setR_Category_ID (int R_Category_ID);

	/** Get Category.
	  * Request Category
	  */
	public int getR_Category_ID();

	public I_R_Category getR_Category() throws RuntimeException;

    /** Column name R_CategoryTo_ID */
    public static final String COLUMNNAME_R_CategoryTo_ID = "R_CategoryTo_ID";

	/** Set Category To.
	  * Request Category
	  */
	public void setR_CategoryTo_ID (int R_CategoryTo_ID);

	/** Get Category To.
	  * Request Category
	  */
	public int getR_CategoryTo_ID();

	public I_R_Category getR_CategoryTo() throws RuntimeException;

    /** Column name R_Group_ID */
    public static final String COLUMNNAME_R_Group_ID = "R_Group_ID";

	/** Set Group.
	  * Request Group
	  */
	public void setR_Group_ID (int R_Group_ID);

	/** Get Group.
	  * Request Group
	  */
	public int getR_Group_ID();

	public I_R_Group getR_Group() throws RuntimeException;

    /** Column name R_GroupTo_ID */
    public static final String COLUMNNAME_R_GroupTo_ID = "R_GroupTo_ID";

	/** Set Group To.
	  * Request Group
	  */
	public void setR_GroupTo_ID (int R_GroupTo_ID);

	/** Get Group To.
	  * Request Group
	  */
	public int getR_GroupTo_ID();

	public I_R_Group getR_GroupTo() throws RuntimeException;

    /** Column name R_RequestAction_ID */
    public static final String COLUMNNAME_R_RequestAction_ID = "R_RequestAction_ID";

	/** Set Request History.
	  * Request has been changed
	  */
	public void setR_RequestAction_ID (int R_RequestAction_ID);

	/** Get Request History.
	  * Request has been changed
	  */
	public int getR_RequestAction_ID();

    /** Column name R_Request_ID */
    public static final String COLUMNNAME_R_Request_ID = "R_Request_ID";

	/** Set Request.
	  * Request from a Business Partner or Prospect
	  */
	public void setR_Request_ID (int R_Request_ID);

	/** Get Request.
	  * Request from a Business Partner or Prospect
	  */
	public int getR_Request_ID();

	public I_R_Request getR_Request() throws RuntimeException;

    /** Column name R_RequestType_ID */
    public static final String COLUMNNAME_R_RequestType_ID = "R_RequestType_ID";

	/** Set Request Type.
	  * Type of request (e.g. Inquiry, Complaint, ..)
	  */
	public void setR_RequestType_ID (int R_RequestType_ID);

	/** Get Request Type.
	  * Type of request (e.g. Inquiry, Complaint, ..)
	  */
	public int getR_RequestType_ID();

	public I_R_RequestType getR_RequestType() throws RuntimeException;

    /** Column name R_RequestTypeTo_ID */
    public static final String COLUMNNAME_R_RequestTypeTo_ID = "R_RequestTypeTo_ID";

	/** Set Request Type To.
	  * Type of request (e.g. Inquiry, Complaint, ..)
	  */
	public void setR_RequestTypeTo_ID (int R_RequestTypeTo_ID);

	/** Get Request Type To.
	  * Type of request (e.g. Inquiry, Complaint, ..)
	  */
	public int getR_RequestTypeTo_ID();

	public I_R_RequestType getR_RequestTypeTo() throws RuntimeException;

    /** Column name R_Resolution_ID */
    public static final String COLUMNNAME_R_Resolution_ID = "R_Resolution_ID";

	/** Set Resolution.
	  * Request Resolution
	  */
	public void setR_Resolution_ID (int R_Resolution_ID);

	/** Get Resolution.
	  * Request Resolution
	  */
	public int getR_Resolution_ID();

	public I_R_Resolution getR_Resolution() throws RuntimeException;

    /** Column name R_ResolutionTo_ID */
    public static final String COLUMNNAME_R_ResolutionTo_ID = "R_ResolutionTo_ID";

	/** Set Resolution To.
	  * Request Resolution
	  */
	public void setR_ResolutionTo_ID (int R_ResolutionTo_ID);

	/** Get Resolution To.
	  * Request Resolution
	  */
	public int getR_ResolutionTo_ID();

	public I_R_Resolution getR_ResolutionTo() throws RuntimeException;

    /** Column name R_Status_ID */
    public static final String COLUMNNAME_R_Status_ID = "R_Status_ID";

	/** Set Status.
	  * Request Status
	  */
	public void setR_Status_ID (int R_Status_ID);

	/** Get Status.
	  * Request Status
	  */
	public int getR_Status_ID();

	public I_R_Status getR_Status() throws RuntimeException;

    /** Column name R_StatusTo_ID */
    public static final String COLUMNNAME_R_StatusTo_ID = "R_StatusTo_ID";

	/** Set Status To.
	  * Request Status
	  */
	public void setR_StatusTo_ID (int R_StatusTo_ID);

	/** Get Status To.
	  * Request Status
	  */
	public int getR_StatusTo_ID();

	public I_R_Status getR_StatusTo() throws RuntimeException;

    /** Column name SalesRep_ID */
    public static final String COLUMNNAME_SalesRep_ID = "SalesRep_ID";

	/** Set Sales Representative.
	  * Sales Representative or Company Agent
	  */
	public void setSalesRep_ID (int SalesRep_ID);

	/** Get Sales Representative.
	  * Sales Representative or Company Agent
	  */
	public int getSalesRep_ID();

	public I_AD_User getSalesRep() throws RuntimeException;

    /** Column name SalesRepTo_ID */
    public static final String COLUMNNAME_SalesRepTo_ID = "SalesRepTo_ID";

	/** Set Sales Representative To.
	  * Sales Representative or Company Agent
	  */
	public void setSalesRepTo_ID (int SalesRepTo_ID);

	/** Get Sales Representative To.
	  * Sales Representative or Company Agent
	  */
	public int getSalesRepTo_ID();

	public I_AD_User getSalesRepTo() throws RuntimeException;

    /** Column name StartDate */
    public static final String COLUMNNAME_StartDate = "StartDate";

	/** Set Start Date.
	  * First effective day (inclusive)
	  */
	public void setStartDate (Timestamp StartDate);

	/** Get Start Date.
	  * First effective day (inclusive)
	  */
	public Timestamp getStartDate();

    /** Column name StartDateTo */
    public static final String COLUMNNAME_StartDateTo = "StartDateTo";

	/** Set Start Date To.
	  * First effective day (inclusive)
	  */
	public void setStartDateTo (Timestamp StartDateTo);

	/** Get Start Date To.
	  * First effective day (inclusive)
	  */
	public Timestamp getStartDateTo();

    /** Column name Summary */
    public static final String COLUMNNAME_Summary = "Summary";

	/** Set Summary.
	  * Textual summary of this request
	  */
	public void setSummary (String Summary);

	/** Get Summary.
	  * Textual summary of this request
	  */
	public String getSummary();

    /** Column name SummaryTo */
    public static final String COLUMNNAME_SummaryTo = "SummaryTo";

	/** Set Summary To.
	  * Textual summary of this request
	  */
	public void setSummaryTo (String SummaryTo);

	/** Get Summary To.
	  * Textual summary of this request
	  */
	public String getSummaryTo();

    /** Column name TaskStatus */
    public static final String COLUMNNAME_TaskStatus = "TaskStatus";

	/** Set Task Status.
	  * Status of the Task
	  */
	public void setTaskStatus (String TaskStatus);

	/** Get Task Status.
	  * Status of the Task
	  */
	public String getTaskStatus();

    /** Column name TaskStatusTo */
    public static final String COLUMNNAME_TaskStatusTo = "TaskStatusTo";

	/** Set Task Status To.
	  * Status of the Task
	  */
	public void setTaskStatusTo (String TaskStatusTo);

	/** Get Task Status To.
	  * Status of the Task
	  */
	public String getTaskStatusTo();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name UUID */
    public static final String COLUMNNAME_UUID = "UUID";

	/** Set Immutable Universally Unique Identifier.
	  * Immutable Universally Unique Identifier
	  */
	public void setUUID (String UUID);

	/** Get Immutable Universally Unique Identifier.
	  * Immutable Universally Unique Identifier
	  */
	public String getUUID();
}
