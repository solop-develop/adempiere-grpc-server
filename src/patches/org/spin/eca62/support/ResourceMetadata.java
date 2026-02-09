/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2015 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.spin.eca62.support;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.MRole;
import org.compiere.model.MUser;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * S3 interface, this can help to implement methods like shared URL
 * @author Yamel Senih, ySenih@erpya.com, ERPCyA http://www.erpya.com
 * @see https://docs.aws.amazon.com/AmazonS3/latest/dev/ObjectOperations.html
 */
public class ResourceMetadata {
	
	private int clientId = -1;
	private int userId = -1;
	private int roleId = -1;
	private String containerId;
	private String tableName;
	private String columnName;
	private int recordId = -1;
	private String name;
	private String resourceName;
	private ContainerType containerType;
	
	public enum ContainerType {
		WINDOW,
		FORM,
		PROCESS,
		REPORT,
		BROWSER,
		ATTACHMENT,
		RESOURCE,
		APPLICATION,
	};
	
	private ResourceMetadata() {
		
	}
	
	public static ResourceMetadata newInstance() {
		return new ResourceMetadata();
	}

	public int getClientId() {
		return clientId;
	}

	public ResourceMetadata withClientId(int clientId) {
		this.clientId = clientId;
		return this;
	}

	public int getUserId() {
		return userId;
	}

	public ResourceMetadata withUserId(int userId) {
		this.userId = userId;
		return this;
	}

	public String getContainerId() {
		return containerId;
	}

	public ResourceMetadata withContainerId(String containerId) {
		this.containerId = containerId;
		return this;
	}

	public String getTableName() {
		return tableName;
	}

	public ResourceMetadata withTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public int getRecordId() {
		return recordId;
	}

	public ResourceMetadata withRecordId(int recordId) {
		this.recordId = recordId;
		return this;
	}

	public String getName() {
		return name;
	}

	public ResourceMetadata withName(String name) {
		this.name = name;
		return this;
	}
	
	public ResourceMetadata withResourceName(String resourceName) {
		this.resourceName = resourceName;
		return this;
	}
	
	public ContainerType getContainerType() {
		return containerType;
	}

	public ResourceMetadata withContainerType(ContainerType containerType) {
		this.containerType = containerType;
		return this;
	}

	public String getColumnName() {
		return columnName;
	}

	public ResourceMetadata withColumnName(String columnName) {
		this.columnName = columnName;
		return this;
	}

	public int getRoleId() {
		return roleId;
	}

	public ResourceMetadata withRoleId(int roleId) {
		this.roleId = roleId;
		return this;
	}

	public String getResourcePath() {
		if(clientId < 0) {
			throw new AdempiereException("Client ID is Mandatory");
		}
		if(containerType == null) {
			throw new AdempiereException("Container Type is Mandatory");
		}
		// TODO: Validate Zero with AD_Client, AD_Role, M_Warehouse
		if(recordId > 0 && Util.isEmpty(tableName, true)) {
			throw new AdempiereException("Table Name is Mandatory");
		}
		if(recordId <= 0 && !Util.isEmpty(tableName, true)) {
			throw new AdempiereException("Record ID is Mandatory");
		}
		if(!Util.isEmpty(columnName, true) && Util.isEmpty(tableName, true)) {
			throw new AdempiereException("Table Name is Mandatory");
		}
		if(containerType != ContainerType.ATTACHMENT && Util.isEmpty(containerId, true)) {
			throw new AdempiereException("Container ID is Mandatory");
		}
		if(containerType == ContainerType.ATTACHMENT && recordId <= 0 && Util.isEmpty(tableName, true)) {
			throw new AdempiereException("Invalid Container Type (Mandatory Record ID and Table Name)");
		}
		String clientUuid = MClient.get(Env.getCtx(), clientId).getUUID();
		if(Util.isEmpty(clientUuid, true)) {
			throw new AdempiereException("Client UUID is Mandatory");
		}
		//	Create Path
		StringBuffer path = new StringBuffer(clientUuid).append("/");
		if(userId >= 0) {
			String userUuid = MUser.get(Env.getCtx(), userId).getUUID();
			if(Util.isEmpty(userUuid, true)) {
				throw new AdempiereException("User UUID is Mandatory");
			}
			path.append("user").append("/").append(userUuid).append("/");
		} else if(roleId >= 0) {
			String roleUuid = MRole.get(Env.getCtx(), roleId).getUUID();
			if(Util.isEmpty(roleUuid, true)) {
				throw new AdempiereException("Role UUID is Mandatory");
			}
			path.append("role").append("/").append(roleUuid).append("/");
		} else {
			path.append("client").append("/");
		}
		//	Container Type
		path.append(containerType.toString());
		if(!Util.isEmpty(containerId, true)) {
			path.append("/").append(containerId);
		}
		//	Table Name
		if(!Util.isEmpty(tableName, true)) {
			path.append("/").append(getValidPathName(tableName)).append("/").append(recordId);
		}
		//	Column
		if(!Util.isEmpty(columnName, true)) {
			path.append("/").append(getValidPathName(columnName));
		}
		return getValidPathName(path.toString().toLowerCase());
	}
	
	public String getResourceFileName() {
		if(!Util.isEmpty(resourceName, true)) {
			return resourceName;
		}
		if(Util.isEmpty(name, true)) {
			throw new AdempiereException("Resource Name is Mandatory");
		}
		return (getResourcePath() + "/" + getValidFileName(name)).toLowerCase();
	}
	
	/**
	 * Get resource path without file name (used for listing files)
	 * @return Resource path directory
	 */
	public String getResourcePathOnly() {
		return getResourcePath() + "/";
	}

	public static String getValidPathName(String path) {
		if(path == null) {
			return "";
		}
		return path.replaceAll("[^A-Za-z0-9/_-]", "_");
	}
	
	public static String getValidFileName(String path) {
		if(path == null) {
			return "";
		}
		return path.replaceAll("[^A-Za-z0-9._-]", "_");
	}
	
	public static String getValidCompleteName(String path) {
		if(path == null) {
			return "";
		}
		return path.replaceAll("[^A-Za-z0-9/._-]", "_");
	}
}
