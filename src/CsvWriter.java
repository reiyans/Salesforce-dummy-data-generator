import java.util.List;

/**
 * 生成結果をData Loaderにそのまま投入できるCSV文字列に変換するクラス。
 */
public class CsvWriter {

    public static String toCsv(List<FieldDefinition> fields, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();

        String[] headers = new String[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            headers[i] = fields.get(i).getApiName();
        }
        sb.append(toCsvLine(headers));

        for (String[] row : rows) {
            sb.append(toCsvLine(row));
        }

        return sb.toString();
    }

    private static String toCsvLine(String[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escape(values[i]));
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuote = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuote ? "\"" + escaped + "\"" : escaped;
    }
}
