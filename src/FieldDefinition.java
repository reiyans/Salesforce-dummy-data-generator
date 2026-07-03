import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Show all画面から取得した項目1件分の情報を保持するクラス。
 * Pythonでいう dataclass / namedtuple に近い役割。
 */
public class FieldDefinition {

    private static final Pattern MAX_LENGTH_PATTERN = Pattern.compile("\\((\\d+)");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("\\(\\s*\\d+\\s*,\\s*(\\d+)\\s*\\)");

    private final String apiName;
    private final String label;
    private final String rawType;
    private final String sampleValue;

    public FieldDefinition(String apiName, String label, String rawType, String sampleValue) {
        this.apiName = apiName;
        this.label = label;
        this.rawType = rawType;
        this.sampleValue = sampleValue;
    }

    public String getApiName() {
        return apiName;
    }

    public String getLabel() {
        return label;
    }

    public String getRawType() {
        return rawType;
    }

    public String getSampleValue() {
        return sampleValue;
    }

    public boolean isRequired() {
        return rawType != null && rawType.toLowerCase().contains("required");
    }

    // "text (80),required" -> "text" / "multipicklist" -> "multipicklist"
    public String getBaseType() {
        if (rawType == null || rawType.isEmpty()) {
            return "";
        }
        String lower = rawType.toLowerCase();
        int end = 0;
        while (end < lower.length() && Character.isLetter(lower.charAt(end))) {
            end++;
        }
        return lower.substring(0, end);
    }

    // "text (80),required" -> 80 / 該当なしなら null
    public Integer getMaxLength() {
        if (rawType == null) {
            return null;
        }
        Matcher m = MAX_LENGTH_PATTERN.matcher(rawType);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    // "currency (16, 2)" -> 2 / 該当なしなら null
    public Integer getDecimalPlaces() {
        if (rawType == null) {
            return null;
        }
        Matcher m = DECIMAL_PATTERN.matcher(rawType);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    @Override
    public String toString() {
        return apiName + " (" + label + ") [" + rawType + "] sample=" + sampleValue;
    }
}
