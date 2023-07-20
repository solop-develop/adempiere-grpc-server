/*************************************************************************************
 * Product: ADempiere GRPC Server                                                    *
 * Copyright (C) 2012-2019 E.R.P. Consultores y Asociados, C.A.                      *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                      *
 * This program is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by              *
 * the Free Software Foundation, either version 3 of the License, or                 *
 * (at your option) any later version.                                               *
 * This program is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                    *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                     *
 * GNU General Public License for more details.                                      *
 * You should have received a copy of the GNU General Public License                 *
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.base.setup;

import java.util.List;
import java.util.logging.Level;

/**
 * Determinate all ADempiere client setup values for Human Resource
 * @author Yamel Senih
 */
public class Server {
	/**	Host Name	*/
	private String host;
	/**	Port	*/
	private int port;
	/**	Certificate Chain	*/
	private String certificate_chain_file;
	/**	Private Key	*/
	private String private_key_file;
	/**	Trust Certificate	*/
	private String trust_certificate_collection_file;
	/**	Log Level	*/
	private String log_level;
	/**	ASecret Key	*/
	private String secret_key;
	/**	Time expiration	*/
	private long expiration;

	/**	Is Enabled All Services	*/
	private boolean is_enabled_all_services;
	/**	Embedded services	*/
	private List<String> services;

	/**
	 * Default constructor
	 * @param host
	 * @param port
	 * @param certificate_chain_file
	 * @param private_key_file
	 * @param trust_certificate_collection_file
	 * @param secret_key
	 * @param expiration
	 * @param log_level
	 * @param services
	 */
	public Server(
		String host, int port, String log_level,
		String certificate_chain_file, String private_key_file, String trust_certificate_collection_file,
		String secret_key, long expiration,
		boolean is_enabled_all_services, List<String> services
	) {
		this.host = host;
		this.port = port;
		this.certificate_chain_file = certificate_chain_file;
		this.private_key_file = private_key_file;
		this.trust_certificate_collection_file = trust_certificate_collection_file;
		this.log_level = log_level;
		this.secret_key = secret_key;
		this.expiration= expiration;
		this.is_enabled_all_services = is_enabled_all_services;
		this.services = services;
		if(this.log_level == null
				|| this.log_level.trim().length() == 0) {
			this.log_level = Level.WARNING.getName();
		}
	}
	
	/**
	 * Default constructor without parameters
	 */
	public Server() {
		this.log_level = Level.WARNING.getName();
	}

	/**
	 * @return the host
	 */
	public final String getHost() {
		return host;
	}

	/**
	 * @return the port
	 */
	public final int getPort() {
		return port;
	}

	/**
	 * @return the certificate_chain_file
	 */
	public final String getCertificate_chain_file() {
		return certificate_chain_file;
	}

	/**
	 * @return the private_key_file
	 */
	public final String getPrivate_key_file() {
		return private_key_file;
	}

	/**
	 * @return the trust_certificate_collection_file
	 */
	public final String getTrust_certificate_collection_file() {
		return trust_certificate_collection_file;
	}

	/**
	 * @return the isTlsEnabled
	 */
	public final boolean isTlsEnabled() {
		return getCertificate_chain_file() != null 
				&& getPrivate_key_file() != null;
	}

	/**
	 * Get Is Enabled All Services
	 * @return
	 */
	public final boolean getIs_enabled_all_services() {
		return this.is_enabled_all_services;
	}

	/**
	 * Get Services
	 * @return
	 */
	public final List<String> getServices() {
		return services;
	}

	/**
	 * Log Level
	 * @return
	 */
	public final String getLog_level() {
		return log_level;
	}

	/**	
	 * Secret key
	 * @return
	 */
	public String getSecret_key() {
		return secret_key;
	}
	
	public long getExpiration() {
		return expiration;
	}

	/**
	 * Validate is a service is enabled
	 * @param serviceName
	 * @return
	 */
	public final boolean isValidService(String serviceName) {
		// validate service name
		if(serviceName == null || serviceName.trim().length() == 0) {
			return false;
		}
		// overwrite services
		if (this.is_enabled_all_services) {
			return true;
		}
		// without services
		if (this.services == null || this.services.size() <= 0) {
			return false;
		}
		return getServices()
			.stream()
			.filter(serviceToFind -> serviceToFind != null && serviceToFind.equals(serviceName)).findFirst().isPresent();
	}

	@Override
	public String toString() {
		return "Server [host=" + host + ", port=" + port + ", certificate_chain_file=" + certificate_chain_file
				+ ", private_key_file=" + private_key_file + ", trust_certificate_collection_file="
				+ trust_certificate_collection_file + ", log_level=" + log_level + ", services=" + services + "]";
	}

}
