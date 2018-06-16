package remi.distributedFS.log;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class TimeTypeFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {
		StringBuilder st = new StringBuilder();
		st.append(new Date(record.getMillis()).toString())
			.append(' ')
			.append(record.getLoggerName())
			.append(' ')
			.append(record.getLevel())
			.append(": ")
			.append(record.getMessage())
			.append('\n');
		return st.toString();
	}

}
