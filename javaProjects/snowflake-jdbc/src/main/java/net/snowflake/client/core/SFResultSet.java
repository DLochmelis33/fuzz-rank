package net.snowflake.client.core;

import static net.snowflake.client.core.StmtUtil.eventHandler;
import static net.snowflake.client.jdbc.SnowflakeUtil.systemGetProperty;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import net.snowflake.client.core.BasicEvent.QueryState;
import net.snowflake.client.core.json.Converters;
import net.snowflake.client.jdbc.ErrorCode;
import net.snowflake.client.jdbc.JsonResultChunk;
import net.snowflake.client.jdbc.SnowflakeResultChunk;
import net.snowflake.client.jdbc.SnowflakeResultSetSerializableV1;
import net.snowflake.client.jdbc.SnowflakeSQLException;
import net.snowflake.client.jdbc.SnowflakeSQLLoggedException;
import net.snowflake.client.jdbc.telemetry.Telemetry;
import net.snowflake.client.jdbc.telemetry.TelemetryData;
import net.snowflake.client.jdbc.telemetry.TelemetryField;
import net.snowflake.client.jdbc.telemetry.TelemetryUtil;
import net.snowflake.client.log.SFLogger;
import net.snowflake.client.log.SFLoggerFactory;
import net.snowflake.common.core.SqlState;

/** Snowflake ResultSet implementation */
public class SFResultSet extends SFJsonResultSet {
  private static final SFLogger logger = SFLoggerFactory.getLogger(SFResultSet.class);

  private int columnCount = 0;

  private int currentChunkRowCount = 0;

  private int currentChunkRowIndex = -1;

  private JsonNode firstChunkRowset = null;

  private JsonResultChunk currentChunk = null;

  private String queryId;

  private SFStatementType statementType;

  private boolean totalRowCountTruncated;

  private boolean sortResult = false;

  private Object[][] firstChunkSortedRowSet;

  // time the first chunk is consumed at (timestamp taken at object creation)
  private final long firstChunkTime;

  private long chunkCount = 0;

  private long nextChunkIndex = 0;

  private ChunkDownloader chunkDownloader;

  protected SFBaseStatement statement;

  private final boolean arrayBindSupported;

  private Telemetry telemetryClient;

  // If customer wants Timestamp_NTZ values to be stored in UTC time
  // instead of a local/session timezone, set to true
  private boolean treatNTZAsUTC;

  private boolean formatDateWithTimezone;

  /**
   * Constructor takes a result from the API response that we get from executing a SQL statement.
   *
   * <p>The constructor will initialize the ResultSetMetaData.
   *
   * @param resultSetSerializable result data after parsing
   * @param statement statement object
   * @param sortResult true if sort results otherwise false
   * @throws SQLException exception raised from general SQL layers
   */
  public SFResultSet(
      SnowflakeResultSetSerializableV1 resultSetSerializable,
      SFBaseStatement statement,
      boolean sortResult)
      throws SQLException {
    this(
        resultSetSerializable,
        statement.getSFBaseSession(),
        statement.getSFBaseSession().getTelemetryClient(),
        sortResult);

    this.statement = statement;
    SFBaseSession session = statement.getSFBaseSession();
    session.setDatabase(resultSetSerializable.getFinalDatabaseName());
    session.setSchema(resultSetSerializable.getFinalSchemaName());
    session.setRole(resultSetSerializable.getFinalRoleName());
    session.setWarehouse(resultSetSerializable.getFinalWarehouseName());
    this.treatNTZAsUTC = resultSetSerializable.getTreatNTZAsUTC();
    this.formatDateWithTimezone = resultSetSerializable.getFormatDateWithTimeZone();

    // update the driver/session with common parameters from GS
    SessionUtil.updateSfDriverParamValues(this.parameters, statement.getSFBaseSession());

    // if server gives a send time, log time it took to arrive
    if (resultSetSerializable.getSendResultTime() != 0) {
      long timeConsumeFirstResult = this.firstChunkTime - resultSetSerializable.getSendResultTime();
      logMetric(TelemetryField.TIME_CONSUME_FIRST_RESULT, timeConsumeFirstResult);
    }

    eventHandler.triggerStateTransition(
        BasicEvent.QueryState.CONSUMING_RESULT,
        String.format(QueryState.CONSUMING_RESULT.getArgString(), queryId, 0));
  }

  /**
   * This is a minimum initialization for SFResultSet. Mainly used for testing purpose. However,
   * real prod constructor will call this constructor as well
   *
   * @param resultSetSerializable data returned in query response
   * @param telemetryClient telemetryClient
   * @param sortResult should sorting take place
   * @throws SQLException if exception is encountered
   */
  public SFResultSet(
      SnowflakeResultSetSerializableV1 resultSetSerializable,
      Telemetry telemetryClient,
      boolean sortResult)
      throws SQLException {
    this(resultSetSerializable, new SFSession(), telemetryClient, sortResult);
  }

  /**
   * This is a minimum initialization for SFResultSet. Mainly used for testing purpose. However,
   * real prod constructor will call this constructor as well
   *
   * @param resultSetSerializable data returned in query response
   * @param session snowflake session
   * @param telemetryClient telemetryClient
   * @param sortResult should sorting take place
   * @throws SQLException if an exception is encountered.
   */
  public SFResultSet(
      SnowflakeResultSetSerializableV1 resultSetSerializable,
      SFBaseSession session,
      Telemetry telemetryClient,
      boolean sortResult)
      throws SQLException {
    super(resultSetSerializable.getTimeZone(), new Converters(session, resultSetSerializable));
    this.resultSetSerializable = resultSetSerializable;
    this.columnCount = 0;
    this.sortResult = sortResult;
    this.firstChunkTime = System.currentTimeMillis();

    this.telemetryClient = telemetryClient;
    this.queryId = resultSetSerializable.getQueryId();
    this.statementType = resultSetSerializable.getStatementType();
    this.totalRowCountTruncated = resultSetSerializable.isTotalRowCountTruncated();
    this.parameters = resultSetSerializable.getParameters();
    this.columnCount = resultSetSerializable.getColumnCount();
    this.firstChunkRowset = resultSetSerializable.getAndClearFirstChunkRowset();
    this.currentChunkRowCount = resultSetSerializable.getFirstChunkRowCount();
    this.chunkCount = resultSetSerializable.getChunkFileCount();
    this.chunkDownloader = resultSetSerializable.getChunkDownloader();
    this.timestampNTZFormatter = resultSetSerializable.getTimestampNTZFormatter();
    this.timestampLTZFormatter = resultSetSerializable.getTimestampLTZFormatter();
    this.timestampTZFormatter = resultSetSerializable.getTimestampTZFormatter();
    this.dateFormatter = resultSetSerializable.getDateFormatter();
    this.timeFormatter = resultSetSerializable.getTimeFormatter();
    this.honorClientTZForTimestampNTZ = resultSetSerializable.isHonorClientTZForTimestampNTZ();
    this.binaryFormatter = resultSetSerializable.getBinaryFormatter();
    this.resultVersion = resultSetSerializable.getResultVersion();
    this.numberOfBinds = resultSetSerializable.getNumberOfBinds();
    this.arrayBindSupported = resultSetSerializable.isArrayBindSupported();
    this.metaDataOfBinds = resultSetSerializable.getMetaDataOfBinds();
    this.resultSetMetaData = resultSetSerializable.getSFResultSetMetaData();
    this.treatNTZAsUTC = resultSetSerializable.getTreatNTZAsUTC();
    this.formatDateWithTimezone = resultSetSerializable.getFormatDateWithTimeZone();

    // sort result set if needed
    if (sortResult) {
      // we don't support sort result when there are offline chunks
      if (chunkCount > 0) {
        throw new SnowflakeSQLLoggedException(
            queryId,
            session,
            ErrorCode.CLIENT_SIDE_SORTING_NOT_SUPPORTED.getMessageCode(),
            SqlState.FEATURE_NOT_SUPPORTED);
      }

      sortResultSet();
    }
  }

  private boolean fetchNextRow() throws SFException, SnowflakeSQLException {
    if (sortResult) {
      return fetchNextRowSorted();
    } else {
      return fetchNextRowUnsorted();
    }
  }

  private boolean fetchNextRowSorted() {
    currentChunkRowIndex++;

    if (currentChunkRowIndex < currentChunkRowCount) {
      return true;
    }

    firstChunkSortedRowSet = null;

    // no more chunks as sorted is only supported
    // for one chunk
    return false;
  }

  private boolean fetchNextRowUnsorted() throws SFException, SnowflakeSQLException {
    currentChunkRowIndex++;

    if (currentChunkRowIndex < currentChunkRowCount) {
      return true;
    }

    // let GC collect first rowset
    firstChunkRowset = null;

    if (nextChunkIndex < chunkCount) {
      try {
        eventHandler.triggerStateTransition(
            BasicEvent.QueryState.CONSUMING_RESULT,
            String.format(QueryState.CONSUMING_RESULT.getArgString(), queryId, nextChunkIndex));

        SnowflakeResultChunk nextChunk = chunkDownloader.getNextChunkToConsume();

        if (nextChunk == null) {
          throw new SnowflakeSQLLoggedException(
              queryId,
              session,
              ErrorCode.INTERNAL_ERROR.getMessageCode(),
              SqlState.INTERNAL_ERROR,
              "Expect chunk but got null for chunk index " + nextChunkIndex);
        }

        currentChunkRowIndex = 0;
        currentChunkRowCount = nextChunk.getRowCount();
        currentChunk = (JsonResultChunk) nextChunk;

        logger.debug(
            "Moving to chunk index {}, row count={}", nextChunkIndex, currentChunkRowCount);

        nextChunkIndex++;

        return true;
      } catch (InterruptedException ex) {
        throw new SnowflakeSQLLoggedException(
            queryId, session, ErrorCode.INTERRUPTED.getMessageCode(), SqlState.QUERY_CANCELED);
      }
    } else if (chunkCount > 0) {
      try {
        logger.debug("End of chunks", false);
        DownloaderMetrics metrics = chunkDownloader.terminate();
        logChunkDownloaderMetrics(metrics);
      } catch (InterruptedException ex) {
        throw new SnowflakeSQLLoggedException(
            queryId, session, ErrorCode.INTERRUPTED.getMessageCode(), SqlState.QUERY_CANCELED);
      }
    }

    return false;
  }

  private void logMetric(TelemetryField field, long value) {
    TelemetryData data = TelemetryUtil.buildJobData(this.queryId, field, value);
    this.telemetryClient.addLogToBatch(data);
  }

  private void logChunkDownloaderMetrics(DownloaderMetrics metrics) {
    if (metrics != null) {
      logMetric(TelemetryField.TIME_WAITING_FOR_CHUNKS, metrics.getMillisWaiting());
      logMetric(TelemetryField.TIME_DOWNLOADING_CHUNKS, metrics.getMillisDownloading());
      logMetric(TelemetryField.TIME_PARSING_CHUNKS, metrics.getMillisParsing());
    }
  }

  /**
   * Advance to next row
   *
   * @return true if next row exists, false otherwise
   */
  @Override
  public boolean next() throws SFException, SnowflakeSQLException {
    if (isClosed()) {
      return false;
    }

    // otherwise try to fetch again
    if (fetchNextRow()) {
      row++;
      if (isLast()) {
        long timeConsumeLastResult = System.currentTimeMillis() - this.firstChunkTime;
        logMetric(TelemetryField.TIME_CONSUME_LAST_RESULT, timeConsumeLastResult);
      }
      return true;
    } else {
      logger.debug("End of result", false);

      /*
       * Here we check if the result has been truncated and throw exception if
       * so.
       */
      if (totalRowCountTruncated
          || Boolean.TRUE
              .toString()
              .equalsIgnoreCase(systemGetProperty("snowflake.enable_incident_test2"))) {
        throw new SFException(queryId, ErrorCode.MAX_RESULT_LIMIT_EXCEEDED);
      }

      // mark end of result
      return false;
    }
  }

  @Override
  protected Object getObjectInternal(int columnIndex) throws SFException {
    if (columnIndex <= 0 || columnIndex > resultSetMetaData.getColumnCount()) {
      throw new SFException(queryId, ErrorCode.COLUMN_DOES_NOT_EXIST, columnIndex);
    }

    final int internalColumnIndex = columnIndex - 1;
    Object retValue;
    if (sortResult) {
      retValue = firstChunkSortedRowSet[currentChunkRowIndex][internalColumnIndex];
    } else if (firstChunkRowset != null) {
      retValue =
          JsonResultChunk.extractCell(firstChunkRowset, currentChunkRowIndex, internalColumnIndex);
    } else if (currentChunk != null) {
      retValue = currentChunk.getCell(currentChunkRowIndex, internalColumnIndex);
    } else {
      throw new SFException(queryId, ErrorCode.COLUMN_DOES_NOT_EXIST, columnIndex);
    }
    wasNull = retValue == null;
    return retValue;
  }

  private void sortResultSet() {
    // first fetch rows into firstChunkSortedRowSet
    firstChunkSortedRowSet = new Object[currentChunkRowCount][];

    for (int rowIdx = 0; rowIdx < currentChunkRowCount; rowIdx++) {
      firstChunkSortedRowSet[rowIdx] = new Object[columnCount];
      for (int colIdx = 0; colIdx < columnCount; colIdx++) {
        firstChunkSortedRowSet[rowIdx][colIdx] =
            JsonResultChunk.extractCell(firstChunkRowset, rowIdx, colIdx);
      }
    }

    // now sort it
    Arrays.sort(
        firstChunkSortedRowSet,
        new Comparator<Object[]>() {
          public int compare(Object[] a, Object[] b) {
            int numCols = a.length;

            for (int colIdx = 0; colIdx < numCols; colIdx++) {
              if (a[colIdx] == null && b[colIdx] == null) {
                continue;
              }

              // null is considered bigger than all values
              if (a[colIdx] == null) {
                return 1;
              }

              if (b[colIdx] == null) {
                return -1;
              }

              int res = a[colIdx].toString().compareTo(b[colIdx].toString());

              // continue to next column if no difference
              if (res == 0) {
                continue;
              }

              return res;
            }

            // all columns are the same
            return 0;
          }
        });
  }

  @Override
  public boolean isLast() {
    return nextChunkIndex == chunkCount && currentChunkRowIndex + 1 == currentChunkRowCount;
  }

  @Override
  public boolean isAfterLast() {
    return nextChunkIndex == chunkCount && currentChunkRowIndex >= currentChunkRowCount;
  }

  @Override
  public void close() throws SnowflakeSQLException {
    super.close();

    try {
      if (chunkDownloader != null) {
        DownloaderMetrics metrics = chunkDownloader.terminate();
        logChunkDownloaderMetrics(metrics);
        firstChunkSortedRowSet = null;
        firstChunkRowset = null;
        currentChunk = null;
      }
    } catch (InterruptedException ex) {
      throw new SnowflakeSQLLoggedException(
          queryId, session, ErrorCode.INTERRUPTED.getMessageCode(), SqlState.QUERY_CANCELED);
    }
  }

  @Override
  public SFStatementType getStatementType() {
    return statementType;
  }

  @Override
  public void setStatementType(SFStatementType statementType) {
    this.statementType = statementType;
  }

  @Override
  public boolean isArrayBindSupported() {
    return this.arrayBindSupported;
  }

  @Override
  public String getQueryId() {
    return queryId;
  }
}
