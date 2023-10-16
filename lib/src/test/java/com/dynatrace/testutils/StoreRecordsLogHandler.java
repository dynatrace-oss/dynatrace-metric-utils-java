package com.dynatrace.testutils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class StoreRecordsLogHandler extends Handler {
  private final List<LogRecord> records = new ArrayList<>();

  public List<LogRecord> getRecords() {
    return records;
  }

  @Override
  public void publish(LogRecord record) {
    records.add(record);
  }

  @Override
  public void flush() {}

  @Override
  public void close() throws SecurityException {}
}
