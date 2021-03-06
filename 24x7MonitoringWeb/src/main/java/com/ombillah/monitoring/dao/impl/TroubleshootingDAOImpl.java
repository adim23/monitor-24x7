package com.ombillah.monitoring.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ombillah.monitoring.dao.TroubleshootingDAO;
import com.ombillah.monitoring.domain.ExceptionLogger;
import com.ombillah.monitoring.domain.HttpRequestUrl;
import com.ombillah.monitoring.domain.MethodSignature;
import com.ombillah.monitoring.domain.MonitoredItemTracer;
import com.ombillah.monitoring.domain.SearchFilter;
import com.ombillah.monitoring.domain.SqlQuery;


/**
 * Data Access Object for collecting and storing performance data.
 * @author Oussama M Billah
 *
 */
@Repository("troubleshootingDAO")
@Transactional(readOnly = true)
public class TroubleshootingDAOImpl implements TroubleshootingDAO {
	
	private JdbcTemplate jdbcTemplate;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private SimpleJdbcInsert methodSignatureJdbcInsert;
	private SimpleJdbcInsert exceptionLoggerJdbcInsert;
	private SimpleJdbcInsert sqlQueriesJdbcInsert;
	private SimpleJdbcInsert httpRequestJdbcInsert;
	private SimpleJdbcInsert monitoredItemTracerJdbcInsert;
	
    @Resource(name="H2DataSource")
    public void setDataSource(BasicDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        methodSignatureJdbcInsert = new SimpleJdbcInsert(dataSource).withTableName("METHOD_SIGNATURES");
        exceptionLoggerJdbcInsert = new SimpleJdbcInsert(dataSource).withTableName("EXCEPTION_LOGGER");
        sqlQueriesJdbcInsert = new SimpleJdbcInsert(dataSource).withTableName("SQL_QUERIES");
        httpRequestJdbcInsert = new SimpleJdbcInsert(dataSource).withTableName("HTTP_REQUESTS");
        monitoredItemTracerJdbcInsert = new SimpleJdbcInsert(dataSource).withTableName("MONITORED_ITEM_TRACER");
    }
	
	public List<MethodSignature> retrieveMethodSignatures() {
		
		List<MethodSignature> list = this.jdbcTemplate.query(
	        "SELECT METHOD_SIGNATURE FROM METHOD_SIGNATURES",
	        new RowMapper<MethodSignature>() {
	            public MethodSignature mapRow(ResultSet rs, int rowNum) throws SQLException {
	            	MethodSignature signature = new MethodSignature();
	            	signature.setMethodName(rs.getString("METHOD_SIGNATURE"));
	                return signature;
	            }
	        });
			
		return list;
	}

	public List<MonitoredItemTracer> retrieveItemStatisticsGroupedByMonitoredItem(SearchFilter searchFilter) {
		List<Object> params = new ArrayList<Object>();
		int[] types = new int[0];
		
		String query = "SELECT ITEM_NAME, ROUND(SUM(AVERAGE * COUNT) / SUM(COUNT)) AS AVERAGE, " +
				" MIN(MIN) AS MIN, MAX(MAX) AS MAX, SUM(COUNT) AS COUNT " + 
				" FROM MONITORED_ITEM_TRACER " + 
				" WHERE CREATION_DATE BETWEEN ? " +
				"	AND ? ";
		
		params.add(DATE_FORMAT.format(searchFilter.getMinDate()));
		params.add(DATE_FORMAT.format(searchFilter.getMaxDate()));
		types = ArrayUtils.add(types, Types.VARCHAR);
		types = ArrayUtils.add(types, Types.VARCHAR);
		
		if(searchFilter.getMinExecTime() != null) {
			query += " AND AVERAGE > ? ";
			params.add(searchFilter.getMinExecTime());
			types = ArrayUtils.add(types, Types.DOUBLE);
		}
		
		if(searchFilter.getMaxExecTime() != null) {
			query += " AND AVERAGE < ? ";
			params.add(searchFilter.getMaxExecTime());
			types = ArrayUtils.add(types, Types.DOUBLE);
		}
		query += "  AND ( ";
		
		List<String> methodSignatures = searchFilter.getMethodSignatures();
		for(int i = 0; i < methodSignatures.size(); i++) {
			String itemName = methodSignatures.get(i);
			String[] queryTypes = {"SELECT", "UPDATE", "INSERT", "DELETE" };
			if(StringUtils.equals(itemName, "SQL")) {
				query += "TYPE = ? " ;
				params.add("SQL");
				types = ArrayUtils.add(types, Types.VARCHAR);
			} 
			else if(StringUtils.equals(itemName, "OTHER")) {
				query += "( TYPE = ? AND ITEM_NAME NOT LIKE ? AND ITEM_NAME NOT LIKE ? " ;
				query += "AND ITEM_NAME NOT LIKE ? AND ITEM_NAME NOT LIKE ? )";
				params.add("SQL");
				params.add("SELECT%");
				params.add("UPDATE%");
				params.add("DELETE%");
				params.add("INSERT%");
				types = ArrayUtils.add(types, Types.VARCHAR);
				types = ArrayUtils.add(types, Types.VARCHAR);
				types = ArrayUtils.add(types, Types.VARCHAR);
				types = ArrayUtils.add(types, Types.VARCHAR);
				types = ArrayUtils.add(types, Types.VARCHAR);
			}
			else if(ArrayUtils.contains(queryTypes, itemName)) {
				query += "(TYPE = ? AND ITEM_NAME LIKE ? ) " ;
				params.add("SQL");
				params.add(itemName + "%");
				types = ArrayUtils.add(types, Types.VARCHAR);
				types = ArrayUtils.add(types, Types.VARCHAR);
			}
			else if(StringUtils.equals(itemName, "HTTP Requests")) {
				query += "TYPE = ? " ;
				params.add("HTTP_REQUEST");
				types = ArrayUtils.add(types, Types.VARCHAR);
			} else {
				params.add(itemName + "%");
				types = ArrayUtils.add(types, Types.VARCHAR);
				query += "ITEM_NAME LIKE ? " ;
			}
			
			if(i != methodSignatures.size() - 1) {
				query += " OR ";
			}
		}
		query += " ) GROUP BY ITEM_NAME ORDER BY AVERAGE DESC";
		
		List<MonitoredItemTracer> result = this.jdbcTemplate.query(query, params.toArray(), types,
				 new RowMapper<MonitoredItemTracer>() {
           public MonitoredItemTracer mapRow(ResultSet rs, int rowNum) throws SQLException {
           		MonitoredItemTracer tracer = new MonitoredItemTracer();
				tracer.setItemName(rs.getString("ITEM_NAME"));
				tracer.setAverage(rs.getDouble("AVERAGE"));
				tracer.setCount(rs.getInt("COUNT"));
				tracer.setMax(rs.getDouble("MAX"));
				tracer.setMin(rs.getDouble("MIN"));
				return tracer;
           }
       });
		return result;
	}

	public List<MonitoredItemTracer> retrieveItemStatistics(SearchFilter searchFilter) {
		
		List<Object> params = new ArrayList<Object>();
		int[] types = new int[0];
		String query = "SELECT CREATION_DATE, ITEM_NAME, ROUND(SUM(AVERAGE * COUNT) / SUM(COUNT)) AS AVERAGE, " +
				" MIN(MIN) AS MIN, MAX(MAX) AS MAX, SUM(COUNT) AS COUNT " + 
				" FROM MONITORED_ITEM_TRACER " + 
				" WHERE CREATION_DATE BETWEEN ? " +
				"	AND ? ";
			
		params.add(DATE_FORMAT.format(searchFilter.getMinDate()));
		params.add(DATE_FORMAT.format(searchFilter.getMaxDate()));
		types = ArrayUtils.add(types, Types.VARCHAR);
		types = ArrayUtils.add(types, Types.VARCHAR);
		
		if(searchFilter.getMinExecTime() != null) {
			query += " AND AVERAGE > ? ";
			params.add(searchFilter.getMinExecTime());
			types = ArrayUtils.add(types, Types.DOUBLE);
		}
		
		if(searchFilter.getMaxExecTime() != null) {
			query += " AND AVERAGE < ? ";
			params.add(searchFilter.getMaxExecTime());
			types = ArrayUtils.add(types, Types.DOUBLE);
		}
		query += "  AND ( ";
		
		List<String> methodSignatures = searchFilter.getMethodSignatures();
		for(int i = 0; i < methodSignatures.size(); i++) {
			String itemName = methodSignatures.get(i);
			String[] queryTypes = {"SELECT", "UPDATE", "INSERT", "DELETE" };
			
			if(StringUtils.equals(itemName, "Memory")) {
				query += "TYPE = ? " ;
				params.add("MEMORY");
				types = ArrayUtils.add(types, Types.VARCHAR);
			}
			else if(StringUtils.equals(itemName, "CPU Usage")) {
				query += "TYPE = ? " ;
				params.add("CPU");
				types = ArrayUtils.add(types, Types.VARCHAR);
			}
			else if(StringUtils.equals(itemName, "Database Connections")) {
				query += "TYPE = ? " ;
				params.add("ACTIVE_CONNECTION");
				types = ArrayUtils.add(types, Types.VARCHAR);
			}
			else if(StringUtils.equals(itemName, "Active Sessions")) {
				query += "TYPE = ? " ;
				params.add("ACTIVE_SESSION");
				types = ArrayUtils.add(types, Types.VARCHAR);
			}
			else if(StringUtils.equals(itemName, "SQL")) {
				query += "TYPE = ? " ;
				params.add("SQL");
				types = ArrayUtils.add(types, Types.VARCHAR);
			}
			else if(StringUtils.equals(itemName, "Live Threads")) {
				query += "TYPE = ? " ;
				params.add("ACTIVE_THREAD");
				types = ArrayUtils.add(types, Types.VARCHAR);
			}
			else if(StringUtils.equals(itemName, "OTHER")) {
				query += "( TYPE = ? AND ITEM_NAME NOT LIKE ? AND ITEM_NAME NOT LIKE ? " ;
				query += "AND ITEM_NAME NOT LIKE ? AND ITEM_NAME NOT LIKE ? )";
				params.add("SQL");
				params.add("SELECT%");
				params.add("UPDATE%");
				params.add("DELETE%");
				params.add("INSERT%");
				types = ArrayUtils.add(types, Types.VARCHAR);
				types = ArrayUtils.add(types, Types.VARCHAR);
				types = ArrayUtils.add(types, Types.VARCHAR);
				types = ArrayUtils.add(types, Types.VARCHAR);
				types = ArrayUtils.add(types, Types.VARCHAR);
			}
			else if(ArrayUtils.contains(queryTypes, itemName)) {
				query += "(TYPE = ? AND ITEM_NAME LIKE ? ) " ;
				params.add("SQL");
				params.add(itemName + "%");
				types = ArrayUtils.add(types, Types.VARCHAR);
				types = ArrayUtils.add(types, Types.VARCHAR);
			}
			else if(StringUtils.equals(itemName, "HTTP Requests")) {
				query += "TYPE = ? " ;
				params.add("HTTP_REQUEST");
				types = ArrayUtils.add(types, Types.VARCHAR);
			}
			else {
				params.add(itemName + "%");
				types = ArrayUtils.add(types, Types.VARCHAR);
				query += "ITEM_NAME LIKE ? " ;
			}
			
			if(i != methodSignatures.size() - 1) {
				query += " OR ";
			}
		}
		
		query += " ) GROUP BY DATEDIFF(SECOND, '1970-01-01', CREATION_DATE) / " + searchFilter.getResolutionInSecs()
				+ ", ITEM_NAME, CREATION_DATE ORDER BY ITEM_NAME, CREATION_DATE";
		
		List<MonitoredItemTracer> result = this.jdbcTemplate.query(query, params.toArray(), types,
				 new RowMapper<MonitoredItemTracer>() {
          public MonitoredItemTracer mapRow(ResultSet rs, int rowNum) throws SQLException {
          		MonitoredItemTracer tracer = new MonitoredItemTracer();
				tracer.setItemName(rs.getString("ITEM_NAME"));
				tracer.setAverage(rs.getDouble("AVERAGE"));
				tracer.setCount(rs.getInt("COUNT"));
				tracer.setMax(rs.getDouble("MAX"));
				tracer.setMin(rs.getDouble("MIN"));
				tracer.setCreationDate(rs.getTimestamp("CREATION_DATE"));

				return tracer;
          }
      });
		
		return result;
	}
	
	public List<SqlQuery> retrieveSqlQueries() {
		List<SqlQuery> list = this.jdbcTemplate.query(
		        "SELECT DISTINCT SQL_QUERY FROM SQL_QUERIES",
		        new RowMapper<SqlQuery>() {
		            public SqlQuery mapRow(ResultSet rs, int rowNum) throws SQLException {
		            	SqlQuery query = new SqlQuery();
		            	query.setSqlQuery(rs.getString("SQL_QUERY"));
		                return query;
		            }
		        });
				
			return list;
	}

	public List<ExceptionLogger> retrieveExceptionLoggers(SearchFilter searchFilter) {
		List<Object> params = new ArrayList<Object>();
		int[] types = new int[0];
		String query = "SELECT EXCEPTION_MESSAGE, STACKTRACE, COUNT(1) AS COUNT " +
				" FROM EXCEPTION_LOGGER " + 
				" WHERE CREATION_DATE BETWEEN ? AND ? " +
				" GROUP BY EXCEPTION_MESSAGE";
			
		params.add(DATE_FORMAT.format(searchFilter.getMinDate()));
		params.add(DATE_FORMAT.format(searchFilter.getMaxDate()));
		types = ArrayUtils.add(types, Types.VARCHAR);
		types = ArrayUtils.add(types, Types.VARCHAR);
		
		List<ExceptionLogger> result = this.jdbcTemplate.query(query, params.toArray(), types,
				 new RowMapper<ExceptionLogger>() {
	          public ExceptionLogger mapRow(ResultSet rs, int rowNum) throws SQLException {
	        	  	ExceptionLogger logger = new ExceptionLogger();
	        	  	logger.setCount(rs.getInt("COUNT"));
	        	  	logger.setExceptionMessage(rs.getString("EXCEPTION_MESSAGE"));
	        	  	logger.setStacktrace(rs.getString("STACKTRACE"));
					return logger;
	          }
		});
		return result;
	}

	public List<HttpRequestUrl> retrieveHttpRequestUrls() {
		List<HttpRequestUrl> list = this.jdbcTemplate.query(
		        "SELECT DISTINCT REQUEST FROM HTTP_REQUESTS",
		        new RowMapper<HttpRequestUrl>() {
		            public HttpRequestUrl mapRow(ResultSet rs, int rowNum) throws SQLException {
		            	HttpRequestUrl request = new HttpRequestUrl();
		            	request.setRequestUrl(rs.getString("REQUEST"));
		                return request;
		            }
		        });
				
		return list;
	}

	public MonitoredItemTracer checkPerformanceDegredation(
			String monitoredItem, String itemType, Long timeToAlert,
			Long threshold) {
			
		String sql = "SELECT ITEM_NAME, ROUND(AVG(AVERAGE), 2) AS AVG, MAX(MAX) AS MAX FROM MONITORED_ITEM_TRACER"
				+ " WHERE type = ?" 
				+ " AND ITEM_NAME = ?"
				+ " AND CREATION_DATE > TIMESTAMPADD('MINUTE', ?, NOW())" 
				+ " GROUP BY ITEM_NAME"
				+ " HAVING AVG > ?";
		
		List<Object> params = new ArrayList<Object>();
		int[] types = new int[0];
					
		params.add(itemType);
		params.add(monitoredItem);
		params.add(timeToAlert * -1);
		params.add(threshold);
		
		types = ArrayUtils.add(types, Types.VARCHAR);
		types = ArrayUtils.add(types, Types.VARCHAR);
		types = ArrayUtils.add(types, Types.INTEGER);
		types = ArrayUtils.add(types, Types.INTEGER);
		
		List<MonitoredItemTracer> result = this.jdbcTemplate.query(sql, params.toArray(), types,
				 new RowMapper<MonitoredItemTracer>() {
	         public MonitoredItemTracer mapRow(ResultSet rs, int rowNum) throws SQLException {
	         		MonitoredItemTracer tracer = new MonitoredItemTracer();
					tracer.setItemName(rs.getString("ITEM_NAME"));
					tracer.setAverage(rs.getDouble("AVG"));
					tracer.setMax(rs.getDouble("MAX"));
					return tracer;
	         }
		});
		
		if(result.isEmpty()) {
			return null;
		}
		return result.get(0);
		
	}

	@Transactional(readOnly = false)
	public void saveMethodSignatures(List<MethodSignature> methodSignatures) {
		SqlParameterSource[] array = new SqlParameterSource[methodSignatures.size()]; 
		for (int i = 0; i < methodSignatures.size(); i++) {
			MethodSignature method = (MethodSignature)methodSignatures.get(i);
			array[i] = new MapSqlParameterSource()		
				.addValue("METHOD_SIGNATURE", method.getItemName(), Types.VARCHAR);			
		}
		methodSignatureJdbcInsert.executeBatch(array);
		
	}

	@Transactional(readOnly = false)
	public void saveMonitoredItemTracingStatistics(List<MonitoredItemTracer> tracers) {
        SqlParameterSource[] array = new SqlParameterSource[tracers.size()]; 
		for (int i = 0; i < tracers.size(); i++) {
			MonitoredItemTracer tracer = (MonitoredItemTracer) tracers.get(i);
			array[i] = new MapSqlParameterSource()		
				.addValue("ITEM_NAME", tracer.getItemName())
				.addValue("TYPE", tracer.getType())
				.addValue("AVERAGE", tracer.getAverage())	
				.addValue("MIN", tracer.getMin())
				.addValue("MAX", tracer.getMax())
				.addValue("COUNT", tracer.getCount())
				.addValue("CREATION_DATE", tracer.getCreationDate());
		}
		monitoredItemTracerJdbcInsert.executeBatch(array);	
	}

	@Transactional(readOnly = false)
	public void saveSqlQueries(List<SqlQuery> queries) {
		SqlParameterSource[] array = new SqlParameterSource[queries.size()]; 
		for (int i = 0; i < queries.size(); i++) {
			SqlQuery query = (SqlQuery)queries.get(i);
			array[i] = new MapSqlParameterSource()		
				.addValue("SQL_QUERY", query.getSqlQuery(), Types.VARCHAR);			
		}
		sqlQueriesJdbcInsert.executeBatch(array);
		
	}

	@Transactional(readOnly = false)
	public void saveException(ExceptionLogger logger) {
		Map<String, Object> parameters = new HashMap<String, Object>(7);
        parameters.put("EXCEPTION_MESSAGE", logger.getExceptionMessage());
        parameters.put("STACKTRACE", logger.getStacktrace());
        parameters.put("CREATION_DATE", logger.getCreationDate());
        exceptionLoggerJdbcInsert.execute(parameters);
	}

	@Transactional(readOnly = false)
	public void saveHttpRequestUrls(List<HttpRequestUrl> requestUrls) {
		SqlParameterSource[] array = new SqlParameterSource[requestUrls.size()]; 
		for (int i = 0; i < requestUrls.size(); i++) {
			HttpRequestUrl request = (HttpRequestUrl) requestUrls.get(i);
			array[i] = new MapSqlParameterSource()		
				.addValue("REQUEST", request.getRequestUrl(), Types.VARCHAR);			
		}
		httpRequestJdbcInsert.executeBatch(array);
	}

	@Transactional(readOnly = false)
	public void saveExceptions(List<ExceptionLogger> exceptions) {
		SqlParameterSource[] array = new SqlParameterSource[exceptions.size()]; 
		for (int i = 0; i < exceptions.size(); i++) {
			ExceptionLogger exception = (ExceptionLogger) exceptions.get(i);
			array[i] = new MapSqlParameterSource()		
				.addValue("EXCEPTION_MESSAGE", exception.getExceptionMessage())
				.addValue("STACKTRACE", exception.getStacktrace())
				.addValue("CREATION_DATE", exception.getCreationDate());			
		}
		exceptionLoggerJdbcInsert.executeBatch(array);
		
	}


}
