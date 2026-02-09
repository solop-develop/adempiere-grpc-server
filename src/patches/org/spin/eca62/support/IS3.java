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

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * S3 interface, this can help to implement methods like shared URL
 * @author Yamel Senih, ySenih@erpya.com, ERPCyA http://www.erpya.com
 * @see https://docs.aws.amazon.com/AmazonS3/latest/dev/ObjectOperations.html
 */
public interface IS3 {
	
	/**
	 * Put resource based on metadata (Client, User, container Type, Table, Record ID)
	 * @param resourceMetadata
	 * @param resource
	 * @throws Exception
	 */
	public String putResource(ResourceMetadata resourceMetadata, InputStream resource) throws Exception;
	
	/**
	 * Put resource based on metadata (Client, User, container Type, Table, Record ID)
	 * @param resourceMetadata
	 * @return
	 * @throws Exception
	 */
	public InputStream getResource(ResourceMetadata resourceMetadata) throws Exception;
	
	/**
	 * Put a Temporary Resource
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public String putTemporaryFile(File file) throws Exception;
	
	/**
	 * Get list of file names from a resource path based on metadata
	 * @param resourceMetadata
	 * @return List of file names
	 * @throws Exception
	 */
	public List<String> getResourceFileNames(ResourceMetadata resourceMetadata) throws Exception;

}
