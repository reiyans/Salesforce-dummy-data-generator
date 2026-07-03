import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * FieldDefinitionのリストと生成件数を受け取り、ダミー値の行データを作るクラス。
 * メールドメイン・日付範囲・数値範囲はコンストラクタで指定する(将来的にGUIの入力欄と対応させる)。
 */
public class DummyDataGenerator {

    private static final String ALPHANUM =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int DEFAULT_TEXT_LENGTH = 10;

    private final Random random;
    private final String emailDomain;
    private final LocalDate dateRangeStart;
    private final LocalDate dateRangeEnd;
    private final double numberRangeMin;
    private final double numberRangeMax;

    public DummyDataGenerator(String emailDomain, LocalDate dateRangeStart, LocalDate dateRangeEnd,
                               double numberRangeMin, double numberRangeMax) {
        this(emailDomain, dateRangeStart, dateRangeEnd, numberRangeMin, numberRangeMax, new Random());
    }

    public DummyDataGenerator(String emailDomain, LocalDate dateRangeStart, LocalDate dateRangeEnd,
                               double numberRangeMin, double numberRangeMax, Random random) {
        this.emailDomain = emailDomain;
        this.dateRangeStart = dateRangeStart;
        this.dateRangeEnd = dateRangeEnd;
        this.numberRangeMin = numberRangeMin;
        this.numberRangeMax = numberRangeMax;
        this.random = random;
    }

    public List<String[]> generate(List<FieldDefinition> fields, int count) {
        List<String[]> rows = new ArrayList<>();
        for (int rowIndex = 1; rowIndex <= count; rowIndex++) {
            String[] row = new String[fields.size()];
            for (int col = 0; col < fields.size(); col++) {
                row[col] = generateValue(fields.get(col), rowIndex);
            }
            rows.add(row);
        }
        return rows;
    }

    private String generateValue(FieldDefinition field, int rowIndex) {
        switch (field.getBaseType()) {
            case "text":
            case "textarea":
            case "phone":
            case "url":
                return randomText(field);
            case "email":
                return rowIndex + "@" + emailDomain;
            case "date":
                return randomDate().toString();
            case "datetime":
                return randomDateTime().toString();
            case "number":
            case "currency":
            case "percent":
                return randomNumber(field);
            case "checkbox":
            case "boolean":
                return String.valueOf(random.nextBoolean());
            case "picklist":
            case "multipicklist":
                return field.getSampleValue();
            case "lookup":
            case "id":
            case "reference":
                return randomSalesforceId();
            default:
                return field.getSampleValue() == null ? "" : field.getSampleValue();
        }
    }

    private String randomText(FieldDefinition field) {
        Integer maxLength = field.getMaxLength();
        int length = maxLength == null ? DEFAULT_TEXT_LENGTH : Math.min(maxLength, DEFAULT_TEXT_LENGTH);
        length = Math.max(length, 1);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUM.charAt(random.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }

    private LocalDate randomDate() {
        long startEpoch = dateRangeStart.toEpochDay();
        long endEpoch = dateRangeEnd.toEpochDay();
        long randomEpoch = startEpoch + (long) (random.nextDouble() * (endEpoch - startEpoch + 1));
        return LocalDate.ofEpochDay(randomEpoch);
    }

    private LocalDateTime randomDateTime() {
        return randomDate().atTime(random.nextInt(24), random.nextInt(60), random.nextInt(60));
    }

    private String randomNumber(FieldDefinition field) {
        Integer decimals = field.getDecimalPlaces();
        int digitsAfterPoint = decimals == null ? 0 : decimals;
        double value = numberRangeMin + random.nextDouble() * (numberRangeMax - numberRangeMin);
        return String.format("%." + digitsAfterPoint + "f", value);
    }

    private String randomSalesforceId() {
        StringBuilder sb = new StringBuilder(18);
        for (int i = 0; i < 18; i++) {
            sb.append(ALPHANUM.charAt(random.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }
}
