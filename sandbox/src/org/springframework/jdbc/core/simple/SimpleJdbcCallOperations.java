/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.core.simple;

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.Map;

/**
 * @author trisberg
 */
public interface SimpleJdbcCallOperations {

	/**
	 * Specify the procedure name to be used - this implies that we will be calling a stored procedure.
	 *
	 * @param procedureName the name of the stored procedure
	 * @return the instance of this SimpleJdbcCall
	 */
	SimpleJdbcCall withProcedureName(String procedureName);

	/**
	 * Specify the procedure name to be used - this implies that we will be calling a stored function.
	 *
	 * @param functionName the name of the stored function
	 * @return the instance of this SimpleJdbcCall
	 */
	SimpleJdbcCall withFunctionName(String functionName);

	/**
	 * Optionally, specify the name of the schema that contins the stored procedure.
	 *
	 * @param schemaName the name of the schema
	 * @return the instance of this SimpleJdbcCall
	 */
	SimpleJdbcCall withSchemaName(String schemaName);

	/**
	 * Optionally, specify the name of the catalog that contins the stored procedure.
	 *
	 * To provide consistency with the Oracle DatabaseMetaData, this is used to specify the package name if
	 * the procedure is declared as part of a package.
	 *
	 * @param catalogName the catalog or package name
	 * @return the instance of this SimpleJdbcCall
	 */
	SimpleJdbcCall withCatalogName(String catalogName);

	/**
	 * Indicates the procedure's return value should be included in the results returned.
	 *
	 * @return the instance of this SimpleJdbcCall
	 */
	SimpleJdbcCall withReturnValue();

	/**
	 * Specify one or more parameters if desired.  These parameters will be supplemented with any
	 * parameter information retrieved from the database meta data.
	 *
	 * @param sqlParameters the parameters to use
	 * @return the instance of this SimpleJdbcCall
	 */
	SimpleJdbcCall declareParameters(SqlParameter... sqlParameters);

	/** Not uesed yet */
	SimpleJdbcCall useInParameterNames(String... inParameterNames);

	/** Not uesed yet */
	SimpleJdbcCall useOutParameterNames(String... inParameterNames);

	/**
	 * Turn off any processing of parameter mete data information obtained via JDBC.
	 *
	 * @return the instance of this SimpleJdbcCall
	 */
	SimpleJdbcCall withoutProcedureColumnMetaDataAccess();

	/**
	 * Execute the stored function and return the results obtained as an Object of the specified return type.
	 * @param returnType the type of the value tp return
	 * @param args Map containing the parameter values to be used in the call.
	 */
	<T> T executeFunction(Class<T> returnType, Map args);

	/**
	 * Execute the stored function and return the results obtained as an Object of the specified return type.
	 * @param returnType the type of the value tp return
	 * @param args MapSqlParameterSource containing the parameter values to be used in the call.
	 */
	<T> T executeFunction(Class<T> returnType, MapSqlParameterSource args);

	/**
	 * Execute the stored procedure and return the single out parameter as an Object of the specified return type.
	 * @param returnType the type of the value tp return
	 * @param args Map containing the parameter values to be used in the call.
	 */
	<T> T executeObject(Class<T> returnType, Map args);

	/**
	 * Execute the stored procedure and return the single out parameter as an Object of the specified return type.
	 * @param returnType the type of the value tp return
	 * @param args MapSqlParameterSource containing the parameter values to be used in the call.
	 */
	<T> T executeObject(Class<T> returnType, MapSqlParameterSource args);

	/**
	 * Execute the stored procedure and return a map of output params, keyed by name as in parameter declarations..
	 * @return map of output params.
	 */
	Map<String, Object> execute();

	/**
	 * Execute the stored procedure and return a map of output params, keyed by name as in parameter declarations..
	 * @param args Map containing the parameter values to be used in the call.
	 * @return map of output params.
	 */
	Map<String, Object> execute(Map<String, Object> args);

	/**
	 * Execute the stored procedure and return a map of output params, keyed by name as in parameter declarations..
	 * @param args SqlParameterSource containing the parameter values to be used in the call.
	 * @return map of output params.
	 */
	Map<String, Object> execute(SqlParameterSource args);
}